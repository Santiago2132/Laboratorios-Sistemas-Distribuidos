package main;

import classes.MusicServer;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class MusicLibraryServer {
    
    public static void main(String[] args) {
        try {
            MusicServer server = new MusicServer();
            
            Registry registry = LocateRegistry.createRegistry(1099);
            registry.rebind("MusicLibrary", server);
            
            System.out.println("[Music Server]: Server ready");
        } catch (Exception e) {
            System.err.println("[Music Server]: Server exception: " + e.toString());
            e.printStackTrace();
        }
    }
}