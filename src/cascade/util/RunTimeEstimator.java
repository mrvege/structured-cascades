package cascade.util;

import java.util.ArrayList;
import java.lang.reflect.Method;

public class RunTimeEstimator {

	private static final String defaultFormat = "h:mm:ss a, MM/dd/2009";  
	
	private String metricString;
	private int totalIterations;
	private long startTime;	
	private long stopTime;
	
	private ArrayList<Method> reportStatMethods;
	private ArrayList<Object> reportStatObjects;
	
	private int updateFrequency;	
	private int currentIteration;
			
	private boolean announced = false;
	
	public RunTimeEstimator(int T, double updatePercentage) {
		
		totalIterations = T;
		currentIteration = 0;
		
		updateFrequency = (int) Math.ceil(T * updatePercentage);
		if (updateFrequency == 0)
			updateFrequency = 1;
		
		startTime = System.currentTimeMillis();
		stopTime = -1;
		
		reportStatMethods = null;
		reportStatObjects = null;
		
		
		
//				DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.LONG).format(startTime) + 
//				" [updates every " + updateFrequency + " iter]");		
	}
	
	public void registerStatMethod(Method m, Object o) {

		if (reportStatMethods == null)
			reportStatMethods = new ArrayList<Method>();
		if (reportStatObjects == null)
			reportStatObjects = new ArrayList<Object>();
		
		//if (m.getReturnType().equals(Type.STRING)) {
//			System.out.println("successfully registered " + m.getName());
			//reportStatMethods.add(m);
			//reportStatObjects.add(o);
//		}
	}
	
	public String getExtraInfo() { 
		 String report = "";
			if (reportStatMethods != null) {
				
				for (int i = 0; i < reportStatMethods.size(); i++) {
					Method m = reportStatMethods.get(i);
					Object o = reportStatObjects.get(i);
					
					try {
						report += "[" + (String) m.invoke(o, (Object [])null) + "] ";
					} catch (Exception e) {
						e.printStackTrace();
						System.exit(1);
					}
				}
			}
			
		return report; 
	};
	
	public boolean report() {
		return this.report(++currentIteration);
	}
		
	public void setMetricString(String s) {
		metricString = s;
	}
	public boolean report(int t) {		
		if (!announced) {
			System.out.println("Computation started: updates every " + updateFrequency + " iter");
			announced = true;
		}
		if (t % updateFrequency == 0) {
			
			long elapsed = System.currentTimeMillis() - startTime;
			
			double rate = (double)t / (double)elapsed;			
			double remaining = (totalIterations - t) / rate;
			
			//long estCompletion = elapsed + (long)remaining + startTime;
			
			rate *= 1000; // convert to s
			String rateString = "iter/s";
			if (rate < 0) {
				rate = 1.0/rate;
				rateString = "s/iter";
			}			
			double percentageComplete = (double)t / (double)totalIterations;			
			
			String remainingStr = timeFormat(remaining);
			String elapsedStr = timeFormat(elapsed);
			
			if (metricString != null)
				rateString += ", " + metricString;
			
			String report = getExtraInfo();
			
			System.out.printf("\t%.1f%% - %s elapsed / %s remaining [%.2f %s] %s\n", percentageComplete*100,
					elapsedStr, remainingStr, rate, rateString, report);
			
			return true;
		}		
		return false;
	}
	
	public static String timeFormat(double t) {
		
		int ts = (int) (t / 1000);
		
		int hours = ts / 3600;
		ts -= hours*3600; // subtract hours from seconds
				
		int minutes = ts / 60; 		
		ts -= minutes*60; // subtract minutes: seconds should be left
				
		return String.format("%02d:%02d:%02d", hours, minutes, ts);
	}

	public RunTimeEstimator stop() {
		stopTime = System.currentTimeMillis();
		return this;		
	}

	public long getElapsed() {
		return System.currentTimeMillis() - startTime;
	}
	
	public long getRunTimeMillis() {
		if (stopTime > 0)
			return stopTime - startTime;
		else 
			return -1;
	}

	public int getCurrentIteration() {
		return currentIteration;
	}
	
	
}
