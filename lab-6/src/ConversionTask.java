import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

public class ConversionTask implements Callable<ConversionTask.Result> {
    private final String url;
    private final String outputPath;
    private final String chromePath;
    
    public ConversionTask(String url, String outputPath, String chromePath) {
        this.url = url;
        this.outputPath = outputPath;
        this.chromePath = chromePath;
    }
    
    @Override
    public Result call() {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                chromePath,
                "--headless",
                "--disable-gpu",
                "--no-sandbox",
                "--disable-dev-shm-usage",
                "--print-to-pdf=" + outputPath,
                url
            );
            
            pb.redirectErrorStream(true);
            Process process = pb.start();
            
            boolean finished = process.waitFor(30, TimeUnit.SECONDS);
            
            if (!finished) {
                process.destroyForcibly();
                return new Result(false, outputPath, "Timeout: conversión excedió 30 segundos");
            }
            
            if (process.exitValue() != 0) {
                return new Result(false, outputPath, "Chrome falló con código: " + process.exitValue());
            }
            
            return new Result(true, outputPath, null);
            
        } catch (IOException e) {
            return new Result(false, outputPath, "Error IO: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new Result(false, outputPath, "Interrumpido: " + e.getMessage());
        }
    }
    
    public static class Result {
        public final boolean success;
        public final String outputPath;
        public final String error;
        
        public Result(boolean success, String outputPath, String error) {
            this.success = success;
            this.outputPath = outputPath;
            this.error = error;
        }
    }
}