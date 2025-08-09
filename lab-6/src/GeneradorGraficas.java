import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;
import java.util.*;

public class GeneradorGraficas {
    
    // Datos del benchmark principal (32 URLs)
    private static final int[] HILOS = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16};
    private static final long[] TIEMPOS = {151811, 91687, 67622, 58388, 58167, 54285, 60817, 56437, 
                                          61026, 58941, 61541, 59396, 56971, 59868, 53597, 53903};
    private static final double[] SPEEDUP = {1.00, 1.66, 2.24, 2.60, 2.61, 2.80, 2.50, 2.69,
                                            2.49, 2.58, 2.47, 2.56, 2.66, 2.54, 2.83, 2.82};
    private static final double[] EFICIENCIA = {100.00, 82.79, 74.83, 65.00, 52.20, 46.61, 35.66, 33.62,
                                               27.64, 25.76, 22.43, 21.30, 20.50, 18.11, 18.88, 17.60};
    private static final double[] THROUGHPUT = {0.21, 0.35, 0.47, 0.55, 0.55, 0.59, 0.53, 0.57,
                                               0.52, 0.54, 0.52, 0.54, 0.56, 0.53, 0.60, 0.59};
    
    public static void main(String[] args) {
        try {
            generarGrafica1TiempoVsHilos();
            generarGrafica2SpeedupVsHilos();
            generarGrafica3ThroughputVsHilos();
            generarGrafica4EficienciaVsHilos();
            generarGrafica5Comparativa();
            
            System.out.println("Todas las gráficas generadas exitosamente");
            
        } catch (Exception e) {
            System.err.println("Error generando gráficas: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void generarGrafica1TiempoVsHilos() throws Exception {
        int width = 800, height = 600;
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = img.createGraphics();
        
        // Configuración
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, width, height);
        
        // Márgenes
        int margin = 80;
        int plotWidth = width - 2 * margin;
        int plotHeight = height - 2 * margin;
        
        // Encontrar min/max para escalado
        long maxTiempo = Arrays.stream(TIEMPOS).max().orElse(0);
        long minTiempo = Arrays.stream(TIEMPOS).min().orElse(0);
        
        // Dibujar ejes
        g2d.setColor(Color.BLACK);
        g2d.setStroke(new BasicStroke(2));
        g2d.drawLine(margin, height - margin, width - margin, height - margin); // X
        g2d.drawLine(margin, margin, margin, height - margin); // Y
        
        // Título
        g2d.setFont(new Font("Arial", Font.BOLD, 16));
        FontMetrics fm = g2d.getFontMetrics();
        String titulo = "Tiempo de Ejecución vs. Número de Hilos (32 URLs)";
        g2d.drawString(titulo, (width - fm.stringWidth(titulo)) / 2, 30);
        
        // Labels de ejes
        g2d.setFont(new Font("Arial", Font.PLAIN, 12));
        g2d.drawString("Número de Hilos", width/2 - 50, height - 20);
        
        // Rotar texto para eje Y
        Graphics2D g2dRotated = (Graphics2D) g2d.create();
        g2dRotated.rotate(-Math.PI/2, 20, height/2);
        g2dRotated.drawString("Tiempo (ms)", 20, height/2);
        g2dRotated.dispose();
        
        // Dibujar línea de datos
        g2d.setColor(Color.BLUE);
        g2d.setStroke(new BasicStroke(3));
        
        for (int i = 0; i < HILOS.length - 1; i++) {
            int x1 = margin + (HILOS[i] - 1) * plotWidth / 15;
            int y1 = height - margin - (int)((TIEMPOS[i] - minTiempo) * plotHeight / (maxTiempo - minTiempo));
            int x2 = margin + (HILOS[i+1] - 1) * plotWidth / 15;
            int y2 = height - margin - (int)((TIEMPOS[i+1] - minTiempo) * plotHeight / (maxTiempo - minTiempo));
            
            g2d.drawLine(x1, y1, x2, y2);
        }
        
        // Dibujar puntos
        g2d.setColor(Color.RED);
        for (int i = 0; i < HILOS.length; i++) {
            int x = margin + (HILOS[i] - 1) * plotWidth / 15;
            int y = height - margin - (int)((TIEMPOS[i] - minTiempo) * plotHeight / (maxTiempo - minTiempo));
            g2d.fillOval(x - 4, y - 4, 8, 8);
        }
        
        // Etiquetas de ejes
        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("Arial", Font.PLAIN, 10));
        for (int i = 1; i <= 16; i++) {
            int x = margin + (i - 1) * plotWidth / 15;
            g2d.drawString(String.valueOf(i), x - 5, height - margin + 15);
        }
        
        // Etiquetas Y (tiempo)
        for (int i = 0; i <= 5; i++) {
            long tiempo = minTiempo + (maxTiempo - minTiempo) * i / 5;
            int y = height - margin - i * plotHeight / 5;
            g2d.drawString(String.format("%.0fk", tiempo/1000.0), margin - 40, y + 5);
        }
        
        g2d.dispose();
        ImageIO.write(img, "PNG", new File("grafica1_tiempo_vs_hilos.png"));
        System.out.println("Generada: grafica1_tiempo_vs_hilos.png");
    }
    
    private static void generarGrafica2SpeedupVsHilos() throws Exception {
        int width = 800, height = 600;
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = img.createGraphics();
        
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, width, height);
        
        int margin = 80;
        int plotWidth = width - 2 * margin;
        int plotHeight = height - 2 * margin;
        
        // Dibujar ejes
        g2d.setColor(Color.BLACK);
        g2d.setStroke(new BasicStroke(2));
        g2d.drawLine(margin, height - margin, width - margin, height - margin);
        g2d.drawLine(margin, margin, margin, height - margin);
        
        // Título
        g2d.setFont(new Font("Arial", Font.BOLD, 16));
        FontMetrics fm = g2d.getFontMetrics();
        String titulo = "Speedup Real vs. Speedup Ideal";
        g2d.drawString(titulo, (width - fm.stringWidth(titulo)) / 2, 30);
        
        // Labels
        g2d.setFont(new Font("Arial", Font.PLAIN, 12));
        g2d.drawString("Número de Hilos", width/2 - 50, height - 20);
        
        Graphics2D g2dRotated = (Graphics2D) g2d.create();
        g2dRotated.rotate(-Math.PI/2, 20, height/2);
        g2dRotated.drawString("Speedup", 20, height/2);
        g2dRotated.dispose();
        
        // Speedup ideal (línea diagonal)
        g2d.setColor(Color.GRAY);
        g2d.setStroke(new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{5}, 0));
        g2d.drawLine(margin, height - margin, margin + plotWidth, margin);
        
        // Speedup real
        g2d.setColor(Color.BLUE);
        g2d.setStroke(new BasicStroke(3));
        
        for (int i = 0; i < HILOS.length - 1; i++) {
            int x1 = margin + (HILOS[i] - 1) * plotWidth / 15;
            int y1 = height - margin - (int)(SPEEDUP[i] * plotHeight / 16);
            int x2 = margin + (HILOS[i+1] - 1) * plotWidth / 15;
            int y2 = height - margin - (int)(SPEEDUP[i+1] * plotHeight / 16);
            
            g2d.drawLine(x1, y1, x2, y2);
        }
        
        // Puntos
        g2d.setColor(Color.RED);
        for (int i = 0; i < HILOS.length; i++) {
            int x = margin + (HILOS[i] - 1) * plotWidth / 15;
            int y = height - margin - (int)(SPEEDUP[i] * plotHeight / 16);
            g2d.fillOval(x - 4, y - 4, 8, 8);
        }
        
        // Leyenda
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
        
        // Etiquetas ejes
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
        ImageIO.write(img, "PNG", new File("grafica2_speedup_vs_hilos.png"));
        System.out.println("Generada: grafica2_speedup_vs_hilos.png");
    }
    
    private static void generarGrafica3ThroughputVsHilos() throws Exception {
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
        String titulo = "Throughput del Sistema (URLs/segundo)";
        g2d.drawString(titulo, (width - fm.stringWidth(titulo)) / 2, 30);
        
        // Barras
        g2d.setColor(new Color(0, 150, 0));
        int barWidth = plotWidth / HILOS.length - 5;
        
        double maxThroughput = Arrays.stream(THROUGHPUT).max().orElse(0);
        
        for (int i = 0; i < HILOS.length; i++) {
            int x = margin + i * (plotWidth / HILOS.length) + 2;
            int barHeight = (int)(THROUGHPUT[i] * plotHeight / maxThroughput);
            int y = height - margin - barHeight;
            
            g2d.fillRect(x, y, barWidth, barHeight);
            
            // Valor encima de cada barra
            g2d.setColor(Color.BLACK);
            g2d.setFont(new Font("Arial", Font.PLAIN, 8));
            String valor = String.format("%.2f", THROUGHPUT[i]);
            int textWidth = g2d.getFontMetrics().stringWidth(valor);
            g2d.drawString(valor, x + barWidth/2 - textWidth/2, y - 5);
            g2d.setColor(new Color(0, 150, 0));
        }
        
        // Labels
        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("Arial", Font.PLAIN, 12));
        g2d.drawString("Número de Hilos", width/2 - 50, height - 20);
        
        Graphics2D g2dRotated = (Graphics2D) g2d.create();
        g2dRotated.rotate(-Math.PI/2, 20, height/2);
        g2dRotated.drawString("URLs/segundo", 20, height/2);
        g2dRotated.dispose();
        
        // Etiquetas X
        g2d.setFont(new Font("Arial", Font.PLAIN, 10));
        for (int i = 0; i < HILOS.length; i++) {
            int x = margin + i * (plotWidth / HILOS.length) + (plotWidth / HILOS.length) / 2;
            g2d.drawString(String.valueOf(HILOS[i]), x - 5, height - margin + 15);
        }
        
        g2d.dispose();
        ImageIO.write(img, "PNG", new File("grafica3_throughput_vs_hilos.png"));
        System.out.println("Generada: grafica3_throughput_vs_hilos.png");
    }
    
    private static void generarGrafica4EficienciaVsHilos() throws Exception {
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
        String titulo = "Eficiencia vs. Número de Hilos";
        g2d.drawString(titulo, (width - fm.stringWidth(titulo)) / 2, 30);
        
        // Línea de eficiencia
        g2d.setColor(Color.RED);
        g2d.setStroke(new BasicStroke(3));
        
        for (int i = 0; i < HILOS.length - 1; i++) {
            int x1 = margin + (HILOS[i] - 1) * plotWidth / 15;
            int y1 = height - margin - (int)(EFICIENCIA[i] * plotHeight / 100);
            int x2 = margin + (HILOS[i+1] - 1) * plotWidth / 15;
            int y2 = height - margin - (int)(EFICIENCIA[i+1] * plotHeight / 100);
            
            g2d.drawLine(x1, y1, x2, y2);
        }
        
        // Puntos
        for (int i = 0; i < HILOS.length; i++) {
            int x = margin + (HILOS[i] - 1) * plotWidth / 15;
            int y = height - margin - (int)(EFICIENCIA[i] * plotHeight / 100);
            g2d.fillOval(x - 4, y - 4, 8, 8);
        }
        
        // Labels
        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("Arial", Font.PLAIN, 12));
        g2d.drawString("Número de Hilos", width/2 - 50, height - 20);
        
        Graphics2D g2dRotated = (Graphics2D) g2d.create();
        g2dRotated.rotate(-Math.PI/2, 20, height/2);
        g2dRotated.drawString("Eficiencia (%)", 20, height/2);
        g2dRotated.dispose();
        
        // Etiquetas
        g2d.setFont(new Font("Arial", Font.PLAIN, 10));
        for (int i = 1; i <= 16; i++) {
            int x = margin + (i - 1) * plotWidth / 15;
            g2d.drawString(String.valueOf(i), x - 5, height - margin + 15);
        }
        
        for (int i = 0; i <= 100; i += 20) {
            int y = height - margin - i * plotHeight / 100;
            g2d.drawString(i + "%", margin - 30, y + 5);
        }
        
        g2d.dispose();
        ImageIO.write(img, "PNG", new File("grafica4_eficiencia_vs_hilos.png"));
        System.out.println("Generada: grafica4_eficiencia_vs_hilos.png");
    }
    
    private static void generarGrafica5Comparativa() throws Exception {
        // Datos comparativos por cantidad de URLs
        int[] urls = {1, 12, 32};
        double[] speedupMax = {1.18, 2.77, 2.83};  // Speedup máximo de cada benchmark
        int[] hilosOptimos = {15, 6, 15};  // Hilos óptimos de cada benchmark
        
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
        String titulo = "Comparativa: Speedup Máximo por Cantidad de URLs";
        g2d.drawString(titulo, (width - fm.stringWidth(titulo)) / 2, 30);
        
        // Barras comparativas
        Color[] colores = {Color.BLUE, Color.GREEN, Color.RED};
        int barWidth = plotWidth / (urls.length * 2);
        
        for (int i = 0; i < urls.length; i++) {
            g2d.setColor(colores[i]);
            int x = margin + 50 + i * 200;
            int barHeight = (int)(speedupMax[i] * plotHeight / 3);
            int y = height - margin - barHeight;
            
            g2d.fillRect(x, y, barWidth, barHeight);
            
            // Etiquetas
            g2d.setColor(Color.BLACK);
            g2d.setFont(new Font("Arial", Font.BOLD, 12));
            g2d.drawString(urls[i] + " URL" + (urls[i] > 1 ? "s" : ""), x - 10, height - margin + 20);
            g2d.drawString(String.format("%.2fx", speedupMax[i]), x - 10, y - 10);
            g2d.drawString("(" + hilosOptimos[i] + " hilos)", x - 15, y - 25);
        }
        
        // Labels
        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("Arial", Font.PLAIN, 12));
        g2d.drawString("Configuración de Prueba", width/2 - 70, height - 20);
        
        Graphics2D g2dRotated = (Graphics2D) g2d.create();
        g2dRotated.rotate(-Math.PI/2, 20, height/2);
        g2dRotated.drawString("Speedup Máximo", 20, height/2);
        g2dRotated.dispose();
        
        // Escala Y
        g2d.setFont(new Font("Arial", Font.PLAIN, 10));
        for (int i = 0; i <= 3; i++) {
            int y = height - margin - i * plotHeight / 3;
            g2d.drawString(String.valueOf(i), margin - 20, y + 5);
        }
        
        g2d.dispose();
        ImageIO.write(img, "PNG", new File("grafica5_comparativa_urls.png"));
        System.out.println("Generada: grafica5_comparativa_urls.png");
    }
}
