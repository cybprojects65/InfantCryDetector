package it.cnr.infant.features;

import java.util.Arrays;

public class MFCCExtractor {

    public static double[][] extractMFCCWithDeltas(
            short[] signal,
            double sampleRate,
            int numMfcc,
            int numMelFilters,
            double windowMs,
            double hopMs
    ) {
        double[] x = normalize(signal);
        preEmphasis(x, 0.97);

        int frameSize = (int) Math.round(sampleRate * windowMs / 1000.0);
        int hopSize = (int) Math.round(sampleRate * hopMs / 1000.0);
        int fftSize = nextPowerOfTwo(frameSize);

        double[][] frames = frameSignal(x, frameSize, hopSize);
        double[] hamming = hamming(frameSize);

        double[][] melFilters = melFilterBank(
                numMelFilters,
                fftSize,
                sampleRate,
                20,
                sampleRate / 2.0
        );

        double[][] mfcc = new double[frames.length][numMfcc];

        for (int i = 0; i < frames.length; i++) {
            double[] frame = frames[i];

            for (int j = 0; j < frameSize; j++) {
                frame[j] *= hamming[j];
            }

            double[] powerSpectrum = powerSpectrum(frame, fftSize);
            double[] melEnergies = applyMelFilters(powerSpectrum, melFilters);

            for (int j = 0; j < melEnergies.length; j++) {
                melEnergies[j] = Math.log(Math.max(melEnergies[j], 1e-12));
            }

            mfcc[i] = dct(melEnergies, numMfcc);
        }

        double[][] delta = computeDeltas(mfcc, 2);
        double[][] doubleDelta = computeDeltas(delta, 2);

        return concatenate(mfcc, delta, doubleDelta);
    }

    private static double[] normalize(short[] signal) {
        double[] out = new double[signal.length];

        for (int i = 0; i < signal.length; i++) {
            out[i] = signal[i] / 32768.0;
        }

        return out;
    }

    private static void preEmphasis(double[] x, double coeff) {
        for (int i = x.length - 1; i >= 1; i--) {
            x[i] = x[i] - coeff * x[i - 1];
        }
    }

    private static double[][] frameSignal(double[] signal, int frameSize, int hopSize) {
        if (signal.length < frameSize) {
            double[][] frames = new double[1][frameSize];
            System.arraycopy(signal, 0, frames[0], 0, signal.length);
            return frames;
        }

        int numFrames = 1 + (signal.length - frameSize) / hopSize;
        double[][] frames = new double[numFrames][frameSize];

        for (int i = 0; i < numFrames; i++) {
            int start = i * hopSize;
            System.arraycopy(signal, start, frames[i], 0, frameSize);
        }

        return frames;
    }

    private static double[] hamming(int size) {
        double[] w = new double[size];

        for (int i = 0; i < size; i++) {
            w[i] = 0.54 - 0.46 * Math.cos(
                    2.0 * Math.PI * i / (size - 1)
            );
        }

        return w;
    }

    private static double[] powerSpectrum(double[] frame, int fftSize) {
        double[] real = new double[fftSize];
        double[] imag = new double[fftSize];

        System.arraycopy(frame, 0, real, 0, frame.length);

        fft(real, imag);

        int bins = fftSize / 2 + 1;
        double[] power = new double[bins];

        for (int i = 0; i < bins; i++) {
            power[i] = (real[i] * real[i] + imag[i] * imag[i]) / fftSize;
        }

        return power;
    }

    private static double[][] melFilterBank(
            int numFilters,
            int fftSize,
            double sampleRate,
            double lowFreq,
            double highFreq
    ) {
        int bins = fftSize / 2 + 1;
        double[][] filters = new double[numFilters][bins];

        double lowMel = hzToMel(lowFreq);
        double highMel = hzToMel(highFreq);

        double[] melPoints = new double[numFilters + 2];

        for (int i = 0; i < melPoints.length; i++) {
            melPoints[i] = lowMel +
                    i * (highMel - lowMel) / (numFilters + 1);
        }

        int[] bin = new int[numFilters + 2];

        for (int i = 0; i < bin.length; i++) {
            double freq = melToHz(melPoints[i]);
            bin[i] = (int) Math.floor((fftSize + 1) * freq / sampleRate);
            bin[i] = Math.min(bin[i], bins - 1);
        }

        for (int m = 1; m <= numFilters; m++) {
            int left = bin[m - 1];
            int center = bin[m];
            int right = bin[m + 1];

            for (int k = left; k < center; k++) {
                filters[m - 1][k] =
                        (double) (k - left) / Math.max(center - left, 1);
            }

            for (int k = center; k < right; k++) {
                filters[m - 1][k] =
                        (double) (right - k) / Math.max(right - center, 1);
            }
        }

        return filters;
    }

    private static double[] applyMelFilters(
            double[] powerSpectrum,
            double[][] filters
    ) {
        double[] energies = new double[filters.length];

        for (int m = 0; m < filters.length; m++) {
            double sum = 0.0;

            for (int k = 0; k < powerSpectrum.length; k++) {
                sum += powerSpectrum[k] * filters[m][k];
            }

            energies[m] = sum;
        }

        return energies;
    }

    private static double[] dct(double[] input, int numCoefficients) {
        double[] result = new double[numCoefficients];
        int n = input.length;

        for (int k = 0; k < numCoefficients; k++) {
            double sum = 0.0;

            for (int i = 0; i < n; i++) {
                sum += input[i] * Math.cos(
                        Math.PI * k * (i + 0.5) / n
                );
            }

            result[k] = sum;
        }

        return result;
    }

    private static double[][] computeDeltas(double[][] features, int n) {
        int rows = features.length;
        int cols = features[0].length;

        double[][] deltas = new double[rows][cols];

        double denominator = 0.0;

        for (int i = 1; i <= n; i++) {
            denominator += i * i;
        }

        denominator *= 2.0;

        for (int t = 0; t < rows; t++) {
            for (int c = 0; c < cols; c++) {

                double numerator = 0.0;

                for (int i = 1; i <= n; i++) {
                    int prev = Math.max(0, t - i);
                    int next = Math.min(rows - 1, t + i);

                    numerator += i * (
                            features[next][c] - features[prev][c]
                    );
                }

                deltas[t][c] = numerator / denominator;
            }
        }

        return deltas;
    }

    private static double[][] concatenate(
            double[][] mfcc,
            double[][] delta,
            double[][] doubleDelta
    ) {
        int rows = mfcc.length;
        int cols = mfcc[0].length;

        double[][] out = new double[rows][cols * 3];

        for (int i = 0; i < rows; i++) {
            System.arraycopy(mfcc[i], 0, out[i], 0, cols);
            System.arraycopy(delta[i], 0, out[i], cols, cols);
            System.arraycopy(doubleDelta[i], 0, out[i], cols * 2, cols);
        }

        return out;
    }

    private static double hzToMel(double hz) {
        return 2595.0 * Math.log10(1.0 + hz / 700.0);
    }

    private static double melToHz(double mel) {
        return 700.0 * (Math.pow(10.0, mel / 2595.0) - 1.0);
    }

    private static int nextPowerOfTwo(int n) {
        int power = 1;

        while (power < n) {
            power <<= 1;
        }

        return power;
    }

    private static void fft(double[] real, double[] imag) {
        int n = real.length;

        for (int i = 1, j = 0; i < n; i++) {
            int bit = n >> 1;

            for (; j >= bit; bit >>= 1) {
                j -= bit;
            }

            j += bit;

            if (i < j) {
                double tempReal = real[i];
                real[i] = real[j];
                real[j] = tempReal;

                double tempImag = imag[i];
                imag[i] = imag[j];
                imag[j] = tempImag;
            }
        }

        for (int len = 2; len <= n; len <<= 1) {
            double angle = -2.0 * Math.PI / len;
            double wLenReal = Math.cos(angle);
            double wLenImag = Math.sin(angle);

            for (int i = 0; i < n; i += len) {
                double wReal = 1.0;
                double wImag = 0.0;

                for (int j = 0; j < len / 2; j++) {
                    int u = i + j;
                    int v = i + j + len / 2;

                    double realV = real[v] * wReal - imag[v] * wImag;
                    double imagV = real[v] * wImag + imag[v] * wReal;

                    real[v] = real[u] - realV;
                    imag[v] = imag[u] - imagV;

                    real[u] += realV;
                    imag[u] += imagV;

                    double nextWReal =
                            wReal * wLenReal - wImag * wLenImag;

                    double nextWImag =
                            wReal * wLenImag + wImag * wLenReal;

                    wReal = nextWReal;
                    wImag = nextWImag;
                }
            }
        }
    }

    public static void main(String[] args) {
        double samplingFrequency = 16000.0;

        short[] signal = new short[16000];

        for (int i = 0; i < signal.length; i++) {
            signal[i] = (short) (
                    10000 * Math.sin(2.0 * Math.PI * 440.0 * i / samplingFrequency)
            );
        }

        double[][] features = MFCCExtractor.extractMFCCWithDeltas(
                signal,
                samplingFrequency,
                13,
                26,
                25.0,
                10.0
        );

        System.out.println("Rows: " + features.length);
        System.out.println("Columns: " + features[0].length);

        System.out.println(Arrays.toString(features[0]));
    }
}
