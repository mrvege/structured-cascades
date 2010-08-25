package cascade.learn;

import cascade.io.SimpleLogger;

/**
 * A data-storage class that holds the filter vs. efficiency trade-off for many different values of alpha.
 * Since computing is slightly different than computing regular just "generalization" statistics, and it
 * requires its own bookkeeping, this is a different class than GeneralizationStatistics. 
 * 
 * @author djweiss
 *
 */
public class FilterTradeoffStatistics {

	public int numSequences; 

	// / specific to using error cap to choose
	public double bestZeroErrUnderCap;
	public double bestEffUnderCap;
	public double bestZeroEffUnderCap;
	public double bestErrUnderCap;	
	public double bestAlpha;
	public double alphaCap;
	
	public double cap = -1;

	// Efficiency - error trade off curve
	public double alphas[], effs[], errs[], zeroErrs[], zeroEffs[];

	public double baselineErr, zeroBaselineErr, zeroBaselineEff;
	
	public boolean useZeroError = false;
	
	public static final String [] writeFields = {"bestZeroErrUnderCap", "bestErrUnderCap", "bestZeroEffUnderCap", "bestEffUnderCap", 
		"bestAlpha", "alphaCap"};
	
	
	public FilterTradeoffStatistics() {
	
		alphas = new double[201];
		for (int i = 0; i < alphas.length; i++)
			alphas[i] = (double) i * (1.0 / (alphas.length - 1));
		
		effs = new double[alphas.length];
		errs = new double[alphas.length];
		zeroErrs = new double[alphas.length];
		zeroEffs = new double[alphas.length];
		

	}
	
	// Finds the best Alpha
	public void findBestAlpha(double maxerr, double maxalpha) {

		cap = maxerr;
		alphaCap = maxalpha;
		
//		System.out.printf("looking for min with err cap: %g, maxalpha %g\n", maxerr, maxalpha);

		double [] errs;
		if (useZeroError)
			errs = this.zeroErrs;
		else
			errs = this.errs;
		
		double min = Double.POSITIVE_INFINITY;
		int argmin = 0;
		for (int i = 0; i < effs.length; i++) {

			//System.out.printf("%5f %5f\n", effs[i], errs[i]);
			if (effs[i] < min && errs[i] <= maxerr && alphas[i] <= maxalpha) {
				min = effs[i];
				argmin = i;
				//System.out.printf("%5f %5f***\n", effs[i], errs[i]);
			}
		}

		bestAlpha = alphas[argmin];
		bestEffUnderCap = effs[argmin];
		bestErrUnderCap = this.errs[argmin];
		bestZeroErrUnderCap = this.zeroErrs[argmin];
		bestZeroEffUnderCap = this.zeroEffs[argmin];
	}
	
	public void saveTradeoffCurves(String fileName) {

		SimpleLogger out = new SimpleLogger(fileName);
		
		out.println("alpha, effiencyLoss, filterLoss, zeroFilterLoss, zeroEffLoss");
		for (int i = 0; i < alphas.length; i++) {
			out.println(String.format("%.8g, %.8g, %.8g, %.8g, %.8g", alphas[i], effs[i]*100, errs[i]*100, zeroErrs[i]*100, zeroEffs[i]*100));
		}
		
		
	}
	
	public String summarize() {
		return String.format(
						"\n\tAlpha: %.4f (max %.4f), Err: %.4f%% [+%.4f%%], ZErr: %.4f%% [+%.4f%%] (cap %.4f%%), Eff: %.4f%% (%.4f%% zero [-%.4f%%])",
						bestAlpha, alphaCap, bestErrUnderCap , baselineErr, bestZeroErrUnderCap, 
						zeroBaselineErr, cap, bestEffUnderCap , bestZeroEffUnderCap, zeroBaselineEff);
	}
	
	public void average() {
		
		baselineErr = 100*(baselineErr/numSequences);
		zeroBaselineErr = 100*(zeroBaselineErr/numSequences);
		zeroBaselineEff = 100*(zeroBaselineEff/numSequences);
		
		for (int i = 0; i < alphas.length; i++) {
			errs[i] = 100*(errs[i] / numSequences) - baselineErr;
			effs[i] = 100*effs[i]/numSequences;
			zeroErrs[i] = 100*(zeroErrs[i] / numSequences) - zeroBaselineErr;
			zeroEffs[i] = 100*(zeroEffs[i] / numSequences); // - zeroBaselineEff;
		}
		
		
	}
	
	
}
