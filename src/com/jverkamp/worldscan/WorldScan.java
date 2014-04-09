package com.jverkamp.worldscan;

import com.jverkamp.worldscan.data.AlexaTopSites;
import com.jverkamp.worldscan.data.Geolocation;
import com.jverkamp.worldscan.scanner.DNSScanner;
import com.jverkamp.worldscan.scanner.ScanResult;
import com.jverkamp.worldscan.scanner.Scanner;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Scan the world for censorship.
 */
public class WorldScan {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        
        for (Inet4Address ip : new Geolocation().ipsByCountry("NU")) {
            System.out.println(ip);
        }
        
//        try {
//            Scanner s = new DNSScanner(InetAddress.getByName("8.8.4.4"), 10);
//            for (ScanResult result : s) {
//                System.out.println(result);
//            }
//            
//        } catch (UnknownHostException ex) {
//            Logger.getLogger(WorldScan.class.getName()).log(Level.SEVERE, null, ex);
//        }
    }
}
