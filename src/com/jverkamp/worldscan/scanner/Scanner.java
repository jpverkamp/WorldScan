package com.jverkamp.worldscan.scanner;

/**
 * 
 */
public interface Scanner extends Iterable<ScanResult> {
    
    /**
     * Check if a given scanner is valid.
     * 
     * Examples would be DNS based scans may require an open DNS server on the 
     * remote machine, which can be checked before running the full scan.
     * 
     * @return If the scan should proceed.
     */
    public boolean isValid();
}
