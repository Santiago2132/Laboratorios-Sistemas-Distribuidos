import java.nio.file.*;
import java.util.*;

public class CLIInterface {
    private static final String DEFAULT_OUTPUT_DIR = "./pdfs";
    private static final String DEFAULT_CHROME_PATH = "/usr/bin/google-chrome"; // Linux
    // Windows: "C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe"
    // macOS: "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome"
    
    public static void main(String[] args) {
        System.out.println("=== Web to PDF Converter ===\n");
        
        Scanner scanner = new Scanner(System.in);
        
        // Configuración
        String outputDir = getInput(scanner, "Directorio de salida", DEFAULT_OUTPUT_DIR);
        String chromePath = getInput(scanner, "Ruta de Chrome", DEFAULT_CHROME_PATH);
        
        // Validar Chrome
        if (!Files.exists(Paths.get(chromePath))) {
            System.err.println("Error: Chrome no encontrado en: " + chromePath);
            System.exit(1);
        }
        
        WebToPDFConverter converter = new WebToPDFConverter(outputDir, chromePath);
        
        // Obtener URLs
        List<String> urls = getUrls(scanner);
        
        // Obtener número de hilos
        int threads = getThreadCount(scanner);
        
        // Ejecutar conversión
        System.out.println("\nIniciando conversión con " + threads + " hilos...");
        WebToPDFConverter.ConversionResult result = converter.convertUrls(urls, threads);
        
        // Mostrar resultados
        displayResults(result);
        
        scanner.close();
    }
    
    private static String getInput(Scanner scanner, String prompt, String defaultValue) {
        System.out.print(prompt + " [" + defaultValue + "]: ");
        String input = scanner.nextLine().trim();
        return input.isEmpty() ? defaultValue : input;
    }
    
    private static List<String> getUrls(Scanner scanner) {
        List<String> urls = new ArrayList<>();
        System.out.println("\nIngrese URLs (línea vacía para terminar):");
        
        while (true) {
            System.out.print("URL " + (urls.size() + 1) + ": ");
            String url = scanner.nextLine().trim();
            
            if (url.isEmpty()) break;
            
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                url = "https://" + url;
            }
            
            urls.add(url);
        }
        
        if (urls.isEmpty()) {
            System.err.println("Error: Debe proporcionar al menos una URL");
            System.exit(1);
        }
        
        return urls;
    }
    
    private static int getThreadCount(Scanner scanner) {
        while (true) {
            System.out.print("Número de hilos [4]: ");
            String input = scanner.nextLine().trim();
            
            if (input.isEmpty()) return 4;
            
            try {
                int threads = Integer.parseInt(input);
                if (threads > 0 && threads <= 32) {
                    return threads;
                } else {
                    System.out.println("Error: Número de hilos debe estar entre 1 y 32");
                }
            } catch (NumberFormatException e) {
                System.out.println("Error: Ingrese un número válido");
            }
        }
    }
    
    private static void displayResults(WebToPDFConverter.ConversionResult result) {
        System.out.println("\n=== Resultados ===");
        System.out.println(result);
        
        if (!result.successfulPdfs.isEmpty()) {
            System.out.println("\nPDFs creados exitosamente:");
            result.successfulPdfs.forEach(path -> System.out.println("  " + path));
        }
        
        if (!result.errors.isEmpty()) {
            System.out.println("\nErrores:");
            result.errors.forEach(error -> System.out.println("  " + error));
        }
    }
}