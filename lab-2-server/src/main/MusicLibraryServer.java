package main;

import classes.MusicServer;
import java.io.IOException;

public class MusicLibraryServer {
    
    public static void main(String[] args) {
        MusicServer server = null;
        
        try {
            server = new MusicServer(1804);
            
            // Agregar shutdown hook para cerrar el servidor correctamente
            final MusicServer finalServer = server;
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("\n[Music Server]: Cerrando servidor...");
                finalServer.close();
            }));
            
            server.listening();
            
        } catch (IOException e) {
            System.err.println("[Music Server]: No se pudo iniciar el servidor en el puerto 1802");
            System.err.println("[Music Server]: Verifica que el puerto no esté siendo usado por otro proceso");
            System.err.println("[Music Server]: Error: " + e.getMessage());
            
            // Sugerencias para el usuario
            System.out.println("\nSugerencias para solucionar el problema:");
            System.out.println("1. Ejecuta: sudo netstat -tulpn | grep 1802");
            System.out.println("2. Si hay un proceso usando el puerto, termínalo con: sudo kill -9 <PID>");
            System.out.println("3. O cambia el puerto en el código a otro número (ej: 1803)");
            
            System.exit(1);
        } catch (Exception e) {
            System.err.println("[Music Server]: Error inesperado: " + e.getMessage());
            if (server != null) {
                server.close();
            }
            System.exit(1);
        }
    }
}