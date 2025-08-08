import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class BenchmarkRunner {
    private static final String[] TEST_URLS = {
        "https://www.google.com", "https://www.github.com", "https://stackoverflow.com",
        "https://www.wikipedia.org", "https://www.oracle.com", "https://www.mozilla.org",
        "https://www.w3.org", "https://www.apache.org", "https://www.nginx.com",
        "https://www.docker.com", "https://kubernetes.io", "https://www.elastic.co",
        "https://www.mongodb.com", "https://www.postgresql.org", "https://redis.io",
        "https://nodejs.org", "https://reactjs.org", "https://angular.io",
        "https://vuejs.org", "https://getbootstrap.com", "https://tailwindcss.com",
        "https://www.jetbrains.com", "https://code.visualstudio.com", "https://www.sublimetext.com",
        "https://www.atlassian.com", "https://slack.com", "https://discord.com",
        "https://www.zoom.us", "https://www.dropbox.com", "https://www.notion.so",
        "https://trello.com", "https://asana.com"
    };
    
    private final WebToPDFConverter converter;
    private final List<BenchmarkResult> results;
    private final String reportDir;
    
    public BenchmarkRunner(String chromePath, String outputDir) {
        this.converter = new WebToPDFConverter(outputDir, chromePath);
        this.results = new ArrayList<>();
        this.reportDir = "./benchmark_reports";
        createReportDirectory();
    }
    
    private void createReportDirectory() {
        try {
            Files.createDirectories(Paths.get(reportDir));
        } catch (IOException e) {
            throw new RuntimeException("No se pudo crear directorio de reportes", e);
        }
    }
    
    public void runBenchmark() {
        System.out.println("Iniciando benchmark con 32 URLs...");
        List<String> urls = Arrays.asList(TEST_URLS);
        
        for (int threads = 1; threads <= 16; threads++) {
            System.out.printf("Ejecutando prueba con %d hilos...\n", threads);
            
            // Limpiar directorio de salida
            cleanOutputDirectory();
            
            // Ejecutar múltiples iteraciones para obtener promedio
            long totalTime = 0;
            int iterations = 3;
            
            for (int i = 0; i < iterations; i++) {
                WebToPDFConverter.ConversionResult result = converter.convertUrls(urls, threads);
                totalTime += result.executionTimeMs;
                
                // Limpiar entre iteraciones
                cleanOutputDirectory();
                
                // Pausa entre iteraciones
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            
            long avgTime = totalTime / iterations;
            results.add(new BenchmarkResult(threads, avgTime, urls.size()));
            System.out.printf("  Tiempo promedio: %dms\n", avgTime);
        }
        
        generateReport();
    }
    
    private void cleanOutputDirectory() {
        try {
            Path outputPath = Paths.get(converter.outputDir);
            if (Files.exists(outputPath)) {
                Files.walk(outputPath)
                    .filter(Files::isRegularFile)
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            System.err.println("Error eliminando: " + path);
                        }
                    });
            }
        } catch (IOException e) {
            System.err.println("Error limpiando directorio: " + e.getMessage());
        }
    }
    
    private void generateReport() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String reportFile = reportDir + "/benchmark_report_" + timestamp + ".txt";
        
        try (PrintWriter writer = new PrintWriter(new FileWriter(reportFile))) {
            writer.println("=== INFORME DE BENCHMARK - WEB TO PDF CONVERTER ===");
            writer.println("Fecha: " + LocalDateTime.now());
            writer.println("URLs procesadas: " + TEST_URLS.length);
            writer.println("Iteraciones por prueba: 3 (promedio)");
            writer.println();
            
            // Información del sistema
            writer.println("=== INFORMACIÓN DEL SISTEMA ===");
            writer.println("Java Version: " + System.getProperty("java.version"));
            writer.println("OS: " + System.getProperty("os.name") + " " + System.getProperty("os.version"));
            writer.println("Procesadores disponibles: " + Runtime.getRuntime().availableProcessors());
            writer.println("Memoria máxima JVM: " + (Runtime.getRuntime().maxMemory() / 1024 / 1024) + " MB");
            writer.println();
            
            // Tabla de resultados
            writer.println("=== RESULTADOS ===");
            writer.printf("%-8s %-15s %-15s %-15s\n", "Hilos", "Tiempo (ms)", "URLs/seg", "Speedup");
            writer.println("--------------------------------------------------------");
            
            long baselineTime = results.get(0).avgExecutionTimeMs;
            
            for (BenchmarkResult result : results) {
                double urlsPerSecond = (result.urlCount * 1000.0) / result.avgExecutionTimeMs;
                double speedup = (double) baselineTime / result.avgExecutionTimeMs;
                
                writer.printf("%-8d %-15d %-15.2f %-15.2fx\n", 
                    result.threadCount, result.avgExecutionTimeMs, urlsPerSecond, speedup);
            }
            
            writer.println();
            writer.println("=== ANÁLISIS ===");
            
            BenchmarkResult fastest = results.stream()
                .min(Comparator.comparing(r -> r.avgExecutionTimeMs))
                .orElse(results.get(0));
            
            writer.println("Configuración más rápida: " + fastest.threadCount + " hilos");
            writer.println("Tiempo más rápido: " + fastest.avgExecutionTimeMs + "ms");
            writer.println("Mejora máxima: " + String.format("%.2fx", (double) baselineTime / fastest.avgExecutionTimeMs));
            
            // Generar datos para gráfico
            generateChartData(writer);
            
        } catch (IOException e) {
            System.err.println("Error generando reporte: " + e.getMessage());
        }
        
        System.out.println("\nReporte generado: " + reportFile);
        displaySummary();
    }
    
    private void generateChartData(PrintWriter writer) {
        writer.println();
        writer.println("=== DATOS PARA GRÁFICO (CSV) ===");
        writer.println("Hilos,TiempoMs,URLsPorSegundo");
        
        for (BenchmarkResult result : results) {
            double urlsPerSecond = (result.urlCount * 1000.0) / result.avgExecutionTimeMs;
            writer.printf("%d,%d,%.2f\n", result.threadCount, result.avgExecutionTimeMs, urlsPerSecond);
        }
    }
    
    private void displaySummary() {
        System.out.println("\n=== RESUMEN ===");
        System.out.printf("%-8s %-15s %-15s\n", "Hilos", "Tiempo (ms)", "Speedup");
        System.out.println("----------------------------------------");
        
        long baseline = results.get(0).avgExecutionTimeMs;
        
        for (BenchmarkResult result : results) {
            double speedup = (double) baseline / result.avgExecutionTimeMs;
            System.out.printf("%-8d %-15d %-15.2fx\n", 
                result.threadCount, result.avgExecutionTimeMs, speedup);
        }
    }
    
    private static class BenchmarkResult {
        final int threadCount;
        final long avgExecutionTimeMs;
        final int urlCount;
        
        BenchmarkResult(int threads, long time, int urls) {
            this.threadCount = threads;
            this.avgExecutionTimeMs = time;
            this.urlCount = urls;
        }
    }
    
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        
        System.out.print("Ruta de Chrome [/usr/bin/google-chrome]: ");
        String chromePath = scanner.nextLine().trim();
        if (chromePath.isEmpty()) {
            chromePath = "/usr/bin/google-chrome";
        }
        
        System.out.print("Directorio de salida [./benchmark_output]: ");
        String outputDir = scanner.nextLine().trim();
        if (outputDir.isEmpty()) {
            outputDir = "./benchmark_output";
        }
        
        BenchmarkRunner runner = new BenchmarkRunner(chromePath, outputDir);
        runner.runBenchmark();
        
        scanner.close();
    }
}