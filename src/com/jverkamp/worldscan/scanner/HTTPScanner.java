package com.jverkamp.worldscan.scanner;

import java.net.InetAddress;
import java.util.Iterator;

/**
 * Scan for HTTP based censorship.
 */
public class HTTPScanner implements Scanner {

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public Iterator<ScanResult> iterator() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    
}
