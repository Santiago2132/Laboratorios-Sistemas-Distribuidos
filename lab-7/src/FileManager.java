import java.io.*;
import java.nio.file.*;
import java.util.*;

public class FileManager {
    private static final String FILE_PATHS_FILE = "file_paths.txt";
    private static final String OUTPUT_DIRECTORY = "converted_pdfs";
    private static final String REPORTS_DIRECTORY = "reports";
    
    public static List<String> loadFilePaths() {
        List<String> paths = new ArrayList<>();
        Path pathsFile = Paths.get(FILE_PATHS_FILE);
        
        if (!Files.exists(pathsFile)) {
            createDefaultFilePathsFile();
        }
        
        try {
            paths = Files.readAllLines(pathsFile);
            paths = paths.stream()
                    .map(String::trim)
                    .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                    .toList();
            
            System.out.println("Archivos cargados desde " + FILE_PATHS_FILE + ": " + paths.size());
            
        } catch (IOException e) {
            System.err.println("Error leyendo archivo de rutas: " + e.getMessage());
            System.out.println("Usando rutas vacías.");
        }
        
        return paths;
    }
    
    private static void createDefaultFilePathsFile() {
        try {
            List<String> defaultContent = Arrays.asList(
                "# Agregue las rutas de los archivos a convertir, una por línea",
                "# Ejemplo:",
                "# C:\\Documents\\archivo1.docx",
                "# C:\\Documents\\archivo2.xlsx",
                "# /home/user/documents/archivo3.pptx",
                "",
                "# Agregue sus rutas aquí:"
            );
            
            Files.write(Paths.get(FILE_PATHS_FILE), defaultContent);
            System.out.println("Archivo " + FILE_PATHS_FILE + " creado. Por favor edítelo con las rutas de sus archivos.");
            
        } catch (IOException e) {
            System.err.println("Error creando archivo de rutas: " + e.getMessage());
        }
    }
    
    public static String getOutputDirectory() {
        try {
            Path outputPath = Paths.get(OUTPUT_DIRECTORY);
            Files.createDirectories(outputPath);
            return outputPath.toAbsolutePath().toString();
        } catch (IOException e) {
            System.err.println("Error creando directorio de salida: " + e.getMessage());
            return System.getProperty("user.dir");
        }
    }
    
    public static String getReportsDirectory() {
        try {
            Path reportsPath = Paths.get(REPORTS_DIRECTORY);
            Files.createDirectories(reportsPath);
            return reportsPath.toAbsolutePath().toString();
        } catch (IOException e) {
            System.err.println("Error creando directorio de reportes: " + e.getMessage());
            return System.getProperty("user.dir");
        }
    }
    
    public static void saveReport(String content, String fileName) {
        try {
            String reportsDir = getReportsDirectory();
            Path reportPath = Paths.get(reportsDir, fileName);
            Files.writeString(reportPath, content);
            System.out.println("Reporte guardado: " + reportPath.toAbsolutePath());
        } catch (IOException e) {
            System.err.println("Error guardando reporte: " + e.getMessage());
        }
    }
    
    public static List<Path> getReportFiles() {
        try {
            Path reportsPath = Paths.get(getReportsDirectory());
            return Files.list(reportsPath)
                    .filter(path -> path.toString().endsWith(".txt"))
                    .sorted()
                    .toList();
        } catch (IOException e) {
            System.err.println("Error listando archivos de reportes: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    public static void cleanupTempDirectory(String tempDir) {
        try {
            if (Files.exists(Paths.get(tempDir))) {
                Files.walk(Paths.get(tempDir))
                    .sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException e) {
                            // Ignorar errores de limpieza
                        }
                    });
            }
        } catch (IOException e) {
            // Ignorar errores de limpieza
        }
    }
}