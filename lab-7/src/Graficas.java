import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Graficas {
    
    private static final String GRAFICAS_DIR = "graficas";
    private static final String REPORTS_DIR = "/home/santiago/Repositorios/Laboratorios-Sistemas-Distribuidos/lab-7/reports";
    
    public static void generateReports() {
        try {
            // Crear directorio de gráficas
            Files.createDirectories(Paths.get(GRAFICAS_DIR));
            
            // Cargar todos los benchmarks
            List<BenchmarkData> benchmarks = loadAllBenchmarks();
            
            if (benchmarks.isEmpty()) {
                System.out.println("No se encontraron reportes de benchmark.");
                return;
            }
            
            // Generar gráficas individuales para cada benchmark
            for (BenchmarkData benchmark : benchmarks) {
                generarGraficasIndividuales(benchmark);
            }
            
            // Generar gráficas comparativas
            generarGraficasComparativas(benchmarks);
            
            System.out.println("\nTodas las gráficas generadas en: " + Paths.get(GRAFICAS_DIR).toAbsolutePath());
            
        } catch (Exception e) {
            System.err.println("Error generando gráficas: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static List<BenchmarkData> loadAllBenchmarks() {
        List<BenchmarkData> benchmarks = new ArrayList<>();
        List<Path> reportFiles = getReportFiles();
        
        for (Path reportFile : reportFiles) {
            try {
                BenchmarkData benchmark = parseBenchmarkFile(reportFile);
                if (benchmark != null) {
                    benchmarks.add(benchmark);
                }
            } catch (Exception e) {
                System.err.println("Error parseando " + reportFile + ": " + e.getMessage());
            }
        }
        
        return benchmarks;
    }
    
    private static List<Path> getReportFiles() {
        try {
            Path reportsPath = Paths.get(REPORTS_DIR);
            if (!Files.exists(reportsPath)) {
                System.out.println("Directorio de reportes no encontrado: " + REPORTS_DIR);
                return new ArrayList<>();
            }
            return Files.list(reportsPath)
                    .filter(path -> path.toString().endsWith(".txt"))
                    .sorted()
                    .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        } catch (IOException e) {
            System.err.println("Error listando archivos de reportes: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    private static BenchmarkData parseBenchmarkFile(Path filePath) throws IOException {
        List<String> lines = Files.readAllLines(filePath);
        BenchmarkData data = new BenchmarkData();
        data.fileName = filePath.getFileName().toString();
        
        boolean inCsvData = false;
        Pattern csvPattern = Pattern.compile("(\\d+),(\\d+),([\\d,]+),([\\d,]+)");
        
        for (String line : lines) {
            if (line.contains("Archivos procesados:")) {
                String[] parts = line.split(":");
                if (parts.length > 1) {
                    data.fileCount = Integer.parseInt(parts[1].trim());
                }
            } else if (line.contains("Fecha y hora:")) {
                data.timestamp = line.substring(line.indexOf(":") + 1).trim();
            } else if (line.equals("Hilos,TiempoMs,TiempoSeg,Mejora")) {
                inCsvData = true;
                continue;
            } else if (inCsvData && !line.trim().isEmpty()) {
                Matcher matcher = csvPattern.matcher(line);
                if (matcher.matches()) {
                    int threads = Integer.parseInt(matcher.group(1));
                    long timeMs = Long.parseLong(matcher.group(2));
                    double timeSec = Double.parseDouble(matcher.group(3).replace(",", "."));
                    double speedup = Double.parseDouble(matcher.group(4).replace(",", "."));
                    
                    data.threads.add(threads);
                    data.times.add(timeMs);
                    data.speedups.add(speedup);
                    data.efficiency.add((speedup / threads) * 100);
                    data.throughput.add((double) data.fileCount / timeSec);
                }
            }
        }
        
        return data.threads.isEmpty() ? null : data;
    }
    
    private static void generarGraficasIndividuales(BenchmarkData data) throws Exception {
        String prefix = data.fileName.replace(".txt", "");
        
        generarGraficaTiempo(data, prefix + "_tiempo.png");
        generarGraficaSpeedup(data, prefix + "_speedup.png");
        generarGraficaThroughput(data, prefix + "_throughput.png");
        generarGraficaEficiencia(data, prefix + "_eficiencia.png");
    }
    
    private static void generarGraficaTiempo(BenchmarkData data, String fileName) throws Exception {
        int width = 800, height = 600;
        BufferedImage img = createBaseImage(width, height);
        Graphics2D g2d = img.createGraphics();
        
        int margin = 80;
        int plotWidth = width - 2 * margin;
        int plotHeight = height - 2 * margin;
        
        // Título
        drawTitle(g2d, width, "Tiempo de Ejecución vs. Número de Hilos (" + data.fileCount + " archivos)");
        
        // Ejes
        drawAxes(g2d, margin, width, height, plotWidth, plotHeight);
        drawAxisLabels(g2d, width, height, margin, "Número de Hilos", "Tiempo (ms)");
        
        // Datos
        long maxTime = Collections.max(data.times);
        long minTime = Collections.min(data.times);
        
        // Línea
        g2d.setColor(Color.BLUE);
        g2d.setStroke(new BasicStroke(3));
        drawLineChart(g2d, data.threads, data.times, margin, plotWidth, plotHeight, height, 16, minTime, maxTime);
        
        // Puntos
        g2d.setColor(Color.RED);
        drawPoints(g2d, data.threads, data.times, margin, plotWidth, plotHeight, height, 16, minTime, maxTime);
        
        // Etiquetas
        drawXLabels(g2d, margin, plotWidth, height, 16);
        drawYLabels(g2d, margin, plotHeight, height, minTime, maxTime, true);
        
        g2d.dispose();
        saveImage(img, fileName);
    }
    
    private static void generarGraficaSpeedup(BenchmarkData data, String fileName) throws Exception {
        int width = 800, height = 600;
        BufferedImage img = createBaseImage(width, height);
        Graphics2D g2d = img.createGraphics();
        
        int margin = 80;
        int plotWidth = width - 2 * margin;
        int plotHeight = height - 2 * margin;
        
        drawTitle(g2d, width, "Speedup Real vs. Speedup Ideal");
        drawAxes(g2d, margin, width, height, plotWidth, plotHeight);
        drawAxisLabels(g2d, width, height, margin, "Número de Hilos", "Speedup");
        
        // Línea ideal (diagonal)
        g2d.setColor(Color.GRAY);
        g2d.setStroke(new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{5}, 0));
        g2d.drawLine(margin, height - margin, margin + plotWidth, margin);
        
        // Speedup real
        g2d.setColor(Color.BLUE);
        g2d.setStroke(new BasicStroke(3));
        
        double maxSpeedup = 16.0;
        List<Long> speedupLongs = data.speedups.stream().map(d -> Math.round(d * 1000)).collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        drawLineChart(g2d, data.threads, speedupLongs, margin, plotWidth, plotHeight, height, 16, 0, Math.round(maxSpeedup * 1000));
        
        g2d.setColor(Color.RED);
        drawPoints(g2d, data.threads, speedupLongs, margin, plotWidth, plotHeight, height, 16, 0, Math.round(maxSpeedup * 1000));
        
        drawXLabels(g2d, margin, plotWidth, height, 16);
        
        // Etiquetas Y para speedup
        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("Arial", Font.PLAIN, 10));
        for (int i = 0; i <= 16; i += 4) {
            int y = height - margin - i * plotHeight / 16;
            g2d.drawString(String.valueOf(i), margin - 20, y + 5);
        }
        
        drawLegend(g2d, width);
        
        g2d.dispose();
        saveImage(img, fileName);
    }
    
    private static void generarGraficaThroughput(BenchmarkData data, String fileName) throws Exception {
        int width = 800, height = 600;
        BufferedImage img = createBaseImage(width, height);
        Graphics2D g2d = img.createGraphics();
        
        int margin = 80;
        int plotWidth = width - 2 * margin;
        int plotHeight = height - 2 * margin;
        
        drawTitle(g2d, width, "Throughput del Sistema (archivos/segundo)");
        drawAxes(g2d, margin, width, height, plotWidth, plotHeight);
        drawAxisLabels(g2d, width, height, margin, "Número de Hilos", "Archivos/segundo");
        
        // Barras
        g2d.setColor(new Color(0, 150, 0));
        int barWidth = plotWidth / data.threads.size() - 5;
        double maxThroughput = Collections.max(data.throughput);
        
        for (int i = 0; i < data.threads.size(); i++) {
            int x = margin + i * (plotWidth / data.threads.size()) + 2;
            int barHeight = (int)(data.throughput.get(i) * plotHeight / maxThroughput);
            int y = height - margin - barHeight;
            
            g2d.fillRect(x, y, barWidth, barHeight);
            
            // Valor encima de barra
            g2d.setColor(Color.BLACK);
            g2d.setFont(new Font("Arial", Font.PLAIN, 8));
            String valor = String.format("%.2f", data.throughput.get(i));
            int textWidth = g2d.getFontMetrics().stringWidth(valor);
            g2d.drawString(valor, x + barWidth/2 - textWidth/2, y - 5);
            g2d.setColor(new Color(0, 150, 0));
        }
        
        // Etiquetas X
        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("Arial", Font.PLAIN, 10));
        for (int i = 0; i < data.threads.size(); i++) {
            int x = margin + i * (plotWidth / data.threads.size()) + (plotWidth / data.threads.size()) / 2;
            g2d.drawString(String.valueOf(data.threads.get(i)), x - 5, height - margin + 15);
        }
        
        g2d.dispose();
        saveImage(img, fileName);
    }
    
    private static void generarGraficaEficiencia(BenchmarkData data, String fileName) throws Exception {
        int width = 800, height = 600;
        BufferedImage img = createBaseImage(width, height);
        Graphics2D g2d = img.createGraphics();
        
        int margin = 80;
        int plotWidth = width - 2 * margin;
        int plotHeight = height - 2 * margin;
        
        drawTitle(g2d, width, "Eficiencia vs. Número de Hilos");
        drawAxes(g2d, margin, width, height, plotWidth, plotHeight);
        drawAxisLabels(g2d, width, height, margin, "Número de Hilos", "Eficiencia (%)");
        
        // Línea de eficiencia
        g2d.setColor(Color.RED);
        g2d.setStroke(new BasicStroke(3));
        
        List<Long> efficiencyLongs = data.efficiency.stream().map(d -> Math.round(d * 100)).collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        drawLineChart(g2d, data.threads, efficiencyLongs, margin, plotWidth, plotHeight, height, 16, 0, 10000);
        
        // Puntos
        drawPoints(g2d, data.threads, efficiencyLongs, margin, plotWidth, plotHeight, height, 16, 0, 10000);
        
        drawXLabels(g2d, margin, plotWidth, height, 16);
        
        // Etiquetas Y para porcentajes
        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("Arial", Font.PLAIN, 10));
        for (int i = 0; i <= 100; i += 20) {
            int y = height - margin - i * plotHeight / 100;
            g2d.drawString(i + "%", margin - 30, y + 5);
        }
        
        g2d.dispose();
        saveImage(img, fileName);
    }
    
    private static void generarGraficasComparativas(List<BenchmarkData> benchmarks) throws Exception {
        generarComparativaSpeedup(benchmarks);
        generarComparativaRegresion(benchmarks);
    }
    
    private static void generarComparativaSpeedup(List<BenchmarkData> benchmarks) throws Exception {
        int width = 1000, height = 600;
        BufferedImage img = createBaseImage(width, height);
        Graphics2D g2d = img.createGraphics();
        
        int margin = 80;
        int plotWidth = width - 2 * margin;
        int plotHeight = height - 2 * margin;
        
        drawTitle(g2d, width, "Comparativa: Speedup Máximo por Cantidad de Archivos");
        drawAxes(g2d, margin, width, height, plotWidth, plotHeight);
        drawAxisLabels(g2d, width, height, margin, "Configuración de Prueba", "Speedup Máximo");
        
        Color[] colores = {Color.BLUE, Color.GREEN, Color.RED, Color.ORANGE, Color.MAGENTA, Color.CYAN};
        int barWidth = Math.min(plotWidth / (benchmarks.size() * 2), 80);
        
        for (int i = 0; i < benchmarks.size(); i++) {
            BenchmarkData benchmark = benchmarks.get(i);
            double maxSpeedup = Collections.max(benchmark.speedups);
            int optimalThreads = benchmark.threads.get(benchmark.speedups.indexOf(maxSpeedup));
            
            g2d.setColor(colores[i % colores.length]);
            int x = margin + 50 + i * (plotWidth / benchmarks.size());
            int barHeight = (int)(maxSpeedup * plotHeight / 4);
            int y = height - margin - barHeight;
            
            g2d.fillRect(x, y, barWidth, barHeight);
            
            // Etiquetas
            g2d.setColor(Color.BLACK);
            g2d.setFont(new Font("Arial", Font.BOLD, 10));
            g2d.drawString(benchmark.fileCount + " arch.", x - 10, height - margin + 20);
            g2d.drawString(String.format("%.2fx", maxSpeedup), x - 5, y - 10);
            g2d.drawString("(" + optimalThreads + " h)", x - 5, y - 25);
        }
        
        // Escala Y
        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("Arial", Font.PLAIN, 10));
        for (int i = 0; i <= 4; i++) {
            int y = height - margin - i * plotHeight / 4;
            g2d.drawString(String.valueOf(i), margin - 20, y + 5);
        }
        
        g2d.dispose();
        saveImage(img, "comparativa_speedup.png");
    }
    
    private static void generarComparativaRegresion(List<BenchmarkData> benchmarks) throws Exception {
        int width = 1000, height = 600;
        BufferedImage img = createBaseImage(width, height);
        Graphics2D g2d = img.createGraphics();
        
        int margin = 80;
        int plotWidth = width - 2 * margin;
        int plotHeight = height - 2 * margin;
        
        drawTitle(g2d, width, "Regresión: Speedup vs. Número de Hilos (Todos los Benchmarks)");
        drawAxes(g2d, margin, width, height, plotWidth, plotHeight);
        drawAxisLabels(g2d, width, height, margin, "Número de Hilos", "Speedup");
        
        Color[] colores = {Color.BLUE, Color.GREEN, Color.RED, Color.ORANGE, Color.MAGENTA, Color.CYAN};
        
        for (int i = 0; i < benchmarks.size(); i++) {
            BenchmarkData benchmark = benchmarks.get(i);
            g2d.setColor(colores[i % colores.length]);
            g2d.setStroke(new BasicStroke(2));
            
            // Dibujar línea de speedup
            for (int j = 0; j < benchmark.threads.size() - 1; j++) {
                int x1 = margin + (benchmark.threads.get(j) - 1) * plotWidth / 15;
                int y1 = height - margin - (int)(benchmark.speedups.get(j) * plotHeight / 4);
                int x2 = margin + (benchmark.threads.get(j+1) - 1) * plotWidth / 15;
                int y2 = height - margin - (int)(benchmark.speedups.get(j+1) * plotHeight / 4);
                
                g2d.drawLine(x1, y1, x2, y2);
            }
            
            // Dibujar puntos
            for (int j = 0; j < benchmark.threads.size(); j++) {
                int x = margin + (benchmark.threads.get(j) - 1) * plotWidth / 15;
                int y = height - margin - (int)(benchmark.speedups.get(j) * plotHeight / 4);
                g2d.fillOval(x - 3, y - 3, 6, 6);
            }
        }
        
        drawXLabels(g2d, margin, plotWidth, height, 16);
        
        // Etiquetas Y
        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("Arial", Font.PLAIN, 10));
        for (int i = 0; i <= 4; i++) {
            int y = height - margin - i * plotHeight / 4;
            g2d.drawString(String.valueOf(i), margin - 20, y + 5);
        }
        
        // Leyenda
        g2d.setFont(new Font("Arial", Font.PLAIN, 10));
        for (int i = 0; i < benchmarks.size(); i++) {
            g2d.setColor(colores[i % colores.length]);
            g2d.fillRect(width - 150, 60 + i * 20, 15, 10);
            g2d.setColor(Color.BLACK);
            g2d.drawString(benchmarks.get(i).fileCount + " archivos", width - 130, 70 + i * 20);
        }
        
        g2d.dispose();
        saveImage(img, "regresion_comparativa.png");
    }
    
    // Métodos auxiliares
    private static BufferedImage createBaseImage(int width, int height) {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = img.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, width, height);
        g2d.dispose();
        return img;
    }
    
    private static void drawTitle(Graphics2D g2d, int width, String title) {
        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("Arial", Font.BOLD, 16));
        FontMetrics fm = g2d.getFontMetrics();
        g2d.drawString(title, (width - fm.stringWidth(title)) / 2, 30);
    }
    
    private static void drawAxes(Graphics2D g2d, int margin, int width, int height, int plotWidth, int plotHeight) {
        g2d.setColor(Color.BLACK);
        g2d.setStroke(new BasicStroke(2));
        g2d.drawLine(margin, height - margin, width - margin, height - margin);
        g2d.drawLine(margin, margin, margin, height - margin);
    }
    
    private static void drawAxisLabels(Graphics2D g2d, int width, int height, int margin, String xLabel, String yLabel) {
        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("Arial", Font.PLAIN, 12));
        g2d.drawString(xLabel, width/2 - 50, height - 20);
        
        Graphics2D g2dRotated = (Graphics2D) g2d.create();
        g2dRotated.rotate(-Math.PI/2, 20, height/2);
        g2dRotated.drawString(yLabel, 20, height/2);
        g2dRotated.dispose();
    }
    
    private static void drawLineChart(Graphics2D g2d, List<Integer> xData, List<Long> yData, 
                                    int margin, int plotWidth, int plotHeight, int height,
                                    int maxX, long minY, long maxY) {
        for (int i = 0; i < xData.size() - 1; i++) {
            int x1 = margin + (xData.get(i) - 1) * plotWidth / (maxX - 1);
            int y1 = height - margin - (int)((yData.get(i) - minY) * plotHeight / (maxY - minY));
            int x2 = margin + (xData.get(i+1) - 1) * plotWidth / (maxX - 1);
            int y2 = height - margin - (int)((yData.get(i+1) - minY) * plotHeight / (maxY - minY));
            
            g2d.drawLine(x1, y1, x2, y2);
        }
    }
    
    private static void drawPoints(Graphics2D g2d, List<Integer> xData, List<Long> yData,
                                 int margin, int plotWidth, int plotHeight, int height,
                                 int maxX, long minY, long maxY) {
        for (int i = 0; i < xData.size(); i++) {
            int x = margin + (xData.get(i) - 1) * plotWidth / (maxX - 1);
            int y = height - margin - (int)((yData.get(i) - minY) * plotHeight / (maxY - minY));
            g2d.fillOval(x - 4, y - 4, 8, 8);
        }
    }
    
    private static void drawXLabels(Graphics2D g2d, int margin, int plotWidth, int height, int maxX) {
        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("Arial", Font.PLAIN, 10));
        for (int i = 1; i <= maxX; i++) {
            int x = margin + (i - 1) * plotWidth / (maxX - 1);
            g2d.drawString(String.valueOf(i), x - 5, height - margin + 15);
        }
    }
    
    private static void drawYLabels(Graphics2D g2d, int margin, int plotHeight, int height,
                                  long minY, long maxY, boolean isTime) {
        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("Arial", Font.PLAIN, 10));
        for (int i = 0; i <= 5; i++) {
            long value = minY + (maxY - minY) * i / 5;
            int y = height - margin - i * plotHeight / 5;
            if (isTime) {
                g2d.drawString(String.format("%.0fk", value/1000.0), margin - 40, y + 5);
            } else {
                g2d.drawString(String.valueOf(value), margin - 40, y + 5);
            }
        }
    }
    
    private static void drawLegend(Graphics2D g2d, int width) {
        g2d.setColor(Color.GRAY);
        g2d.setStroke(new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{5}, 0));
        g2d.drawLine(width - 200, 60, width - 160, 60);
        g2d.setColor(Color.BLACK);
        g2d.drawString("Speedup Ideal", width - 150, 65);
        
        g2d.setColor(Color.BLUE);
        g2d.setStroke(new BasicStroke(3));
        g2d.drawLine(width - 200, 80, width - 160, 80);
        g2d.setColor(Color.BLACK);
        g2d.drawString("Speedup Real", width - 150, 85);
    }
    
    private static void saveImage(BufferedImage img, String fileName) throws IOException {
        File outputFile = new File(GRAFICAS_DIR, fileName);
        ImageIO.write(img, "PNG", outputFile);
        System.out.println("Generada: " + outputFile.getAbsolutePath());
    }
    
    // Clase para almacenar datos del benchmark
    private static class BenchmarkData {
        String fileName;
        String timestamp;
        int fileCount;
        List<Integer> threads = new ArrayList<>();
        List<Long> times = new ArrayList<>();
        List<Double> speedups = new ArrayList<>();
        List<Double> efficiency = new ArrayList<>();
        List<Double> throughput = new ArrayList<>();
    }
}