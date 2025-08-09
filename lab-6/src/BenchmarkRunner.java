import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

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
        new ConcurrentHashMap<>();
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
        
        // Warmup del sistema con 1 hilo
        performWarmup(urls);
        
        // Probar hilos de 1 a 16, incrementando de 1 en 1
        for (int threads = 1; threads <= 16; threads++) {
            System.out.printf("Ejecutando prueba con %d hilo(s)...\n", threads);
            
            createThreadOutputDirectory(threads);
            cleanOutputDirectory(threads);
            
            // Múltiples iteraciones con pausa para estabilización
            long totalTime = 0;
            int validIterations = 0;
            int maxIterations = 5;
            
            for (int i = 0; i < maxIterations; i++) {
                // Pausa antes de cada iteración para estabilizar el sistema
                if (i > 0) {
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
                
                // Limpiar directorio antes de cada iteración
                cleanOutputDirectory(threads);
                
                // Forzar garbage collection antes de la medición
                System.gc();
                Thread.yield();
                
                WebToPDFConverter.ConversionResult result = converter.convertUrls(urls, threads);
                
                // Descartar primeras iteraciones si son outliers
                if (i >= 1) {
                    totalTime += result.executionTimeMs;
                    validIterations++;
                }
                
                System.out.printf("  Iteración %d: %dms\n", i + 1, result.executionTimeMs);
            }
            
            long avgTime = validIterations > 0 ? totalTime / validIterations : 0;
            results.add(new BenchmarkResult(threads, avgTime, urls.size()));
            System.out.printf("  Tiempo promedio (%d iteraciones): %dms\n", validIterations, avgTime);
            
            // Pausa más larga entre configuraciones de hilos
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        generateReport();
    }
    
    private void performWarmup(List<String> urls) {
        System.out.println("Realizando warmup del sistema...");
        createThreadOutputDirectory(1);
        cleanOutputDirectory(1);
        
        // Warmup con subset de URLs
        List<String> warmupUrls = urls.subList(0, Math.min(3, urls.size()));
        converter.convertUrls(warmupUrls, 1);
        
        cleanOutputDirectory(1);
        System.out.println("Warmup completado.\n");
        
        // Pausa después del warmup
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
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
            writer.println("Iteraciones por prueba: 4 (descartando primera)");
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
            writer.printf("%-8s %-15s %-15s %-15s %-15s\n", "Hilos", "Tiempo (ms)", "URLs/seg", "Speedup", "Eficiencia");
            writer.println("------------------------------------------------------------------------");
            
            long baselineTime = results.get(0).avgExecutionTimeMs;
            
            for (BenchmarkResult result : results) {
                double urlsPerSecond = (result.urlCount * 1000.0) / result.avgExecutionTimeMs;
                double speedup = (double) baselineTime / result.avgExecutionTimeMs;
                double efficiency = speedup / result.threadCount;
                
                writer.printf("%-8d %-15d %-15.2f %-15.2fx %-15.2f%%\n", 
                    result.threadCount, result.avgExecutionTimeMs, urlsPerSecond, 
                    speedup, efficiency * 100);
            }
            
            writer.println();
            writer.println("=== ANÁLISIS ===");
            
            BenchmarkResult fastest = results.stream()
                .min(Comparator.comparing(r -> r.avgExecutionTimeMs))
                .orElse(results.get(0));
            
            BenchmarkResult mostEfficient = results.stream()
                .max(Comparator.comparing(r -> ((double) baselineTime / r.avgExecutionTimeMs) / r.threadCount))
                .orElse(results.get(0));
            
            writer.println("Configuración más rápida: " + fastest.threadCount + " hilo(s)");
            writer.println("Tiempo más rápido: " + fastest.avgExecutionTimeMs + "ms");
            writer.println("Mejora máxima: " + String.format("%.2fx", (double) baselineTime / fastest.avgExecutionTimeMs));
            writer.println();
            writer.println("Configuración más eficiente: " + mostEfficient.threadCount + " hilo(s)");
            double maxEfficiency = ((double) baselineTime / mostEfficient.avgExecutionTimeMs) / mostEfficient.threadCount;
            writer.println("Eficiencia máxima: " + String.format("%.2f%%", maxEfficiency * 100));
            
            // Detectar punto de saturación
            detectSaturationPoint(writer, baselineTime);
            
            generateChartData(writer);
            
        } catch (IOException e) {
            System.err.println("Error generando reporte: " + e.getMessage());
        }
        
        System.out.println("\nReporte generado: " + reportFile);
        displaySummary();
    }
    
    private void detectSaturationPoint(PrintWriter writer, long baselineTime) {
        writer.println();
        writer.println("=== ANÁLISIS DE SATURACIÓN ===");
        
        double bestEfficiency = 0;
        int saturationPoint = 1;
        
        for (BenchmarkResult result : results) {
            double efficiency = ((double) baselineTime / result.avgExecutionTimeMs) / result.threadCount;
            if (efficiency > bestEfficiency) {
                bestEfficiency = efficiency;
                saturationPoint = result.threadCount;
            }
        }
        
        writer.println("Punto de saturación estimado: " + saturationPoint + " hilo(s)");
        writer.println("Recomendación: Usar entre " + Math.max(1, saturationPoint - 1) + 
                      " y " + Math.min(16, saturationPoint + 2) + " hilos para rendimiento óptimo");
    }
    
    private void generateChartData(PrintWriter writer) {
        writer.println();
        writer.println("=== DATOS PARA GRÁFICO (CSV) ===");
        writer.println("Hilos,TiempoMs,URLsPorSegundo,Speedup,Eficiencia");
        
        long baseline = results.get(0).avgExecutionTimeMs;
        
        for (BenchmarkResult result : results) {
            double urlsPerSecond = (result.urlCount * 1000.0) / result.avgExecutionTimeMs;
            double speedup = (double) baseline / result.avgExecutionTimeMs;
            double efficiency = speedup / result.threadCount;
            
            writer.printf("%d,%d,%.2f,%.2f,%.4f\n", 
                result.threadCount, result.avgExecutionTimeMs, urlsPerSecond, speedup, efficiency);
        }
    }
    
    private void displaySummary() {
        System.out.println("\n=== RESUMEN ===");
        System.out.printf("%-8s %-15s %-15s %-15s\n", "Hilos", "Tiempo (ms)", "Speedup", "Eficiencia");
        System.out.println("--------------------------------------------------------");
        
        long baseline = results.get(0).avgExecutionTimeMs;
        
        for (BenchmarkResult result : results) {
            double speedup = (double) baseline / result.avgExecutionTimeMs;
            double efficiency = speedup / result.threadCount;
            System.out.printf("%-8d %-15d %-15.2fx %-15.2f%%\n", 
                result.threadCount, result.avgExecutionTimeMs, speedup, efficiency * 100);
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