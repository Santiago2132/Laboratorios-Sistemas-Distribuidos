import java.net.ServerSocket;
import java.net.Socket;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class SOAPServer {
    
    private List<Song> database;
    private ServerSocket serverSocket;
    
    public SOAPServer() {
        initializeDatabase();
    }
    
    private void initializeDatabase() {
        database = new ArrayList<>();
        database.add(new Song("One bite on the Dust", "Rock", "Queen", "English", 1975));
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
    
    public void start(int port) throws IOException {
        serverSocket = new ServerSocket(port);
        System.out.println("[SOAP Server]: Servidor iniciado en puerto " + port);
        System.out.println("[SOAP Server]: Esperando conexiones SOAP...");
        
        while (true) {
            Socket clientSocket = serverSocket.accept();
            new Thread(new ClientHandler(clientSocket)).start();
        }
    }
    
    private class ClientHandler implements Runnable {
        private Socket clientSocket;
        
        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }
        
        public void run() {
            try {
                BufferedReader reader = new BufferedReader(
                    new InputStreamReader(clientSocket.getInputStream()));
                PrintWriter writer = new PrintWriter(
                    clientSocket.getOutputStream(), true);
                
                // Leer HTTP headers
                String line;
                int contentLength = 0;
                while ((line = reader.readLine()) != null && !line.isEmpty()) {
                    if (line.startsWith("Content-Length:")) {
                        contentLength = Integer.parseInt(line.substring(16).trim());
                    }
                }
                
                // Leer SOAP body
                char[] buffer = new char[contentLength];
                reader.read(buffer, 0, contentLength);
                String soapRequest = new String(buffer);
                
                // Procesar SOAP request
                String soapResponse = processSOAPRequest(soapRequest);
                
                // Enviar HTTP response
                writer.println("HTTP/1.1 200 OK");
                writer.println("Content-Type: text/xml; charset=utf-8");
                writer.println("Content-Length: " + soapResponse.length());
                writer.println();
                writer.print(soapResponse);
                writer.flush();
                
                clientSocket.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    private String processSOAPRequest(String soapRequest) {
        try {
            System.out.println("[SOAP Server]: Procesando request: " + soapRequest);
            
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new ByteArrayInputStream(soapRequest.getBytes()));
            
            // Buscar método en cualquier namespace
            String[] possibleMethods = {"searchByTitle", "searchByGenre", "searchByAuthor", "searchByMultipleCriteria"};
            
            for (String method : possibleMethods) {
                NodeList methodNodes = doc.getElementsByTagName("*");
                for (int i = 0; i < methodNodes.getLength(); i++) {
                    Element element = (Element) methodNodes.item(i);
                    String localName = element.getLocalName();
                    
                    if (method.equals(localName)) {
                        switch (method) {
                            case "searchByTitle":
                                String title = getParameterValue(element, "arg0");
                                return createSOAPResponse(searchByTitle(title), "searchByTitleResponse");
                                
                            case "searchByGenre":
                                String genre = getParameterValue(element, "arg0");
                                return createSOAPResponse(searchByGenre(genre), "searchByGenreResponse");
                                
                            case "searchByAuthor":
                                String author = getParameterValue(element, "arg0");
                                return createSOAPResponse(searchByAuthor(author), "searchByAuthorResponse");
                                
                            case "searchByMultipleCriteria":
                                String titleParam = getParameterValue(element, "arg0");
                                String genreParam = getParameterValue(element, "arg1");
                                String authorParam = getParameterValue(element, "arg2");
                                return createSOAPResponse(
                                    searchByMultipleCriteria(titleParam, genreParam, authorParam), 
                                    "searchByMultipleCriteriaResponse");
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return createSOAPFault("Invalid request");
    }
    
    private String getParameterValue(Element methodElement, String paramName) {
        NodeList params = methodElement.getElementsByTagName(paramName);
        if (params.getLength() > 0) {
            return params.item(0).getTextContent();
        }
        return "";
    }
    
    private String createSOAPResponse(List<Song> songs, String methodName) {
        StringBuilder response = new StringBuilder();
        response.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        response.append("<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">");
        response.append("<soap:Body>");
        response.append("<ns2:").append(methodName).append(" xmlns:ns2=\"http://service.musiclibrary.com/\">");
        
        for (Song song : songs) {
            response.append("<return>");
            response.append("<title>").append(escapeXml(song.getTitle())).append("</title>");
            response.append("<genre>").append(escapeXml(song.getGenre())).append("</genre>");
            response.append("<author>").append(escapeXml(song.getAuthor())).append("</author>");
            response.append("<language>").append(escapeXml(song.getLanguage())).append("</language>");
            response.append("<year>").append(song.getYear()).append("</year>");
            response.append("</return>");
        }
        
        response.append("</ns2:").append(methodName).append(">");
        response.append("</soap:Body>");
        response.append("</soap:Envelope>");
        
        return response.toString();
    }
    
    private String createSOAPFault(String message) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
               "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">" +
               "<soap:Body>" +
               "<soap:Fault>" +
               "<faultcode>Server</faultcode>" +
               "<faultstring>" + escapeXml(message) + "</faultstring>" +
               "</soap:Fault>" +
               "</soap:Body>" +
               "</soap:Envelope>";
    }
    
    private String escapeXml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&apos;");
    }
    
    // Métodos de búsqueda
    public List<Song> searchByTitle(String title) {
        List<Song> results = new ArrayList<>();
        for (Song song : database) {
            if (song.getTitle().toLowerCase().contains(title.toLowerCase())) {
                results.add(song);
            }
        }
        return results;
    }
    
    public List<Song> searchByGenre(String genre) {
        List<Song> results = new ArrayList<>();
        for (Song song : database) {
            if (song.getGenre().toLowerCase().contains(genre.toLowerCase())) {
                results.add(song);
            }
        }
        return results;
    }
    
    public List<Song> searchByAuthor(String author) {
        List<Song> results = new ArrayList<>();
        for (Song song : database) {
            if (song.getAuthor().toLowerCase().contains(author.toLowerCase())) {
                results.add(song);
            }
        }
        return results;
    }
    
    public List<Song> searchByMultipleCriteria(String title, String genre, String author) {
        List<Song> results = new ArrayList<>();
        for (Song song : database) {
            boolean matches = true;
            if (title != null && !title.isEmpty() && !song.getTitle().toLowerCase().contains(title.toLowerCase())) {
                matches = false;
            }
            if (genre != null && !genre.isEmpty() && !song.getGenre().toLowerCase().contains(genre.toLowerCase())) {
                matches = false;
            }
            if (author != null && !author.isEmpty() && !song.getAuthor().toLowerCase().contains(author.toLowerCase())) {
                matches = false;
            }
            if (matches) {
                results.add(song);
            }
        }
        return results;
    }
    
    public static void main(String[] args) {
        try {
            SOAPServer server = new SOAPServer();
            server.start(8080);
        } catch (IOException e) {
            System.err.println("[SOAP Server]: Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}