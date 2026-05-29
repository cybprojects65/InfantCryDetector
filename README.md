
# InfantCryDetector

  

InfantCryDetector is a Java-based framework for automatic infant cry detection and anomaly identification from audio recordings. The system combines acoustic feature extraction, machine learning, and self-training strategies to identify infant cry events in real-world recordings and support neonatal monitoring applications.

  

The project has been developed for research on neonatal cry analysis and automatic detection of anomalous crying patterns associated with clinical and behavioral conditions.

  

How to cite:

  

> Coro, G., Bardelli, S., Cuttano, A., Scaramuzzo, R. T., & Ciantelli,
> M. (2023). A self-training automatic infant-cry detector. Neural
> Computing and Applications, 35(11), 8543-8559.

  

---

  

## Overview

  

Infant cry analysis is an important area of research in neonatal care, pediatric monitoring, and early diagnosis support systems. Manual analysis of long audio recordings is time-consuming and often impractical in clinical environments.

  

InfantCryDetector provides:

  

* Automatic infant cry detection from audio recordings

* Self-training classification workflows

* Acoustic feature extraction and analysis

* Batch processing of WAV audio files

* Performance evaluation utilities

* Research-oriented experimentation environment

  

The framework is designed for researchers, clinicians, and developers working on:

  

* Neonatal monitoring

* Biomedical signal processing

* Audio event detection

* Machine learning for healthcare

* Cry acoustics analysis

  

---

  

## Scientific Background

  

The methodology implemented in this repository is based on self-training machine learning approaches for infant cry detection and classification.

  

The framework has been described in:

  

**Coro, G., Bardelli, S., Cuttano, A., Scaramuzzo, R. T., & Ciantelli, M. (2023).**

  

*A self-training automatic infant-cry detector.*

  

Neural Computing and Applications, 35(11), 8543–8559.

  


## Main Classes

  

### Production Workflow

  

```java

it.cnr.infant.main

```

  
This class represents the primary execution entry point for cry detection workflows.

  

### Testing Workflow

  From the "standalone" folder, execute:

```java

java -jar infantCryDetector.jar "./testfile_PCM_signed_16bit_16kHz_mono.wav"

```

  
  
## Requirements

  


* Java 8 or newer

* Eclipse IDE (recommended)

* Whisper ASR service running: 
https://hub.docker.com/repository/docker/gianpaolocoro/asr-whisper-ncss-server/general

* As default, the system connects to a running instance residing at asrncss.ddns.net changable from a configuration file
  

### Operating Systems

  

* Windows

* Linux

* macOS

  

### Dependencies


The project depends on additional Java libraries located in the `lib` directory and on auxiliary machine learning projects referenced by the repository.

Additionally, it requires the following project as a dependency:
https://github.com/cybprojects65/ToneUnitMarker

Before compilation, ensure that all external dependencies are correctly imported into the Java build path.

  