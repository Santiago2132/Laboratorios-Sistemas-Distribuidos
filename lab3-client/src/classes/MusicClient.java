package classes;

import java.rmi.RemoteException;
import java.util.List;
import java.util.Scanner;
import interfaces.InterfaceSong;

public class MusicClient {
    
    private InterfaceSong songService;
    private Scanner scanner;
    
    public MusicClient(InterfaceSong songService) {
        this.songService = songService;
        this.scanner = new Scanner(System.in);
    }
    
    public void startClient() {
        System.out.println("=== BIBLIOTECA DE MÚSICA ===");
        System.out.println("Conectado al servidor RMI.");
        
        int choice = 0;
        while (choice != 6) {
            showMenu();
            choice = scanner.nextInt();
            scanner.nextLine();
            
            switch (choice) {
                case 1:
                    searchByAuthor();
                    break;
                case 2:
                    searchByMultipleCriteriaMenu();
                    break;
                case 3:
                    showAllSongs();
                    break;
                case 4:
                    searchByTitle();
                    break;
                case 5:
                    searchByGenre();
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
    
    private void searchByAuthor() {
        System.out.print("Ingrese el nombre del autor: ");
        String author = scanner.nextLine();
        try {
            List<Song> results = songService.searchByAuthor(author);
            displayResults(results);
        } catch (RemoteException e) {
            System.out.println("[Music Client]: Error buscando por autor: " + e.getMessage());
        }
    }
    
    private void searchByTitle() {
        System.out.print("Ingrese el título de la canción: ");
        String title = scanner.nextLine();
        try {
            List<Song> results = songService.searchByTitle(title);
            displayResults(results);
        } catch (RemoteException e) {
            System.out.println("[Music Client]: Error buscando por título: " + e.getMessage());
        }
    }
    
    private void searchByGenre() {
        System.out.print("Ingrese el género: ");
        String genre = scanner.nextLine();
        try {
            List<Song> results = songService.searchByGenre(genre);
            displayResults(results);
        } catch (RemoteException e) {
            System.out.println("[Music Client]: Error buscando por género: " + e.getMessage());
        }
    }
    
    private void searchByMultipleCriteriaMenu() {
        System.out.println("=== BÚSQUEDA AVANZADA ===");
        System.out.print("Título (presione Enter para omitir): ");
        String title = scanner.nextLine();
        System.out.print("Género (presione Enter para omitir): ");
        String genre = scanner.nextLine();
        System.out.print("Autor (presione Enter para omitir): ");
        String author = scanner.nextLine();
        
        try {
            List<Song> results = songService.searchByMultipleCriteria(title, genre, author);
            displayResults(results);
        } catch (RemoteException e) {
            System.out.println("[Music Client]: Error en búsqueda múltiple: " + e.getMessage());
        }
    }
    
    private void showAllSongs() {
        try {
            List<Song> results = songService.searchByMultipleCriteria("", "", "");
            displayResults(results);
        } catch (RemoteException e) {
            System.out.println("[Music Client]: Error obteniendo todas las canciones: " + e.getMessage());
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
}