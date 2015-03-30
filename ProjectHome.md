# Overview #

### NOTE: NEWLY UPDATED AS OF 8/15/2013 ###

A Java implementation of the Structured Prediction Cascades framework as proposed by Weiss & Taskar (2010). This project contains an updated version of the code used to generate the handwriting recognition/OCR results in the paper, as well as a properly formatted version of the [OCR dataset](http://www.seas.upenn.edu/~taskar/ocr/). This project is published under the MIT license, which allows free usage and redistribution of the code for any purpose so long as the license and copyright statement remains in the code.

An early version of this code was used to generate the OCR results in the original paper. The results have improved further quite dramatically. We have also expanded the benchmarks to include more variants.

| **Method** | **OCR-Large** | **OCR-Small** |
|:-----------|:--------------|:--------------|
| Cascades (3-gram) | 3.22% | 13.02%  |
| Cascades (4-gram) | 2.16% | 10.82%  |
| HC-Search (3-gram) | 3.78% | 13.35% |
| HC-Search (4-gram) | 3.03%| 11.24% |

The OCR dataset (both small and large versions) as well as the Nettalk benchmark dataset preformatted for use with this code is available for download from this website.

References:

  * David Weiss & Ben Taskar. **Structured Prediction Cascades.** AISTATS 2010 `[`[link](http://www.cis.upenn.edu/~dwe/pub.html)`]`
  * Janardhan Rao Doppa, Alan Fern, Prasad Tadepalli. **HC-Search: Learning Heuristics and Cost Functions for Structured Prediction.** AAAI 2013 [link](http://web.engr.oregonstate.edu/~doppa/pubs/AAAI2013_HC-Search.pdf)

### Contributors ###

The authors of this package are [David Weiss](http://www.cis.upenn.edu/~dwe) and [Kuzman Ganchev](http://www.seas.upenn.edu/~kuzman/), with additional help from
[Joao Graca](http://www.cis.upenn.edu/~graca/About_Me.html).

# Installation (binary) #

  1. Download the [structured-cascades.tar.gz](http://code.google.com/p/structured-cascades/downloads/detail?name=structured-cascades.tar.gz&can=2) file and extract the contents.
  1. Download the [datasets](http://code.google.com/p/structured-cascades/downloads/detail?name=benchmark_data.zip) and extract them into a `data` subfolder.
  1. You can now invoke the program by adding the jar your Java class path, or by invoking java as follows:
```
java -cp structured-cascades.jar cascades.programs.TrainTagger <args>
```
  1. See the QuickStart guide to run the demo on the OCR dataset.

To install from source code, check out the code from the repository and follow the README instructions. The repository can be checked out directly from Eclipse if you install the Mercurial Eclipse plugin from the Eclipse Marketplace.

# Usage #

See the wiki documentation for usage instructions:

  * QuickStart - A quick demo to run the OCR dataset showcasing the state-of-the-art accuracy of the SC
  * UsingConfigurationFiles - How to set up `.fig` files that the program uses to configure the cascade
  * AdvancedUsage - Testing on alternative datasets, inspecting the cascade output, etc.