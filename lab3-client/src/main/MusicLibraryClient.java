package main;

import classes.MusicClient;
import interfaces.InterfaceSong;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class MusicLibraryClient {
    
    public static void main(String[] args) {
        try {
            // Conectar al registro RMI
            Registry registry = LocateRegistry.getRegistry("127.0.0.1", 1099);
            InterfaceSong songService = (InterfaceSong) registry.lookup("MusicLibrary");
            
            // Crear cliente con el objeto remoto
            MusicClient client = new MusicClient(songService);
            client.startClient();
        } catch (Exception e) {
            System.err.println("[Music Client]: Excepción en el cliente: " + e.toString());
            e.printStackTrace();
        }
    }
}