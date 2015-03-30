# Training a Cascade #

Training is performed with the `cascade.programs.TrainTagger` program. The command line arguments to this program are as follows:
```
java cascade.programs.TrainTagger <fig_file> [suffix] [train_levels]
```
The first argument `fig_file` is required: this specifies the configuration file that determines the structure and parameters of the cascade. (See UsingConfigurationFiles).

The first _optional_ argument is `suffix`, which is a string appended to the `Corpus.prefix` (name of the output directory) and `Corpus.src` (name of the dataset files) String parameters. This is so that a single configuration file can be used with multiple datasets; for example, the single `ocr_example.fig` can be used with the suffix `fold0`, `fold1`, etc., to be used for each fold of the dataset.

The second _optional_ argument is `train_levels`, which is a series of integers specifying which levels of the cascade should be trained (the remainder are loaded from the output directory). Thus, in the OCR example, if we wanted to tweak the trigram model parameters but leave the rest intact, we only need to train the trigram and the quadgram models, not the entire cascade all over again.

Summing up, an example where we train on the third fold, assuming the uni-gram and bi-gram models are already trained, is as follows:

```
java cascade.programs.TrainTagger ocr_example.fig fold3 2 3
```

# Evaluating a Cascade #

Given a trained cascade we can evaluate it on a different dataset. The `UseTrainedTagger` program is used for this purpose.  All arguments are required:
```
java cascade.programs.UseTrainedTagger <fig_file> <suffix> <test_filename> <out_filename>
```
`fig_file` and `suffix` are the same as for `TrainTagger`, but note that both are required. Ideally this functionality will change in future revisions. `test_filename` is the filename (including extension) that you wish to test the cascade on, and `out_filename` is the filename (including extension) that will be generated containing the predictions of the cascade.

Note that `UseTrainedTagger` will compute error statistics for each level of the cascade.

# Inspecting Cascade Output #

Under construction. See `Inspector.java` in the source code for more information.