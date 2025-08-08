package classes;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import interfaces.InterfaceSong;

public class MusicClient implements InterfaceSong {
    
    private InetAddress address;
    private int port;
    private Socket clientSk;
    private ObjectOutputStream oos;
    private ObjectInputStream ois;
    private Scanner scanner;
    
    public MusicClient(String address, int port) {
        try {
            this.port = port;
            this.address = InetAddress.getByName(address);
            this.scanner = new Scanner(System.in);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }
    
    public void startClient() {
        System.out.println("=== BIBLIOTECA DE MÚSICA ===");
        System.out.println("Conectando al servidor...");
        
        int choice = 0;
        while (choice != 6) {
            showMenu();
            choice = scanner.nextInt();
            scanner.nextLine(); // Consumir el salto de línea
            
            switch (choice) {
                case 1:
                    searchBySingleCriteria();
                    break;
                case 2:
                    searchByMultipleCriteriaMenu();
                    break;
                case 3:
                    showAllSongs();
                    break;
                case 4:
                    searchByTitleMenu();
                    break;
                case 5:
                    searchByGenreMenu();
                    break;
                case 6:
                    System.out.println("¡Hasta luego!");
                    break;
                default:
                    System.out.println("Opción inválida. Intente nuevamente.");
            }
        }
    }
    
    private void showMenu() {
        System.out.println("\n=== MENÚ PRINCIPAL ===");
        System.out.println("1. Buscar por autor");
        System.out.println("2. Búsqueda avanzada (múltiples criterios)");
        System.out.println("3. Mostrar todas las canciones");
        System.out.println("4. Buscar por título");
        System.out.println("5. Buscar por género");
        System.out.println("6. Salir");
        System.out.print("Seleccione una opción: ");
    }
    
    private void searchBySingleCriteria() {
        System.out.print("Ingrese el nombre del autor: ");
        String author = scanner.nextLine();
        List<Song> results = searchByAuthor(author);
        displayResults(results);
    }
    
    private void searchByTitleMenu() {
        System.out.print("Ingrese el título de la canción: ");
        String title = scanner.nextLine();
        List<Song> results = searchByTitle(title);
        displayResults(results);
    }
    
    private void searchByGenreMenu() {
        System.out.print("Ingrese el género: ");
        String genre = scanner.nextLine();
        List<Song> results = searchByGenre(genre);
        displayResults(results);
    }
    
    private void searchByMultipleCriteriaMenu() {
        System.out.println("=== BÚSQUEDA AVANZADA ===");
        System.out.print("Título (presione Enter para omitir): ");
        String title = scanner.nextLine();
        System.out.print("Género (presione Enter para omitir): ");
        String genre = scanner.nextLine();
        System.out.print("Autor (presione Enter para omitir): ");
        String author = scanner.nextLine();
        
        List<Song> results = searchByMultipleCriteria(title, genre, author);
        displayResults(results);
    }
    
    private void showAllSongs() {
        try {
            connect();
            sendRequest("GET_ALL");
            List<Song> results = receiveResults();
            displayResults(results);
        } catch (Exception e) {
            System.out.println("[Music Client]: Error obteniendo todas las canciones: " + e.getMessage());
        } finally {
            closeConnection();
        }
    }
    
    private void displayResults(List<Song> results) {
        System.out.println("\n=== RESULTADOS ===");
        if (results.isEmpty()) {
            System.out.println("No se encontraron canciones que coincidan con los criterios.");
        } else {
            System.out.println("Se encontraron " + results.size() + " canción(es):");
            System.out.println();
            for (int i = 0; i < results.size(); i++) {
                Song song = results.get(i);
                System.out.println((i + 1) + ". " + song.getTitle());
                System.out.println("   Artista: " + song.getAuthor());
                System.out.println("   Género: " + song.getGenre());
                System.out.println("   Idioma: " + song.getLanguage());
                System.out.println("   Año: " + song.getYear());
                System.out.println();
            }
        }
    }
    
    private void connect() throws IOException {
        clientSk = new Socket(address, port);
        oos = new ObjectOutputStream(clientSk.getOutputStream());
        oos.flush();
        ois = new ObjectInputStream(clientSk.getInputStream());
    }
    
    private void sendRequest(String request) throws IOException {
        oos.writeObject(request);
        oos.flush();
    }
    
    @SuppressWarnings("unchecked")
    private List<Song> receiveResults() throws IOException, ClassNotFoundException {
        return (List<Song>) ois.readObject();
    }
    
    private void closeConnection() {
        try {
            if (ois != null) ois.close();
            if (oos != null) oos.close();
            if (clientSk != null) clientSk.close();
        } catch (Exception e) {
            System.out.println("[Music Client]: Error cerrando conexión: " + e.getMessage());
        }
    }
    
    @Override
    public List<Song> searchByTitle(String title) {
        try {
            connect();
            sendRequest("SEARCH_TITLE|" + title);
            return receiveResults();
        } catch (Exception e) {
            System.out.println("[Music Client]: Error buscando por título: " + e.getMessage());
            return new ArrayList<>();
        } finally {
            closeConnection();
        }
    }
    
    @Override
    public List<Song> searchByGenre(String genre) {
        try {
            connect();
            sendRequest("SEARCH_GENRE|" + genre);
            return receiveResults();
        } catch (Exception e) {
            System.out.println("[Music Client]: Error buscando por género: " + e.getMessage());
            return new ArrayList<>();
        } finally {
            closeConnection();
        }
    }
    
    @Override
    public List<Song> searchByAuthor(String author) {
        try {
            connect();
            sendRequest("SEARCH_AUTHOR|" + author);
            return receiveResults();
        } catch (Exception e) {
            System.out.println("[Music Client]: Error buscando por autor: " + e.getMessage());
            return new ArrayList<>();
        } finally {
            closeConnection();
        }
    }
    
    @Override
    public List<Song> searchByMultipleCriteria(String title, String genre, String author) {
        try {
            connect();
            sendRequest("SEARCH_MULTIPLE|" + title + "|" + genre + "|" + author);
            return receiveResults();
        } catch (Exception e) {
            System.out.println("[Music Client]: Error en búsqueda múltiple: " + e.getMessage());
            return new ArrayList<>();
        } finally {
            closeConnection();
        }
    }
}