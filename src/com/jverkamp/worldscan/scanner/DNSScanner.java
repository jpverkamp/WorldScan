package com.jverkamp.worldscan.scanner;

import com.jverkamp.worldscan.data.AlexaTopSites;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.xbill.DNS.*;

/**
 * Scan for DNS based censorship.
 */
public class DNSScanner implements Scanner {
    InetAddress resolverIP;
    Resolver resolver;
    Cache cache;
    AlexaTopSites domains;
    
    /**
     * Create a DNS scanner.
     * @param addr The IP address of the resolver.
     * @param domainsToScan How many domains to scan.
     */
    public DNSScanner(InetAddress addr, int domainsToScan) {
        try {
            
            resolverIP = addr;
            
            resolver = new SimpleResolver(addr.getHostAddress());
            resolver.setTimeout(5);
            
            cache = new Cache();
            cache.clearCache();
            
            domains = new AlexaTopSites(domainsToScan, true);
            
        } catch(UnknownHostException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * DNS scans must point to an open resolver.
     * 
     * @return If the Address given when creating the scan is an open resolver.
     */
    @Override
    public boolean isValid() {
        try {
            cache.clearCache();

            Lookup l = new Lookup("www.example.com");
            l.setResolver(resolver);
            l.setCache(cache);
            l.run();
            
            return (l.getResult() == Lookup.SUCCESSFUL);
        } catch(TextParseException ex) { 
            return false;
        }
    }

    /**
     * Provide the scan iterator.
     * 
     * @return An iterator.
     */
    @Override
    public Iterator<ScanResult> iterator() {
        Iterator<String> queue = domains.iterator();
        
        return new Iterator<ScanResult>() {

            /**
             * Are there any more domains to scan on this resolver?
             * @return 
             */
            @Override
            public boolean hasNext() {
                return queue.hasNext();
            }

            /**
             * The scan result for the next domain on the list.
             * @return 
             */
            @Override
            public ScanResult next() {
                String domain = queue.next();
                
                ScanResult result = new ScanResult();
                result.scanType = "DNS";
                result.scanTarget = resolverIP.getHostAddress();
                result.scanCriteria = domain;
                
                try {
                    cache.clearCache();
                    
                    Lookup l = new Lookup(domain);
                    l.setResolver(resolver);
                    l.setCache(cache);
                    Record[] records = l.run();
                    
                    
                    result.censored = (
                            l.getResult() != Lookup.SUCCESSFUL
                            || records.length == 0
                            );
                    
                    
                } catch (TextParseException ex) {
                    result.exception = ex;
                }
                
                return result;
            }
        };
    }
}
