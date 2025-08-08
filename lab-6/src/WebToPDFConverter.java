
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.time.Duration;
import java.time.Instant;

public class WebToPDFConverter {
    String outputDir;
    private final String chromePath;
    
    public WebToPDFConverter(String outputDir, String chromePath) {
        this.outputDir = outputDir;
        this.chromePath = chromePath;
        createOutputDirectory();
    }
    
    private void createOutputDirectory() {
        try {
            Files.createDirectories(Paths.get(outputDir));
        } catch (IOException e) {
            throw new RuntimeException("No se pudo crear directorio: " + outputDir, e);
        }
    }
    
    public ConversionResult convertUrls(List<String> urls, int threadCount) {
        Instant start = Instant.now();
        List<String> results = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<Future<ConversionTask.Result>> futures = new ArrayList<>();
        
        for (int i = 0; i < urls.size(); i++) {
            String url = urls.get(i);
            String filename = String.format("document_%d.pdf", i + 1);
            String outputPath = Paths.get(outputDir, filename).toString();
            
            futures.add(executor.submit(new ConversionTask(url, outputPath, chromePath)));
        }
        
        for (Future<ConversionTask.Result> future : futures) {
            try {
                ConversionTask.Result result = future.get();
                if (result.success) {
                    results.add(result.outputPath);
                } else {
                    errors.add(result.error);
                }
            } catch (Exception e) {
                errors.add("Error ejecutando tarea: " + e.getMessage());
            }
        }
        
        executor.shutdown();
        Duration duration = Duration.between(start, Instant.now());
        
        return new ConversionResult(results, errors, duration.toMillis(), threadCount);
    }
    
    public static class ConversionResult {
        public final List<String> successfulPdfs;
        public final List<String> errors;
        public final long executionTimeMs;
        public final int threadCount;
        
        public ConversionResult(List<String> pdfs, List<String> errors, long time, int threads) {
            this.successfulPdfs = pdfs;
            this.errors = errors;
            this.executionTimeMs = time;
            this.threadCount = threads;
        }
        
        @Override
        public String toString() {
            return String.format("Hilos: %d, Tiempo: %dms, Éxitos: %d, Errores: %d", 
                threadCount, executionTimeMs, successfulPdfs.size(), errors.size());
        }
    }
}