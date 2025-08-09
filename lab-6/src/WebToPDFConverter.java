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
        List<String> successfulPdfs = Collections.synchronizedList(new ArrayList<>());
        List<String> errors = Collections.synchronizedList(new ArrayList<>());
        
        try (// Usar ThreadPoolExecutor con configuración explícita
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
            threadCount,
            threadCount,
            60L,
            TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(urls.size() * 2),
            new ThreadPoolExecutor.CallerRunsPolicy()
        )) {
            List<Future<ConversionTask.Result>> futures = new ArrayList<>();
            
            for (int i = 0; i < urls.size(); i++) {
                String url = urls.get(i);
                String filename = String.format("document_%d.pdf", i + 1);
                String outputPath = Paths.get(outputDir, filename).toString();
                
                futures.add(executor.submit(new ConversionTask(url, outputPath, chromePath)));
            }
            
            // Procesar resultados sin bloquear prematuramente
            for (int i = 0; i < futures.size(); i++) {
                try {
                    ConversionTask.Result result = futures.get(i).get();
                    if (result.success) {
                        successfulPdfs.add(result.outputPath);
                    } else {
                        errors.add(String.format("URL %d: %s", i + 1, result.error));
                    }
                } catch (ExecutionException e) {
                    errors.add(String.format("URL %d - Error ejecución: %s", i + 1, e.getCause().getMessage()));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    errors.add(String.format("URL %d - Interrumpido: %s", i + 1, e.getMessage()));
                    break;
                }
            }
            
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
        
        Duration duration = Duration.between(start, Instant.now());
        
        return new ConversionResult(successfulPdfs, errors, duration.toMillis(), threadCount);
    }
    
    public static class ConversionResult {
        public final List<String> successfulPdfs;
        public final List<String> errors;
        public final long executionTimeMs;
        public final int threadCount;
        
        public ConversionResult(List<String> pdfs, List<String> errors, long time, int threads) {
            this.successfulPdfs = new ArrayList<>(pdfs);
            this.errors = new ArrayList<>(errors);
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