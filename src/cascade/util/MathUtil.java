package cascade.util;

public class MathUtil {

    public static double logsumexp(double a, double b){

        if (a < b)                                                                                                                     
                return a + Math.log(1+Math.exp(b-a));                                                                                  
        else                                                                                                                           
                return b + Math.log(1+Math.exp(a-b));                                                                                  
        
    }                                                                                                                                      

}
