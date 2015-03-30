# Quick Start #

This page assumes that you have followed the instructions on the [home page](http://code.google.com/p/structured-cascades/) to download and install the software and datasets.

To run a tri-gram model on the HW-Small task, invoke the training program using the provided configuration file:
```
java -cp structured-cascades.jar cascades.programs.TrainTagger 3gram.fig ocr_fold0_sm
```
This will run the full training procedure on the first fold of the dataset The program will create an `cascade-outputs/3gram-ocr_fold0_sm` directory that contains subdirectories for each of the 4 models in the cascade: `l0` (multi-class), `l1` (bigram), `l2`, (trigram, used for final classifier). A summary of the test statistics for each level is in the `full-test.txt` file in each directory.

## Interpreting the Command Line Output ##

By default, the program outputs status at the end of each epoch of training and at the end of training each partition of the dataset for each level of the cascade. Note that because training is **jacknifed**, training involves repeatedly training _n_+1 filters for each level of the cascade, where _n_ is the number of jacknife partitions. The final partition trains on the entire training set, but the jacknifing ensures that the each example in the full training set contains output from the previous level of the cascade that was trained _without_ that example. This dramatically reduces overfitting to the previous level of the cascade.

At the end of training each model on the full partition in the cascade, the program will output some test statistics as follows:
```
** DEVEL SET Performance: (best epoch = 1)
Trade-off Optimization:
	Alpha: 0.4600 (max 1.0000), Err: 0.9942% [+0.8938%], ZErr: 0.4800% [+0.4990%] (cap 1.0000%), Eff: 22.8061% (32.3271% zero [-52.6410%])
Best Epoch Peformance:
	Alpha Used: 0.5000, Prune error: 1.2447% [+0.8938%] / 0.6075% [+0.4990%] zero, Eff. Loss: 20.4740%, ZEff: 29.9649%, 
	Error: 37.8196%, Sequence Error: 89.7893%
	Avg States Per Position: 13.6716, Avg Edges Per State: 12.0332
** TEST Peformance: **
	Alpha Used: 0.4600, Prune error: 1.4231% [+0.4090%] / 0.7541% [+0.2282%] zero, Eff. Loss: 22.5903%, ZEff: 32.5286%, 
	Error: 38.0020%, Sequence Error: 90.7670%
	Avg States Per Position: 13.7300, Avg Edges Per State: 12.1534
```
There are three sets of performance metrics in the above output. The first is the trade-off optimization performed on the development set; the second is the performance of the best epoch chosen by cross-validation on the development set; third third is the performance of the weights and filtering threshold chosen by cross-validation on the test set.

The error metrics are defined as follows:
  * In all metrics the error in brackets `[+ ...]` indicates the baseline value of that error metric due to previous levels of the cascade. The value outside of the brackets is therefore the **additional** error incurred by that level of the cascade.
  * _Trade-off performance:_ `Alpha` is the filtering threshold parameter described in Weiss & Taskar (2010) chosen to **maximize** filtering efficiency while keeping the filtering error rate under a **fixed error cap**. `Err` refers to the filtering error defined on the marginals of the current model, while `ZErr` refers to filtering error defined on the original tags of the model. `Eff` refers to marginal filtering efficiency while `ZEff` refers to filtering of the original labels (lower is better).
  * _Best Epoch Performance:_ Here, `Prune error` refers to the filtering error while `Error` and `Sequence Error` refer to classification error at the character and word level, respectively. Note that since the above errors correspond to a learned filter, the classification error is high while the filtering error is very low. The `alpha` value shown is the one that was specified during the corresponding training epoch. Finally, `Avg States Per Position` and `Avg. Edges Per State` show the sparsity of the lattice at the current level of the cascade (since the above is a bigram model, this means that there were ~13 states and ~12 bigrams per state when the model was evaluated.)
  * _Test Performance:_ The metrics are the same as for the "best epoch," but the `Alpha Used` is now the value chosen by the trade-off optimization procedure.

# Changing the Options #

All of the interesting configuration of the cascade (how many levels there are, how they are defined, etc.) takes place in the configuration file `ocr_example.fig`. See UsingConfigurationFiles for a detailed explanation of this file (note: the file itself is heavily commented.)

For more detailed information about how to invoke  the program, see AdvancedUsage.