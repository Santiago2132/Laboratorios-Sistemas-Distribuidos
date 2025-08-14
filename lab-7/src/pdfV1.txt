import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

public class PDFConverter {
    private static final String LIBREOFFICE_CMD = "libreoffice";
    private static final Set<String> SUPPORTED_FORMATS = Set.of("docx", "pptx", "xlsx", "png", "odt", "odp", "ods");
    
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        
        System.out.println("=== PDF Converter ===");
        System.out.println("1. Conversión individual");
        System.out.println("2. Análisis de rendimiento");
        System.out.println("3. Generar gráficas");
        System.out.print("Seleccione opción: ");
        
        int option = scanner.nextInt();
        scanner.nextLine();
        
        switch (option) {
            case 1:
                runSingleConversion(scanner);
                break;
            case 2:
                Benchmark.runPerformanceAnalysis();
                break;
            case 3:
                Graficas.generateReports();
                break;
            default:
                System.out.println("Opción inválida");
        }
        
        scanner.close();
    }
    
    private static void runSingleConversion(Scanner scanner) {
        System.out.print("Usar archivo de rutas? (y/n): ");
        String useFile = scanner.nextLine().trim().toLowerCase();
        
        List<String> inputFiles;
        
        if ("y".equals(useFile)) {
            inputFiles = FileManager.loadFilePaths();
        } else {
            System.out.print("Ingrese rutas de archivos (separadas por coma): ");
            String input = scanner.nextLine();
            String[] paths = input.split(",");
            inputFiles = Arrays.stream(paths)
                    .map(String::trim)
                    .filter(path -> !path.isEmpty())
                    .toList();
        }
        
        System.out.print("Número de hilos (Enter para 1): ");
        String threadsInput = scanner.nextLine().trim();
        int threads = threadsInput.isEmpty() ? 1 : Integer.parseInt(threadsInput);
        
        String outputDir = FileManager.getOutputDirectory();
        
        long startTime = System.currentTimeMillis();
        List<String> convertedFiles = convertToPDF(inputFiles, outputDir, threads);
        long endTime = System.currentTimeMillis();
        
        System.out.println("\n=== Resultados ===");
        System.out.println("Tiempo total: " + (endTime - startTime) + " ms");
        System.out.println("Archivos convertidos:");
        convertedFiles.forEach(System.out::println);
    }
    
    public static List<String> convertToPDF(List<String> inputFiles, String outputDir, int threadCount) {
        List<String> convertedFiles = Collections.synchronizedList(new ArrayList<>());
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        
        try {
            Files.createDirectories(Paths.get(outputDir));
            
            List<Future<String>> futures = new ArrayList<>();
            
            for (String inputFile : inputFiles) {
                Future<String> future = executor.submit(() -> {
                    try {
                        return convertSingleFile(inputFile, outputDir);
                    } catch (Exception e) {
                        System.err.println("Error convirtiendo " + inputFile + ": " + e.getMessage());
                        return null;
                    }
                });
                futures.add(future);
            }
            
            for (Future<String> future : futures) {
                try {
                    String result = future.get();
                    if (result != null) {
                        convertedFiles.add(result);
                    }
                } catch (Exception e) {
                    System.err.println("Error obteniendo resultado: " + e.getMessage());
                }
            }
            
        } catch (IOException e) {
            System.err.println("Error creando directorio de salida: " + e.getMessage());
        } finally {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        return convertedFiles;
    }
    
    private static String convertSingleFile(String inputFile, String outputDir) throws Exception {
        Path inputPath = Paths.get(inputFile);
        
        if (!Files.exists(inputPath)) {
            throw new FileNotFoundException("Archivo no encontrado: " + inputFile);
        }
        
        String extension = getFileExtension(inputPath.getFileName().toString()).toLowerCase();
        if (!SUPPORTED_FORMATS.contains(extension)) {
            throw new UnsupportedOperationException("Formato no soportado: " + extension);
        }
        
        String outputFileName = inputPath.getFileName().toString();
        int lastDot = outputFileName.lastIndexOf('.');
        if (lastDot > 0) {
            outputFileName = outputFileName.substring(0, lastDot);
        }
        outputFileName += ".pdf";
        
        Path outputPath = Paths.get(outputDir, outputFileName);
        
        ProcessBuilder pb = new ProcessBuilder(
            LIBREOFFICE_CMD,
            "--headless",
            "--convert-to", "pdf",
            "--outdir", outputDir,
            inputPath.toAbsolutePath().toString()
        );
        
        pb.redirectErrorStream(true);
        Process process = pb.start();
        
        boolean finished = process.waitFor(60, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            throw new RuntimeException("Timeout en conversión de: " + inputFile);
        }
        
        int exitCode = process.exitValue();
        if (exitCode != 0) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String output = reader.lines().reduce("", (a, b) -> a + "\n" + b);
                throw new RuntimeException("Error en LibreOffice (código " + exitCode + "): " + output);
            }
        }
        
        if (!Files.exists(outputPath)) {
            throw new RuntimeException("Archivo PDF no fue creado: " + outputPath);
        }
        
        return outputPath.toAbsolutePath().toString();
    }
    
    private static String getFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        return lastDot > 0 ? fileName.substring(lastDot + 1) : "";
    }
}