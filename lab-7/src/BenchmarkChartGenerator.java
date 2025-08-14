import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BenchmarkChartGenerator {
    
    static class BenchmarkData {
        String timestamp;
        int filesProcessed;
        double[] tiempos = new double[16];
        double[] mejoras = new double[16];
        int optimalThreads;
        double minTime;
        double maxImprovement;
        double efficiency;
        
        BenchmarkData(String timestamp, int filesProcessed) {
            this.timestamp = timestamp;
            this.filesProcessed = filesProcessed;
        }
    }
    
    public static void main(String[] args) {
        try {
            // Check multiple possible directories
            String[] possibleDirs = {".", "reports", "../reports", "./reports"};
            String benchmarkDir = null;
            String[] benchmarkFiles = null;
            
            for (String dir : possibleDirs) {
                File dirFile = new File(dir);
                if (dirFile.exists() && dirFile.isDirectory()) {
                    String[] files = dirFile.list((d, name) -> 
                        name.startsWith("benchmark_") && name.endsWith(".txt"));
                    if (files != null && files.length > 0) {
                        benchmarkDir = dir;
                        benchmarkFiles = files;
                        break;
                    }
                }
            }
            
            if (benchmarkFiles == null || benchmarkFiles.length == 0) {
                System.err.println("No se encontraron archivos benchmark en los directorios:");
                for (String dir : possibleDirs) {
                    System.err.println("  " + new File(dir).getAbsolutePath());
                }
                return;
            }
            
            Arrays.sort(benchmarkFiles);
            System.out.println("Encontrados " + benchmarkFiles.length + " archivos benchmark en: " + 
                             new File(benchmarkDir).getAbsolutePath());
            
            // Parse datos
            List<BenchmarkData> benchmarks = new ArrayList<>();
            for (String filename : benchmarkFiles) {
                try {
                    String fullPath = benchmarkDir.equals(".") ? filename : benchmarkDir + "/" + filename;
                    BenchmarkData data = parseBenchmarkFile(fullPath);
                    if (data != null) {
                        benchmarks.add(data);
                        System.out.println("Parseado: " + filename);
                    }
                } catch (Exception e) {
                    System.err.println("Error parseando " + filename + ": " + e.getMessage());
                }
            }
            
            if (benchmarks.isEmpty()) {
                System.err.println("No se pudieron parsear datos!");
                return;
            }
            
            // Generar gráficas
            generarGraficaTiemposComparativa(benchmarks);
            generarGraficaMejorasComparativa(benchmarks);
            generarGraficaConfiguracionOptima(benchmarks);
            generarGraficaEficiencias(benchmarks);
            generarGraficaResumen(benchmarks);
            
            System.out.println("Todas las gráficas generadas exitosamente");
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private static BenchmarkData parseBenchmarkFile(String filename) throws IOException {
        String content = new String(Files.readAllBytes(Paths.get(filename)));
        
        // Extraer timestamp
        Pattern timestampPattern = Pattern.compile("Fecha y hora: (\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2})");
        Matcher timestampMatcher = timestampPattern.matcher(content);
        String timestamp = timestampMatcher.find() ? timestampMatcher.group(1) : filename;
        
        // Extraer archivos procesados
        Pattern filesPattern = Pattern.compile("Archivos procesados: (\\d+)");
        Matcher filesMatcher = filesPattern.matcher(content);
        int filesProcessed = filesMatcher.find() ? Integer.parseInt(filesMatcher.group(1)) : 30;
        
        BenchmarkData data = new BenchmarkData(timestamp, filesProcessed);
        
        // Extraer datos de rendimiento
        Pattern dataPattern = Pattern.compile("(\\d+)\\s+\\|\\s+(\\d+)\\s+\\|\\s+([\\d,]+)\\s+\\|\\s+([\\d,]+)x");
        Matcher dataMatcher = dataPattern.matcher(content);
        
        while (dataMatcher.find()) {
            int threads = Integer.parseInt(dataMatcher.group(1));
            double timeMs = Double.parseDouble(dataMatcher.group(2));
            double improvement = Double.parseDouble(dataMatcher.group(4).replace(",", "."));
            
            if (threads >= 1 && threads <= 16) {
                data.tiempos[threads - 1] = timeMs / 1000.0;
                data.mejoras[threads - 1] = improvement;
            }
        }
        
        // Extraer análisis
        Pattern optimalPattern = Pattern.compile("Configuración óptima: (\\d+) hilos");
        Matcher optimalMatcher = optimalPattern.matcher(content);
        data.optimalThreads = optimalMatcher.find() ? Integer.parseInt(optimalMatcher.group(1)) : 1;
        
        Pattern minTimePattern = Pattern.compile("Tiempo mínimo: (\\d+) ms");
        Matcher minTimeMatcher = minTimePattern.matcher(content);
        data.minTime = minTimeMatcher.find() ? Double.parseDouble(minTimeMatcher.group(1)) / 1000.0 : 0;
        
        Pattern maxImprovementPattern = Pattern.compile("Mejora máxima: ([\\d,]+)x");
        Matcher maxImprovementMatcher = maxImprovementPattern.matcher(content);
        data.maxImprovement = maxImprovementMatcher.find() ? 
            Double.parseDouble(maxImprovementMatcher.group(1).replace(",", ".")) : 1.0;
        
        Pattern efficiencyPattern = Pattern.compile("Eficiencia por hilo: ([\\d,]+)%");
        Matcher efficiencyMatcher = efficiencyPattern.matcher(content);
        data.efficiency = efficiencyMatcher.find() ? 
            Double.parseDouble(efficiencyMatcher.group(1).replace(",", ".")) : 0;
        
        return data;
    }
    
    private static void generarGraficaTiemposComparativa(List<BenchmarkData> benchmarks) throws IOException {
        int width = 1000, height = 700;
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = img.createGraphics();
        
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, width, height);
        
        int margin = 80;
        int plotWidth = width - 2 * margin;
        int plotHeight = height - 2 * margin;
        
        // Ejes
        g2d.setColor(Color.BLACK);
        g2d.setStroke(new BasicStroke(2));
        g2d.drawLine(margin, height - margin, width - margin, height - margin);
        g2d.drawLine(margin, margin, margin, height - margin);
        
        // Título
        g2d.setFont(new Font("Arial", Font.BOLD, 16));
        FontMetrics fm = g2d.getFontMetrics();
        String titulo = "Comparativa de Tiempos de Ejecución - Todos los Tests";
        g2d.drawString(titulo, (width - fm.stringWidth(titulo)) / 2, 30);
        
        // Labels
        g2d.setFont(new Font("Arial", Font.PLAIN, 12));
        g2d.drawString("Número de Hilos", width/2 - 50, height - 20);
        
        Graphics2D g2dRotated = (Graphics2D) g2d.create();
        g2dRotated.rotate(-Math.PI/2, 20, height/2);
        g2dRotated.drawString("Tiempo (segundos)", 20, height/2);
        g2dRotated.dispose();
        
        // Encontrar max tiempo para escala
        double maxTime = 0;
        for (BenchmarkData data : benchmarks) {
            for (double tiempo : data.tiempos) {
                if (tiempo > maxTime) maxTime = tiempo;
            }
        }
        
        // Colores para cada test
        Color[] colores = {Color.BLUE, Color.RED, Color.GREEN, Color.ORANGE, 
                          Color.MAGENTA, Color.CYAN, Color.PINK};
        
        // Dibujar líneas para cada benchmark
        for (int b = 0; b < benchmarks.size(); b++) {
            BenchmarkData data = benchmarks.get(b);
            g2d.setColor(colores[b % colores.length]);
            g2d.setStroke(new BasicStroke(2));
            
            for (int i = 0; i < 15; i++) {
                if (data.tiempos[i] > 0 && data.tiempos[i + 1] > 0) {
                    int x1 = margin + i * plotWidth / 15;
                    int y1 = height - margin - (int)(data.tiempos[i] * plotHeight / maxTime);
                    int x2 = margin + (i + 1) * plotWidth / 15;
                    int y2 = height - margin - (int)(data.tiempos[i + 1] * plotHeight / maxTime);
                    
                    g2d.drawLine(x1, y1, x2, y2);
                }
            }
            
            // Puntos
            for (int i = 0; i < 16; i++) {
                if (data.tiempos[i] > 0) {
                    int x = margin + i * plotWidth / 15;
                    int y = height - margin - (int)(data.tiempos[i] * plotHeight / maxTime);
                    g2d.fillOval(x - 3, y - 3, 6, 6);
                }
            }
        }
        
        // Leyenda
        int legendY = 60;
        for (int b = 0; b < benchmarks.size(); b++) {
            g2d.setColor(colores[b % colores.length]);
            g2d.fillRect(width - 250, legendY + b * 20, 15, 10);
            g2d.setColor(Color.BLACK);
            g2d.drawString("Test " + (b + 1) + " (" + benchmarks.get(b).filesProcessed + " archivos)", 
                          width - 230, legendY + b * 20 + 10);
        }
        
        // Etiquetas X
        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("Arial", Font.PLAIN, 10));
        for (int i = 1; i <= 16; i++) {
            int x = margin + (i - 1) * plotWidth / 15;
            g2d.drawString(String.valueOf(i), x - 5, height - margin + 15);
        }
        
        // Etiquetas Y
        for (int i = 0; i <= 5; i++) {
            double tiempo = maxTime * i / 5;
            int y = height - margin - i * plotHeight / 5;
            g2d.drawString(String.format("%.1f", tiempo), margin - 40, y + 5);
        }
        
        g2d.dispose();
        ImageIO.write(img, "PNG", new File("grafica1_tiempos_comparativa.png"));
        System.out.println("Generada: grafica1_tiempos_comparativa.png");
    }
    
    private static void generarGraficaMejorasComparativa(List<BenchmarkData> benchmarks) throws IOException {
        int width = 1000, height = 700;
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = img.createGraphics();
        
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, width, height);
        
        int margin = 80;
        int plotWidth = width - 2 * margin;
        int plotHeight = height - 2 * margin;
        
        // Ejes
        g2d.setColor(Color.BLACK);
        g2d.setStroke(new BasicStroke(2));
        g2d.drawLine(margin, height - margin, width - margin, height - margin);
        g2d.drawLine(margin, margin, margin, height - margin);
        
        // Título
        g2d.setFont(new Font("Arial", Font.BOLD, 16));
        FontMetrics fm = g2d.getFontMetrics();
        String titulo = "Comparativa de Mejoras de Rendimiento (Speedup)";
        g2d.drawString(titulo, (width - fm.stringWidth(titulo)) / 2, 30);
        
        // Labels
        g2d.setFont(new Font("Arial", Font.PLAIN, 12));
        g2d.drawString("Número de Hilos", width/2 - 50, height - 20);
        
        Graphics2D g2dRotated = (Graphics2D) g2d.create();
        g2dRotated.rotate(-Math.PI/2, 20, height/2);
        g2dRotated.drawString("Mejora (x)", 20, height/2);
        g2dRotated.dispose();
        
        // Speedup ideal (línea diagonal)
        g2d.setColor(Color.GRAY);
        g2d.setStroke(new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{5}, 0));
        g2d.drawLine(margin, height - margin, margin + plotWidth, margin);
        
        Color[] colores = {Color.BLUE, Color.RED, Color.GREEN, Color.ORANGE, 
                          Color.MAGENTA, Color.CYAN, Color.PINK};
        
        // Dibujar líneas de mejora
        for (int b = 0; b < benchmarks.size(); b++) {
            BenchmarkData data = benchmarks.get(b);
            g2d.setColor(colores[b % colores.length]);
            g2d.setStroke(new BasicStroke(2));
            
            for (int i = 0; i < 15; i++) {
                if (data.mejoras[i] > 0 && data.mejoras[i + 1] > 0) {
                    int x1 = margin + i * plotWidth / 15;
                    int y1 = height - margin - (int)(data.mejoras[i] * plotHeight / 16);
                    int x2 = margin + (i + 1) * plotWidth / 15;
                    int y2 = height - margin - (int)(data.mejoras[i + 1] * plotHeight / 16);
                    
                    g2d.drawLine(x1, y1, x2, y2);
                }
            }
            
            // Puntos
            for (int i = 0; i < 16; i++) {
                if (data.mejoras[i] > 0) {
                    int x = margin + i * plotWidth / 15;
                    int y = height - margin - (int)(data.mejoras[i] * plotHeight / 16);
                    g2d.fillOval(x - 3, y - 3, 6, 6);
                }
            }
        }
        
        // Leyenda
        g2d.setColor(Color.GRAY);
        g2d.setStroke(new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{5}, 0));
        g2d.drawLine(width - 200, 60, width - 160, 60);
        g2d.setColor(Color.BLACK);
        g2d.drawString("Speedup Ideal", width - 150, 65);
        
        int legendY = 80;
        for (int b = 0; b < benchmarks.size(); b++) {
            g2d.setColor(colores[b % colores.length]);
            g2d.fillRect(width - 250, legendY + b * 20, 15, 10);
            g2d.setColor(Color.BLACK);
            g2d.drawString("Test " + (b + 1), width - 230, legendY + b * 20 + 10);
        }
        
        // Etiquetas
        g2d.setFont(new Font("Arial", Font.PLAIN, 10));
        for (int i = 1; i <= 16; i++) {
            int x = margin + (i - 1) * plotWidth / 15;
            g2d.drawString(String.valueOf(i), x - 5, height - margin + 15);
        }
        
        for (int i = 0; i <= 16; i += 4) {
            int y = height - margin - i * plotHeight / 16;
            g2d.drawString(String.valueOf(i), margin - 20, y + 5);
        }
        
        g2d.dispose();
        ImageIO.write(img, "PNG", new File("grafica2_mejoras_comparativa.png"));
        System.out.println("Generada: grafica2_mejoras_comparativa.png");
    }
    
    private static void generarGraficaConfiguracionOptima(List<BenchmarkData> benchmarks) throws IOException {
        int width = 800, height = 600;
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = img.createGraphics();
        
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, width, height);
        
        int margin = 80;
        int plotWidth = width - 2 * margin;
        int plotHeight = height - 2 * margin;
        
        // Ejes
        g2d.setColor(Color.BLACK);
        g2d.setStroke(new BasicStroke(2));
        g2d.drawLine(margin, height - margin, width - margin, height - margin);
        g2d.drawLine(margin, margin, margin, height - margin);
        
        // Título
        g2d.setFont(new Font("Arial", Font.BOLD, 16));
        FontMetrics fm = g2d.getFontMetrics();
        String titulo = "Configuración Óptima por Test";
        g2d.drawString(titulo, (width - fm.stringWidth(titulo)) / 2, 30);
        
        // Barras
        g2d.setColor(new Color(0, 150, 200));
        int barWidth = plotWidth / benchmarks.size() - 10;
        
        for (int i = 0; i < benchmarks.size(); i++) {
            BenchmarkData data = benchmarks.get(i);
            int x = margin + i * (plotWidth / benchmarks.size()) + 5;
            int barHeight = data.optimalThreads * plotHeight / 16;
            int y = height - margin - barHeight;
            
            g2d.fillRect(x, y, barWidth, barHeight);
            
            // Valor encima
            g2d.setColor(Color.BLACK);
            g2d.setFont(new Font("Arial", Font.BOLD, 10));
            String valor = data.optimalThreads + " hilos";
            int textWidth = g2d.getFontMetrics().stringWidth(valor);
            g2d.drawString(valor, x + barWidth/2 - textWidth/2, y - 5);
            
            // Mejora máxima debajo
            String mejora = String.format("%.2fx", data.maxImprovement);
            textWidth = g2d.getFontMetrics().stringWidth(mejora);
            g2d.drawString(mejora, x + barWidth/2 - textWidth/2, height - margin + 35);
            
            g2d.setColor(new Color(0, 150, 200));
        }
        
        // Labels
        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("Arial", Font.PLAIN, 12));
        g2d.drawString("Número de Test", width/2 - 50, height - 20);
        
        Graphics2D g2dRotated = (Graphics2D) g2d.create();
        g2dRotated.rotate(-Math.PI/2, 20, height/2);
        g2dRotated.drawString("Hilos Óptimos", 20, height/2);
        g2dRotated.dispose();
        
        // Etiquetas X
        g2d.setFont(new Font("Arial", Font.PLAIN, 10));
        for (int i = 0; i < benchmarks.size(); i++) {
            int x = margin + i * (plotWidth / benchmarks.size()) + (plotWidth / benchmarks.size()) / 2;
            g2d.drawString("Test " + (i + 1), x - 15, height - margin + 15);
            g2d.drawString("(" + benchmarks.get(i).filesProcessed + " files)", x - 20, height - margin + 50);
        }
        
        // Etiquetas Y
        for (int i = 0; i <= 16; i += 4) {
            int y = height - margin - i * plotHeight / 16;
            g2d.drawString(String.valueOf(i), margin - 20, y + 5);
        }
        
        g2d.dispose();
        ImageIO.write(img, "PNG", new File("grafica3_configuracion_optima.png"));
        System.out.println("Generada: grafica3_configuracion_optima.png");
    }
    
    private static void generarGraficaEficiencias(List<BenchmarkData> benchmarks) throws IOException {
        int width = 800, height = 600;
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = img.createGraphics();
        
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, width, height);
        
        int margin = 80;
        int plotWidth = width - 2 * margin;
        int plotHeight = height - 2 * margin;
        
        // Ejes
        g2d.setColor(Color.BLACK);
        g2d.setStroke(new BasicStroke(2));
        g2d.drawLine(margin, height - margin, width - margin, height - margin);
        g2d.drawLine(margin, margin, margin, height - margin);
        
        // Título
        g2d.setFont(new Font("Arial", Font.BOLD, 16));
        FontMetrics fm = g2d.getFontMetrics();
        String titulo = "Eficiencia por Hilo - Comparativa";
        g2d.drawString(titulo, (width - fm.stringWidth(titulo)) / 2, 30);
        
        // Barras de eficiencia
        Color[] colores = {new Color(255, 100, 100), new Color(100, 255, 100), new Color(100, 100, 255),
                          new Color(255, 255, 100), new Color(255, 100, 255), new Color(100, 255, 255),
                          new Color(200, 200, 200)};
        
        int barWidth = plotWidth / benchmarks.size() - 10;
        
        for (int i = 0; i < benchmarks.size(); i++) {
            BenchmarkData data = benchmarks.get(i);
            g2d.setColor(colores[i % colores.length]);
            
            int x = margin + i * (plotWidth / benchmarks.size()) + 5;
            int barHeight = (int)(data.efficiency * plotHeight / 100);
            int y = height - margin - barHeight;
            
            g2d.fillRect(x, y, barWidth, barHeight);
            
            // Valor
            g2d.setColor(Color.BLACK);
            g2d.setFont(new Font("Arial", Font.BOLD, 9));
            String valor = String.format("%.1f%%", data.efficiency);
            int textWidth = g2d.getFontMetrics().stringWidth(valor);
            g2d.drawString(valor, x + barWidth/2 - textWidth/2, y - 5);
        }
        
        // Labels
        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("Arial", Font.PLAIN, 12));
        g2d.drawString("Número de Test", width/2 - 50, height - 20);
        
        Graphics2D g2dRotated = (Graphics2D) g2d.create();
        g2dRotated.rotate(-Math.PI/2, 20, height/2);
        g2dRotated.drawString("Eficiencia (%)", 20, height/2);
        g2dRotated.dispose();
        
        // Etiquetas
        g2d.setFont(new Font("Arial", Font.PLAIN, 10));
        for (int i = 0; i < benchmarks.size(); i++) {
            int x = margin + i * (plotWidth / benchmarks.size()) + (plotWidth / benchmarks.size()) / 2;
            g2d.drawString("Test " + (i + 1), x - 15, height - margin + 15);
        }
        
        for (int i = 0; i <= 100; i += 20) {
            int y = height - margin - i * plotHeight / 100;
            g2d.drawString(i + "%", margin - 30, y + 5);
        }
        
        g2d.dispose();
        ImageIO.write(img, "PNG", new File("grafica4_eficiencias.png"));
        System.out.println("Generada: grafica4_eficiencias.png");
    }
    
    private static void generarGraficaResumen(List<BenchmarkData> benchmarks) throws IOException {
        int width = 1000, height = 600;
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = img.createGraphics();
        
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, width, height);
        
        // Título principal
        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("Arial", Font.BOLD, 18));
        FontMetrics fm = g2d.getFontMetrics();
        String titulo = "Resumen Comparativo de Benchmarks PDF";
        g2d.drawString(titulo, (width - fm.stringWidth(titulo)) / 2, 30);
        
        // Crear tabla resumen
        int startY = 80;
        int rowHeight = 40;
        int colWidth = width / 8;
        
        // Headers
        g2d.setFont(new Font("Arial", Font.BOLD, 11));
        String[] headers = {"Test", "Archivos", "Hilos Ópt.", "Mejor Tiempo", "Max Speedup", "Eficiencia", "Timestamp"};
        
        for (int i = 0; i < headers.length; i++) {
            g2d.drawString(headers[i], 20 + i * colWidth, startY);
        }
        
        // Línea separadora
        g2d.setStroke(new BasicStroke(1));
        g2d.drawLine(20, startY + 10, width - 20, startY + 10);
        
        // Datos
        g2d.setFont(new Font("Arial", Font.PLAIN, 10));
        for (int row = 0; row < benchmarks.size(); row++) {
            BenchmarkData data = benchmarks.get(row);
            int y = startY + 30 + row * rowHeight;
            
            // Alternar colores de fondo
            if (row % 2 == 0) {
                g2d.setColor(new Color(245, 245, 245));
                g2d.fillRect(20, y - 15, width - 40, rowHeight - 5);
            }
            
            g2d.setColor(Color.BLACK);
            
            // Datos de la fila
            String[] datos = {
                "Test " + (row + 1),
                String.valueOf(data.filesProcessed),
                String.valueOf(data.optimalThreads),
                String.format("%.1fs", data.minTime),
                String.format("%.2fx", data.maxImprovement),
                String.format("%.1f%%", data.efficiency),
                data.timestamp.substring(11) // Solo hora
            };
            
            for (int col = 0; col < datos.length; col++) {
                g2d.drawString(datos[col], 20 + col * colWidth, y);
            }
        }
        
        // Estadísticas generales
        int statsY = startY + 50 + benchmarks.size() * rowHeight;
        g2d.setFont(new Font("Arial", Font.BOLD, 14));
        g2d.drawString("Estadísticas Generales:", 20, statsY);
        
        // Calcular estadísticas
        double avgSpeedup = benchmarks.stream().mapToDouble(d -> d.maxImprovement).average().orElse(0);
        double avgEfficiency = benchmarks.stream().mapToDouble(d -> d.efficiency).average().orElse(0);
        int bestTest = 0;
        double bestSpeedup = 0;
        
        for (int i = 0; i < benchmarks.size(); i++) {
            if (benchmarks.get(i).maxImprovement > bestSpeedup) {
                bestSpeedup = benchmarks.get(i).maxImprovement;
                bestTest = i + 1;
            }
        }
        
        g2d.setFont(new Font("Arial", Font.PLAIN, 12));
        g2d.drawString("Speedup promedio: " + String.format("%.2fx", avgSpeedup), 40, statsY + 30);
        g2d.drawString("Eficiencia promedio: " + String.format("%.1f%%", avgEfficiency), 40, statsY + 50);
        g2d.drawString("Mejor resultado: Test " + bestTest + " (" + String.format("%.2fx", bestSpeedup) + ")", 40, statsY + 70);
        
        g2d.dispose();
        ImageIO.write(img, "PNG", new File("grafica5_resumen.png"));
        System.out.println("Generada: grafica5_resumen.png");
    }
}