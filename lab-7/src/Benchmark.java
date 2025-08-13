import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class Benchmark {
    
    public static void runPerformanceAnalysis() {
        List<String> inputFiles = FileManager.loadFilePaths();
        
        if (inputFiles.size() < 32) {
            System.out.println("Advertencia: Se necesitan al menos 32 archivos para el análisis completo.");
            System.out.println("Archivos disponibles: " + inputFiles.size());
        }
        
        // Usar solo los primeros 32 archivos si hay más
        List<String> testFiles = inputFiles.size() >= 32 
            ? inputFiles.subList(0, 32) 
            : inputFiles;
        
        System.out.println("\n=== Iniciando análisis de rendimiento ===");
        System.out.println("Archivos a procesar: " + testFiles.size());
        
        Map<Integer, Long> results = new LinkedHashMap<>();
        String baseOutputDir = FileManager.getOutputDirectory();
        
        // Obtener información del sistema
        String systemInfo = getSystemInfo();
        
        for (int threads = 1; threads <= 16; threads++) {
            System.out.println("\nPrueba con " + threads + " hilo(s)...");
            
            String testOutputDir = baseOutputDir + "/test_" + threads + "_threads_" + 
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            
            long startTime = System.currentTimeMillis();
            List<String> converted = PDFConverter.convertToPDF(testFiles, testOutputDir, threads);
            long endTime = System.currentTimeMillis();
            
            long executionTime = endTime - startTime;
            results.put(threads, executionTime);
            
            System.out.println("Tiempo: " + executionTime + " ms");
            System.out.println("Archivos convertidos: " + converted.size());
            
            // Limpiar archivos temporales
            FileManager.cleanupTempDirectory(testOutputDir);
        }
        
        // Generar y guardar reporte
        String report = generateReport(results, systemInfo, testFiles.size());
        String fileName = "benchmark_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".txt";
        FileManager.saveReport(report, fileName);
        
        printResults(results);
    }
    
    private static String generateReport(Map<Integer, Long> results, String systemInfo, int fileCount) {
        StringBuilder report = new StringBuilder();
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        
        report.append("=".repeat(80)).append("\n");
        report.append("REPORTE DE RENDIMIENTO - CONVERSIÓN PDF\n");
        report.append("=".repeat(80)).append("\n");
        report.append("Fecha y hora: ").append(timestamp).append("\n");
        report.append("Archivos procesados: ").append(fileCount).append("\n\n");
        
        report.append("INFORMACIÓN DEL SISTEMA:\n");
        report.append(systemInfo).append("\n");
        
        report.append("RESULTADOS DE RENDIMIENTO:\n");
        report.append("=".repeat(50)).append("\n");
        report.append(String.format("%-8s | %-12s | %-12s | %-10s%n", "Hilos", "Tiempo (ms)", "Tiempo (s)", "Mejora"));
        report.append("-".repeat(50)).append("\n");
        
        long baselineTime = results.get(1);
        
        for (Map.Entry<Integer, Long> entry : results.entrySet()) {
            int threads = entry.getKey();
            long timeMs = entry.getValue();
            double timeSec = timeMs / 1000.0;
            double improvement = (double) baselineTime / timeMs;
            
            report.append(String.format("%-8d | %-12d | %-12.2f | %.2fx%n", 
                threads, timeMs, timeSec, improvement));
        }
        
        // Análisis
        int optimalThreads = results.entrySet().stream()
            .min(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse(1);
        
        long minTime = results.get(optimalThreads);
        double maxImprovement = (double) baselineTime / minTime;
        
        report.append("\n").append("=".repeat(50)).append("\n");
        report.append("ANÁLISIS:\n");
        report.append("Configuración óptima: ").append(optimalThreads).append(" hilos\n");
        report.append("Tiempo mínimo: ").append(minTime).append(" ms (").append(minTime/1000.0).append(" s)\n");
        report.append("Mejora máxima: ").append(String.format("%.2fx", maxImprovement)).append("\n");
        report.append("Eficiencia por hilo: ").append(String.format("%.2f%%", (maxImprovement / optimalThreads) * 100)).append("\n");
        
        // Datos para gráficas en formato CSV
        report.append("\n").append("=".repeat(50)).append("\n");
        report.append("DATOS CSV (para gráficas):\n");
        report.append("Hilos,TiempoMs,TiempoSeg,Mejora\n");
        
        for (Map.Entry<Integer, Long> entry : results.entrySet()) {
            int threads = entry.getKey();
            long timeMs = entry.getValue();
            double timeSec = timeMs / 1000.0;
            double improvement = (double) baselineTime / timeMs;
            
            report.append(String.format("%d,%d,%.2f,%.2f%n", threads, timeMs, timeSec, improvement));
        }
        
        return report.toString();
    }
    
    private static String getSystemInfo() {
        StringBuilder info = new StringBuilder();
        Runtime runtime = Runtime.getRuntime();
        
        info.append("Procesador: ").append(System.getProperty("os.arch")).append("\n");
        info.append("CPU Cores: ").append(runtime.availableProcessors()).append("\n");
        info.append("Memoria máxima JVM: ").append(runtime.maxMemory() / (1024 * 1024)).append(" MB\n");
        info.append("Memoria total JVM: ").append(runtime.totalMemory() / (1024 * 1024)).append(" MB\n");
        info.append("Sistema operativo: ").append(System.getProperty("os.name")).append(" ")
            .append(System.getProperty("os.version")).append("\n");
        info.append("Java versión: ").append(System.getProperty("java.version")).append("\n");
        
        return info.toString();
    }
    
    private static void printResults(Map<Integer, Long> results) {
        System.out.println("\n" + "=".repeat(50));
        System.out.println("RESULTADOS DE RENDIMIENTO");
        System.out.println("=".repeat(50));
        System.out.printf("%-8s | %-12s | %-10s%n", "Hilos", "Tiempo (ms)", "Mejora");
        System.out.println("-".repeat(35));
        
        long baselineTime = results.get(1);
        
        for (Map.Entry<Integer, Long> entry : results.entrySet()) {
            int threads = entry.getKey();
            long time = entry.getValue();
            double improvement = ((double) baselineTime / time);
            
            System.out.printf("%-8d | %-12d | %.2fx%n", threads, time, improvement);
        }
        
        // Encontrar configuración óptima
        int optimalThreads = results.entrySet().stream()
            .min(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse(1);
        
        System.out.println("\n" + "=".repeat(50));
        System.out.println("ANÁLISIS:");
        System.out.println("Configuración óptima: " + optimalThreads + " hilos");
        System.out.println("Tiempo mínimo: " + results.get(optimalThreads) + " ms");
        System.out.printf("Mejora sobre 1 hilo: %.2fx%n", 
            (double) baselineTime / results.get(optimalThreads));
    }
}