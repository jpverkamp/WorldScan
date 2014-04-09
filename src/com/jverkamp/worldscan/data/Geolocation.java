package com.jverkamp.worldscan.data;

import java.io.File;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.charset.MalformedInputException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.apache.commons.io.FileUtils;

/**
 * Methods for determining the country for an IP address or IP addresses for a
 * country.
 *
 * This product includes GeoLite2 data created by MaxMind, available from
 * <a href="http://www.maxmind.com">http://www.maxmind.com</a>.
 */
public class Geolocation {

    final static String GEO_IP_REMOTE = "http://geolite.maxmind.com/download/geoip/database/GeoLite2-Country-CSV.zip";
    final static String GEO_IP_LOCAL = "GeoLite2-Country-CSV.zip";

    final static String GEO_IP_BLOCKS_NAME = "GeoLite2-Country-Blocks.csv";
    final static String GEO_IP_LOCATIONS_NAME = "GeoLite2-Country-Locations.csv";

    /**
     * Combine a lower bound and size into an IP block.
     */
    class Block {
        Inet4Address first;
        long size;
        
        Block(Inet4Address first, long size) {
            this.first = first;
            this.size = size;
        }
        
        @Override
        public String toString() { 
            return first + "/" + size;
        }
    }
    
    Map<String, List<Block>> countryCodeToBlock;
    
    public Geolocation() {
        // Check for a cached local file, otherwise download a new version
        File localFile = new File(GEO_IP_LOCAL);
        try {
            if (!localFile.exists()) {
                FileUtils.copyURLToFile(new URL(GEO_IP_REMOTE), localFile);
            }
        } catch(IOException ex) {
            Logger.getLogger(Geolocation.class.getName()).log(Level.SEVERE, null, ex);
            return;
        }

        // Read the csv directly from the zip, stop once we have enough
        try (ZipFile zip = new ZipFile(localFile)) {
            // Load IP location blocks first
            // File is CSV: location_id, _, _, country_iso_code, country_name, _, _, _, _, _
            Map<Integer, String> locationIdToCountryCode = new HashMap<>();
            Collections.list(zip.entries())
                    .stream()
                    .filter((entry) -> {
                        return entry.getName().contains(GEO_IP_LOCATIONS_NAME);
                    })
                    .forEach((entry) -> {
                        try (Scanner locationsReader = new Scanner(zip.getInputStream(entry))) {
                            
                            locationsReader.nextLine();
                            while (locationsReader.hasNextLine()) {
                                try {

                                    String[] line = locationsReader.nextLine().split(",");
                                    int locationId = Integer.parseInt(line[0]);
                                    String isoCode = line[3];

                                    locationIdToCountryCode.put(locationId, isoCode);

                                } catch(ArrayIndexOutOfBoundsException | NumberFormatException ex) {
                                }
                            }
                            
                        } catch(IOException ex) {
                            Logger.getLogger(Geolocation.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    });

            // Then get the IP prefix blocks
            // File is CSV: ip, block size, location_id, _, _, _, _, _, is_anonymous_proxy, is_satellite_provider
            countryCodeToBlock = new HashMap<>();
            Collections.list(zip.entries())
                    .stream()
                    .filter((entry) -> {
                        return entry.getName().contains(GEO_IP_BLOCKS_NAME);
                    })
                    .forEach((entry) -> {
                        try (Scanner blocksReader = new Scanner(zip.getInputStream(entry))) {
                            
                            blocksReader.nextLine(); // Skip header
                            while (blocksReader.hasNextLine()) {
                                try {

                                    String[] line = blocksReader.nextLine().split(",");
                                    
                                    InetAddress first = InetAddress.getByName(line[0]);
                                    long size = Long.parseLong(line[1]);
                                    int locationID = Integer.parseInt(line[2]);
                                    
                                    if (!(first instanceof Inet4Address)) continue;
                                    
                                    String countryCode = locationIdToCountryCode.get(locationID);
                                    Block block = new Block((Inet4Address) first, size);
                                    
                                    if (countryCode == null) continue;

                                    countryCodeToBlock.putIfAbsent(countryCode, new ArrayList<>());
                                    countryCodeToBlock.get(countryCode).add(block);

                                } catch(ArrayIndexOutOfBoundsException | NumberFormatException e) {
                                }
                            }
                            
                        } catch(IOException ex) {
                            Logger.getLogger(Geolocation.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    });
            
            // DEBUG
            countryCodeToBlock.forEach((countryCode, blocks) -> {
                System.out.println(
                        countryCode + " has " + 
                        blocks.size() + " blocks containing " + 
                        blocks.stream().collect(Collectors.summingLong((block) -> block.size)) + " IPs"
                        );
            });

        } catch (IOException ex) {
            Logger.getLogger(Geolocation.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    /**
     * Given a country code, iterate over IP address more or less randomly.
     * 
     * Specifically, choose a random prefix in that country than a random IP
     * in that prefix.
     * 
     * These iterators will never end, so I hope you have an exit strategy. :)
     * 
     * @param countryCode 
     * @return IP addresses.
     */
    public Iterable<Inet4Address> ipsByCountry(String countryCode) {
        Random r = new Random();
        List<Block> blocks = countryCodeToBlock.get(countryCode);
        
        return new Iterable<Inet4Address>() {
            @Override
            public Iterator<Inet4Address> iterator() {
                return new Iterator<Inet4Address>() {

                    @Override
                    public boolean hasNext() {
                        return true;
                    }

                    @Override
                    public Inet4Address next() {
                        
                        Block block = blocks.get(r.nextInt(blocks.size()));
                        long ip = 
                                block.first.getAddress()[0] * 256 * 256 * 256 +
                                block.first.getAddress()[1] * 256 * 256 +
                                block.first.getAddress()[2] * 256 +
                                block.first.getAddress()[3] +
                                r.nextInt((int) block.size);

                        byte[] addr = new byte[4];
                        for (int i = 3; i >= 0; i--) {
                            addr[i] = (byte) (ip % 256);
                            ip /= 256;
                        }

                        try {
                            return (Inet4Address) InetAddress.getByAddress(addr);
                        } catch (UnknownHostException ex) {
                            Logger.getLogger(Geolocation.class.getName()).log(Level.SEVERE, null, ex);
                            return (Inet4Address) InetAddress.getLoopbackAddress();
                        }
                        
                    }
                };
            }
        };
    }
}
