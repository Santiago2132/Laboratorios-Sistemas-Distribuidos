import java.net.HttpURLConnection;
import java.net.URL;
import java.io.OutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Element;
import java.io.ByteArrayInputStream;

public class MusicLibraryClient {
    
    private static final String SOAP_ENDPOINT = "http://localhost:8080";
    private Scanner scanner;
    
    public MusicLibraryClient() {
        this.scanner = new Scanner(System.in);
    }
    
    public static void main(String[] args) {
        MusicLibraryClient client = new MusicLibraryClient();
        client.startClient();
    }
    
    public void startClient() {
        System.out.println("=== BIBLIOTECA DE MÚSICA ===");
        System.out.println("Conectado al servidor SOAP.");
        
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
        
        String soapRequest = createSOAPRequest("searchByAuthor", 
            "<arg0>" + escapeXml(author) + "</arg0>");
        
        try {
            String response = sendSOAPRequest(soapRequest);
            List<Song> results = parseSearchResponse(response);
            displayResults(results);
        } catch (Exception e) {
            System.out.println("[Music Client]: Error buscando por autor: " + e.getMessage());
        }
    }
    
    private void searchByTitle() {
        System.out.print("Ingrese el título de la canción: ");
        String title = scanner.nextLine();
        
        String soapRequest = createSOAPRequest("searchByTitle", 
            "<arg0>" + escapeXml(title) + "</arg0>");
        
        try {
            String response = sendSOAPRequest(soapRequest);
            List<Song> results = parseSearchResponse(response);
            displayResults(results);
        } catch (Exception e) {
            System.out.println("[Music Client]: Error buscando por título: " + e.getMessage());
        }
    }
    
    private void searchByGenre() {
        System.out.print("Ingrese el género: ");
        String genre = scanner.nextLine();
        
        String soapRequest = createSOAPRequest("searchByGenre", 
            "<arg0>" + escapeXml(genre) + "</arg0>");
        
        try {
            String response = sendSOAPRequest(soapRequest);
            List<Song> results = parseSearchResponse(response);
            displayResults(results);
        } catch (Exception e) {
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
        
        String parameters = "<arg0>" + escapeXml(title) + "</arg0>" +
                           "<arg1>" + escapeXml(genre) + "</arg1>" +
                           "<arg2>" + escapeXml(author) + "</arg2>";
        
        String soapRequest = createSOAPRequest("searchByMultipleCriteria", parameters);
        
        try {
            String response = sendSOAPRequest(soapRequest);
            List<Song> results = parseSearchResponse(response);
            displayResults(results);
        } catch (Exception e) {
            System.out.println("[Music Client]: Error en búsqueda múltiple: " + e.getMessage());
        }
    }
    
    private void showAllSongs() {
        String parameters = "<arg0></arg0><arg1></arg1><arg2></arg2>";
        String soapRequest = createSOAPRequest("searchByMultipleCriteria", parameters);
        
        try {
            String response = sendSOAPRequest(soapRequest);
            List<Song> results = parseSearchResponse(response);
            displayResults(results);
        } catch (Exception e) {
            System.out.println("[Music Client]: Error obteniendo todas las canciones: " + e.getMessage());
        }
    }
    
    private String createSOAPRequest(String methodName, String parameters) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
               "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\" " +
               "xmlns:ser=\"http://service.musiclibrary.com/\">" +
               "<soap:Header/>" +
               "<soap:Body>" +
               "<ser:" + methodName + ">" +
               parameters +
               "</ser:" + methodName + ">" +
               "</soap:Body>" +
               "</soap:Envelope>";
    }
    
    private String sendSOAPRequest(String soapRequest) throws Exception {
        URL url = new URL(SOAP_ENDPOINT);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "text/xml; charset=utf-8");
        connection.setRequestProperty("SOAPAction", "");
        connection.setDoOutput(true);
        
        try (OutputStream os = connection.getOutputStream()) {
            os.write(soapRequest.getBytes("UTF-8"));
        }
        
        StringBuilder response = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(connection.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        }
        
        return response.toString();
    }
    
    private List<Song> parseSearchResponse(String xmlResponse) throws Exception {
        List<Song> songs = new ArrayList<>();
        
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new ByteArrayInputStream(xmlResponse.getBytes()));
        
        NodeList returnElements = doc.getElementsByTagName("return");
        
        for (int i = 0; i < returnElements.getLength(); i++) {
            Element returnElement = (Element) returnElements.item(i);
            Song song = new Song();
            
            NodeList titles = returnElement.getElementsByTagName("title");
            if (titles.getLength() > 0) {
                song.setTitle(titles.item(0).getTextContent());
            }
            
            NodeList genres = returnElement.getElementsByTagName("genre");
            if (genres.getLength() > 0) {
                song.setGenre(genres.item(0).getTextContent());
            }
            
            NodeList authors = returnElement.getElementsByTagName("author");
            if (authors.getLength() > 0) {
                song.setAuthor(authors.item(0).getTextContent());
            }
            
            NodeList languages = returnElement.getElementsByTagName("language");
            if (languages.getLength() > 0) {
                song.setLanguage(languages.item(0).getTextContent());
            }
            
            NodeList years = returnElement.getElementsByTagName("year");
            if (years.getLength() > 0) {
                try {
                    song.setYear(Integer.parseInt(years.item(0).getTextContent()));
                } catch (NumberFormatException e) {
                    song.setYear(0);
                }
            }
            
            songs.add(song);
        }
        
        return songs;
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
    
    private String escapeXml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&apos;");
    }
    
    // Song class for client-side representation
    private static class Song {
        private String title;
        private String genre;
        private String author;
        private String language;
        private int year;
        
        public Song() {}
        
        // Getters and setters
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        
        public String getGenre() { return genre; }
        public void setGenre(String genre) { this.genre = genre; }
        
        public String getAuthor() { return author; }
        public void setAuthor(String author) { this.author = author; }
        
        public String getLanguage() { return language; }
        public void setLanguage(String language) { this.language = language; }
        
        public int getYear() { return year; }
        public void setYear(int year) { this.year = year; }
    }
}