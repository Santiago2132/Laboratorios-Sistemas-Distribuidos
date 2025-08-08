package classes;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import interfaces.InterfaceSong;

public class MusicServer implements InterfaceSong {
    
    private int port;
    private ServerSocket serverSk;
    private Socket clientSk;
    private ObjectOutputStream oos;
    private ObjectInputStream ois;
    private List<Song> database;
    
    public MusicServer(int port) throws IOException {
        this.port = port;
        try {
            this.serverSk = new ServerSocket(port, 100);
            this.oos = null;
            this.ois = null;
            initializeDatabase();
            System.out.println("[Music Server]: Servidor iniciado correctamente en puerto " + port);
        } catch (IOException e) {
            System.err.println("[Music Server]: Error al iniciar servidor en puerto " + port);
            System.err.println("[Music Server]: " + e.getMessage());
            throw e; // Re-lanzar la excepción para que el programa no continúe
        }
    }
    
    private void initializeDatabase() {
        database = new ArrayList<>();
        
        // Base de datos con canciones de ejemplo
        database.add(new Song("Bohemian Rhapsody", "Rock", "Queen", "English", 1975));
        database.add(new Song("Hotel California", "Rock", "Eagles", "English", 1976));
        database.add(new Song("Imagine", "Pop", "John Lennon", "English", 1971));
        database.add(new Song("Like a Rolling Stone", "Rock", "Bob Dylan", "English", 1965));
        database.add(new Song("Smells Like Teen Spirit", "Grunge", "Nirvana", "English", 1991));
        database.add(new Song("Billie Jean", "Pop", "Michael Jackson", "English", 1983));
        database.add(new Song("Satisfaction", "Rock", "The Rolling Stones", "English", 1965));
        database.add(new Song("Hey Jude", "Pop", "The Beatles", "English", 1968));
        database.add(new Song("Purple Haze", "Rock", "Jimi Hendrix", "English", 1967));
        database.add(new Song("Stairway to Heaven", "Rock", "Led Zeppelin", "English", 1971));
        database.add(new Song("La Vida es Una Fiesta", "Salsa", "Celia Cruz", "Spanish", 1982));
        database.add(new Song("Despacito", "Reggaeton", "Luis Fonsi", "Spanish", 2017));
        database.add(new Song("Careless Whisper", "Pop", "George Michael", "English", 1984));
        database.add(new Song("Sweet Child O' Mine", "Rock", "Guns N' Roses", "English", 1987));
        database.add(new Song("Thunderstruck", "Rock", "AC/DC", "English", 1990));
    }
    
    public void listening() {
        if (serverSk == null) {
            System.err.println("[Music Server]: No se puede iniciar el servidor - ServerSocket es null");
            return;
        }
        
        try {
            System.out.println("[Music Server]: Esperando conexiones en puerto " + port);
            
            while (true) {
                clientSk = serverSk.accept();
                oos = new ObjectOutputStream(clientSk.getOutputStream());
                oos.flush();
                ois = new ObjectInputStream(clientSk.getInputStream());
                System.out.println("[Music Server]: Cliente conectado");
                
                try {
                    while (true) {
                        String request = (String) ois.readObject();
                        if (request == null) {
                            break;
                        }
                        
                        System.out.println("[Music Server]: Solicitud recibida: " + request);
                        processRequest(request);
                    }
                } catch (Exception e) {
                    System.out.println("[Music Server]: Cliente desconectado");
                } finally {
                    closeClient();
                }
            }
        } catch (IOException e) {
            System.err.println("[Music Server]: Error en el servidor: " + e.getMessage());
        }
    }
    
    private void processRequest(String request) {
        try {
            String[] parts = request.split("\\|");
            String action = parts[0];
            List<Song> results = new ArrayList<>();
            
            switch (action) {
                case "SEARCH_TITLE":
                    if (parts.length > 1) {
                        results = searchByTitle(parts[1]);
                    }
                    break;
                case "SEARCH_GENRE":
                    if (parts.length > 1) {
                        results = searchByGenre(parts[1]);
                    }
                    break;
                case "SEARCH_AUTHOR":
                    if (parts.length > 1) {
                        results = searchByAuthor(parts[1]);
                    }
                    break;
                case "SEARCH_MULTIPLE":
                    String title = parts.length > 1 ? parts[1] : "";
                    String genre = parts.length > 2 ? parts[2] : "";
                    String author = parts.length > 3 ? parts[3] : "";
                    results = searchByMultipleCriteria(title, genre, author);
                    break;
                case "GET_ALL":
                    results = new ArrayList<>(database);
                    break;
                default:
                    System.out.println("[Music Server]: Acción no reconocida: " + action);
                    break;
            }
            
            sendResults(results);
        } catch (Exception e) {
            System.out.println("[Music Server]: Error procesando solicitud: " + e.getMessage());
        }
    }
    
    private void sendResults(List<Song> results) {
        try {
            oos.writeObject(results);
            oos.flush();
            System.out.println("[Music Server]: Enviados " + results.size() + " resultados");
        } catch (Exception e) {
            System.out.println("[Music Server]: Error enviando resultados: " + e.getMessage());
        }
    }
    
    private void closeClient() {
        try {
            if (ois != null) ois.close();
            if (oos != null) oos.close();
            if (clientSk != null) clientSk.close();
        } catch (Exception e) {
            System.out.println("[Music Server]: Error cerrando conexión: " + e.getMessage());
        }
    }
    
    @Override
    public List<Song> searchByTitle(String title) {
        List<Song> results = new ArrayList<>();
        for (Song song : database) {
            if (song.getTitle().toLowerCase().contains(title.toLowerCase())) {
                results.add(song);
            }
        }
        return results;
    }
    
    @Override
    public List<Song> searchByGenre(String genre) {
        List<Song> results = new ArrayList<>();
        for (Song song : database) {
            if (song.getGenre().toLowerCase().contains(genre.toLowerCase())) {
                results.add(song);
            }
        }
        return results;
    }
    
    @Override
    public List<Song> searchByAuthor(String author) {
        List<Song> results = new ArrayList<>();
        for (Song song : database) {
            if (song.getAuthor().toLowerCase().contains(author.toLowerCase())) {
                results.add(song);
            }
        }
        return results;
    }
    
    @Override
    public List<Song> searchByMultipleCriteria(String title, String genre, String author) {
        List<Song> results = new ArrayList<>();
        for (Song song : database) {
            boolean matches = true;
            
            if (!title.isEmpty() && !song.getTitle().toLowerCase().contains(title.toLowerCase())) {
                matches = false;
            }
            if (!genre.isEmpty() && !song.getGenre().toLowerCase().contains(genre.toLowerCase())) {
                matches = false;
            }
            if (!author.isEmpty() && !song.getAuthor().toLowerCase().contains(author.toLowerCase())) {
                matches = false;
            }
            
            if (matches) {
                results.add(song);
            }
        }
        return results;
    }
    
    public void close() {
        try {
            if (serverSk != null && !serverSk.isClosed()) {
                serverSk.close();
                System.out.println("[Music Server]: Servidor cerrado correctamente");
            }
        } catch (IOException e) {
            System.out.println("[Music Server]: Error cerrando servidor: " + e.getMessage());
        }
    }
}