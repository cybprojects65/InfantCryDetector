package it.cnr.infant.asr;

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;

public class ASRClient {

    public static String sendWaveFile(File wavFile, String serviceUrl) throws Exception {

        if (!wavFile.exists()) {
            throw new IllegalArgumentException("File does not exist: " + wavFile);
        }

        if (!wavFile.getName().toLowerCase().endsWith(".wav")) {
            throw new IllegalArgumentException("File must be a WAV file: " + wavFile);
        }

        HttpClient client = HttpClient.newHttpClient();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(serviceUrl))
                .header("X-Filename", wavFile.getName())
                .header("Content-Type", "audio/wav")
                .POST(HttpRequest.BodyPublishers.ofByteArray(
                        Files.readAllBytes(wavFile.toPath())
                ))
                .build();

        HttpResponse<String> response = client.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );

        if (response.statusCode() != 200) {
            throw new RuntimeException(
                    "ASR server returned HTTP " + response.statusCode()
                            + "\n" + response.body()
            );
        }

        return response.body();
    }

    public static void main(String[] args) throws Exception {

        File wavFile = new File("./testFiles/PS14Audio_1_vocals_large_16khz.wav");

        String result = sendWaveFile(
                wavFile,
                "http://asrncss.ddns.net"
        );

        System.out.println(result);
    }
}