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
    private final String baseOutputDir;
    
    public BenchmarkRunner(String chromePath, String outputDir) {
        this.baseOutputDir = outputDir;
        this.converter = new WebToPDFConverter(outputDir, chromePath);
        this.results = new ArrayList<>();
        this.reportDir = "/home/santiago/Repositorios/Laboratorios-Sistemas-Distribuidos/lab-6/benchmark_reports";
        createReportDirectory();
    }
    
    private void createReportDirectory() {
        try {
            Files.createDirectories(Paths.get(reportDir));
        } catch (IOException e) {
            throw new RuntimeException("No se pudo crear directorio de reportes", e);
        }
    }
    
    private void createThreadOutputDirectory(int threads) {
        try {
            String threadDir = baseOutputDir + "/" + threads + "hilo";
            Files.createDirectories(Paths.get(threadDir));
            // Actualizar el directorio de salida del converter
            converter.outputDir = threadDir;
        } catch (IOException e) {
            throw new RuntimeException("No se pudo crear directorio para " + threads + " hilos", e);
        }
    }
    
    public void runBenchmark(int urlCount) {
        if (urlCount <= 0 || urlCount > TEST_URLS.length) {
            throw new IllegalArgumentException("Número de URLs debe ser entre 1 y " + TEST_URLS.length);
        }
        
        System.out.println("Iniciando benchmark con " + urlCount + " URLs...");
        List<String> urls = Arrays.asList(TEST_URLS).subList(0, urlCount);
        
        // Probar hilos de 1 a 16, incrementando de 1 en 1
        for (int threads = 1; threads <= 16; threads++) {
            System.out.printf("Ejecutando prueba con %d hilo(s)...\n", threads);
            
            // Crear directorio específico para esta configuración de hilos
            createThreadOutputDirectory(threads);
            
            // Limpiar directorio de salida antes de cada prueba
            cleanOutputDirectory(threads);
            
            // Ejecutar múltiples iteraciones para obtener promedio
            long totalTime = 0;
            int iterations = 3;
            
            for (int i = 0; i < iterations; i++) {
                WebToPDFConverter.ConversionResult result = converter.convertUrls(urls, threads);
                totalTime += result.executionTimeMs;
                
                // Limpiar entre iteraciones (excepto la última)
                if (i < iterations - 1) {
                    cleanOutputDirectory(threads);
                }
                
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
    
    private void cleanOutputDirectory(int threads) {
        try {
            Path outputPath = Paths.get(baseOutputDir + "/" + threads + "hilo");
            if (Files.exists(outputPath)) {
                Files.walk(outputPath)
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".pdf"))
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
        String reportFileName = "benchmark_report_" + timestamp + ".txt";
        String reportFile = reportDir + "/" + reportFileName;
        
        // Asegurar que el nombre sea único
        int counter = 1;
        while (Files.exists(Paths.get(reportFile))) {
            reportFileName = "benchmark_report_" + timestamp + "_" + counter + ".txt";
            reportFile = reportDir + "/" + reportFileName;
            counter++;
        }
        
        try (PrintWriter writer = new PrintWriter(new FileWriter(reportFile))) {
            writer.println("=== INFORME DE BENCHMARK - WEB TO PDF CONVERTER ===");
            writer.println("Fecha: " + LocalDateTime.now());
            writer.println("URLs procesadas: " + results.get(0).urlCount);
            writer.println("Iteraciones por prueba: 3 (promedio)");
            writer.println("Hilos probados: 1 a 16");
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
            
            writer.println("Configuración más rápida: " + fastest.threadCount + " hilo(s)");
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
    
    private static String findChromePath() {
        String[] possiblePaths = {
            "/usr/bin/google-chrome",
            "/usr/bin/google-chrome-stable", 
            "/usr/bin/chromium",
            "/usr/bin/chromium-browser",
            "/opt/google/chrome/chrome",
            "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome",
            "C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe",
            "C:\\Program Files (x86)\\Google\\Chrome\\Application\\chrome.exe"
        };
        
        for (String path : possiblePaths) {
            if (Files.exists(Paths.get(path))) {
                System.out.println("Chrome encontrado en: " + path);
                return path;
            }
        }
        
        System.err.println("Chrome no encontrado en ubicaciones comunes");
        System.exit(1);
        return null;
    }
    
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        
        String chromePath = findChromePath();
        
        System.out.print("Directorio de salida [/home/santiago/Repositorios/Laboratorios-Sistemas-Distribuidos/lab-6/benchmark_output]: ");
        String outputDir = scanner.nextLine().trim();
        if (outputDir.isEmpty()) {
            outputDir = "/home/santiago/Repositorios/Laboratorios-Sistemas-Distribuidos/lab-6/benchmark_output";
        }
        
        System.out.print("¿Con cuántos links hacer el benchmark? (1-" + TEST_URLS.length + ") [" + TEST_URLS.length + "]: ");
        String urlCountInput = scanner.nextLine().trim();
        int urlCount = TEST_URLS.length;
        
        if (!urlCountInput.isEmpty()) {
            try {
                urlCount = Integer.parseInt(urlCountInput);
                if (urlCount <= 0 || urlCount > TEST_URLS.length) {
                    System.err.println("Número inválido, usando " + TEST_URLS.length + " URLs");
                    urlCount = TEST_URLS.length;
                }
            } catch (NumberFormatException e) {
                System.err.println("Número inválido, usando " + TEST_URLS.length + " URLs");
                urlCount = TEST_URLS.length;
            }
        }
        
        BenchmarkRunner runner = new BenchmarkRunner(chromePath, outputDir);
        runner.runBenchmark(urlCount);
        
        scanner.close();
    }
}