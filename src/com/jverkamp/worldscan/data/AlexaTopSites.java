
package com.jverkamp.worldscan.data;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.apache.commons.io.FileUtils;

/**
 * Accessors for the Alexa Top Million domains.
 * http://www.alexa.com/
 */
public class AlexaTopSites implements Iterable<String>{
    final static String TOP_MILLION_REMOTE = "http://s3.amazonaws.com/alexa-static/top-1m.csv.zip";
    final static String TOP_MILLION_LOCAL = "top-1m.csv.zip";
    final static String TOP_MILLION_NAME = "top-1m.csv";
    
    int currentIndex = 0;
    List<String> sites;
    
    /**
     * Iterate over the Alexa top n sites.
     * 
     * @param topN The number of sites to scan.
     */
    public AlexaTopSites(int topN) {
        this(topN, false);
    }
    
    /**
     * Iterate over the Alexa top n sites.
     * 
     * @param topN The number of sites to scan.
     * @param shuffle If the sites should be returned in a random order.
     */
    public AlexaTopSites(int topN, boolean shuffle) {
        try {
            if (topN < 1) throw new IllegalArgumentException("N must be at least 1");
            if (topN > 1000000) throw new IllegalArgumentException("N must be less than 1 million");
            
            sites = new ArrayList<>();
            
            // Check for a cached local file, otherwise download a new version
            File localFile = new File(TOP_MILLION_LOCAL);
            if (!localFile.exists()) {
                FileUtils.copyURLToFile(new URL(TOP_MILLION_REMOTE), localFile);
            }
            
            // Read the csv directly from the zip, stop once we have enough
            // File is CSV with fields: rank,domain
            ZipFile zip = new ZipFile(localFile);
            Scanner alexaReader = new Scanner(zip.getInputStream(zip.getEntry(TOP_MILLION_NAME)));
            for (int i = 0; i < topN && alexaReader.hasNextLine(); i++) {
                String[] line = alexaReader.nextLine().split(",");
                sites.add(line[1]);
            }
            alexaReader.close();
            zip.close();
            
            if (shuffle) {
                Collections.shuffle(sites);
            }
        } catch (IOException ex) {
            Logger.getLogger(AlexaTopSites.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    /**
     * Iterate over the domains.
     * @return Domain iterator.
     */
    @Override
    public Iterator<String> iterator() {
        return new Iterator<String>() {
            /**
             * @return Are we done yet?
             */
            @Override
            public boolean hasNext() {
                return currentIndex != sites.size();
            }

            /**
             * @return Next Alexa domain.
             */
            @Override
            public String next() {
                return sites.get(currentIndex++);
            }
        };
    }
}
