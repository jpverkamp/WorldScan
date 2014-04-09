
package com.jverkamp.worldscan.scanner;

import com.google.gson.Gson;

/**
 * 
 */
public class ScanResult {
    String scanType = null;
    String scanTarget = null;
    String scanCriteria = null;
    
    boolean censored = false;
    
    Exception exception = null;
    
    @Override
    public String toString() {
        return new Gson().toJson(this);
    }
}
