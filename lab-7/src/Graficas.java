import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

class BenchmarkData {
    String fileName;
    String timestamp;
    int fileCount;
    Map<Integer, PerformanceResult> results;
}

class PerformanceResult {
    long timeMs;
    double improvement;
    
    PerformanceResult(long timeMs, double improvement) {
        this.timeMs = timeMs;
        this.improvement = improvement;
    }
}

class LinearRegression {
    double slope;
    double intercept;
    double correlation;
    
    LinearRegression(double slope, double intercept, double correlation) {
        this.slope = slope;
        this.intercept = intercept;
        this.correlation = correlation;
    }
}

public class Graficas {
    
    public static void generateReports() {
        List<Path> reportFiles = FileManager.getReportFiles();
        
        if (reportFiles.isEmpty()) {
            System.out.println("No se encontraron reportes. Ejecute primero el análisis de rendimiento.");
            return;
        }
        
        System.out.println("Reportes encontrados: " + reportFiles.size());
        
        List<BenchmarkData> allData = new ArrayList<>();
        
        for (Path reportFile : reportFiles) {
            BenchmarkData data = parseReport(reportFile);
            if (data != null) {
                allData.add(data);
                System.out.println("Procesado: " + reportFile.getFileName());
            }
        }
        
        if (allData.isEmpty()) {
            System.out.println("No se pudieron procesar los reportes.");
            return;
        }
        
        generateComparativeTable(allData);
        generateBarChart(allData);
        generateRegressionAnalysis(allData);
        generateSummaryReport(allData);
    }
    
    private static BenchmarkData parseReport(Path reportFile) {
        try {
            List<String> lines = Files.readAllLines(reportFile);
            BenchmarkData data = new BenchmarkData();
            data.fileName = reportFile.getFileName().toString();
            data.results = new LinkedHashMap<>();
            
            boolean inDataSection = false;
            
            for (String line : lines) {
                if (line.contains("Fecha y hora:")) {
                    data.timestamp = line.substring(line.indexOf(":") + 1).trim();
                }
                if (line.contains("Archivos procesados:")) {
                    try {
                        data.fileCount = Integer.parseInt(line.substring(line.indexOf(":") + 1).trim());
                    } catch (NumberFormatException e) {
                        data.fileCount = 0;
                    }
                }
                if (line.startsWith("Hilos,TiempoMs")) {
                    inDataSection = true;
                    continue;
                }
                
                if (inDataSection && line.contains(",")) {
                    String[] parts = line.split(",");
                    if (parts.length >= 4) {
                        try {
                            int threads = Integer.parseInt(parts[0]);
                            long timeMs = Long.parseLong(parts[1]);
                            double improvement = Double.parseDouble(parts[3]);
                            data.results.put(threads, new PerformanceResult(timeMs, improvement));
                        } catch (NumberFormatException e) {
                            // Ignorar líneas malformadas
                        }
                    }
                }
            }
            
            return data.results.isEmpty() ? null : data;
            
        } catch (IOException e) {
            System.err.println("Error leyendo reporte " + reportFile + ": " + e.getMessage());
            return null;
        }
    }
    
    private static void generateComparativeTable(List<BenchmarkData> allData) {
        StringBuilder table = new StringBuilder();
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        
        table.append("=".repeat(100)).append("\n");
        table.append("TABLA COMPARATIVA DE RENDIMIENTO\n");
        table.append("=".repeat(100)).append("\n");
        table.append("Generada: ").append(timestamp).append("\n");
        table.append("Reportes analizados: ").append(allData.size()).append("\n\n");
        
        // Encabezado de la tabla
        table.append(String.format("%-8s", "Hilos"));
        for (int i = 0; i < allData.size(); i++) {
            table.append(String.format(" | %-15s", "Test" + (i + 1) + " (ms)"));
        }
        table.append(" | %-15s | %-15s\n", "Promedio (ms)", "Desv. Std");
        table.append("-".repeat(100)).append("\n");
        
        // Datos de la tabla
        for (int threads = 1; threads <= 16; threads++) {
            table.append(String.format("%-8d", threads));
            
            List<Long> times = new ArrayList<>();
            for (BenchmarkData data : allData) {
                PerformanceResult result = data.results.get(threads);
                if (result != null) {
                    table.append(String.format(" | %-15d", result.timeMs));
                    times.add(result.timeMs);
                } else {
                    table.append(String.format(" | %-15s", "N/A"));
                }
            }
            
            if (!times.isEmpty()) {
                double avg = times.stream().mapToLong(Long::longValue).average().orElse(0.0);
                double stdDev = calculateStandardDeviation(times, avg);
                table.append(String.format(" | %-15.0f | %-15.2f\n", avg, stdDev));
            } else {
                table.append(" | N/A             | N/A\n");
            }
        }
        
        String fileName = "comparative_table_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".txt";
        FileManager.saveReport(table.toString(), fileName);
    }
    
    private static void generateBarChart(List<BenchmarkData> allData) {
        StringBuilder chart = new StringBuilder();
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        
        chart.append("=".repeat(80)).append("\n");
        chart.append("GRÁFICO DE BARRAS - RENDIMIENTO POR HILOS\n");
        chart.append("=".repeat(80)).append("\n");
        chart.append("Generado: ").append(timestamp).append("\n\n");
        
        // Calcular promedios
        Map<Integer, Double> averages = new LinkedHashMap<>();
        for (int threads = 1; threads <= 16; threads++) {
            List<Long> times = allData.stream()
                .map(data -> data.results.get(threads))
                .filter(Objects::nonNull)
                .map(result -> result.timeMs)
                .collect(Collectors.toList());
            
            if (!times.isEmpty()) {
                double avg = times.stream().mapToLong(Long::longValue).average().orElse(0.0);
                averages.put(threads, avg);
            }
        }
        
        // Encontrar valor máximo para escalar
        double maxTime = averages.values().stream().mapToDouble(Double::doubleValue).max().orElse(1.0);
        int maxBarLength = 50;
        
        chart.append("Tiempo promedio por número de hilos (ms):\n\n");
        
        for (Map.Entry<Integer, Double> entry : averages.entrySet()) {
            int threads = entry.getKey();
            double time = entry.getValue();
            int barLength = (int) ((time / maxTime) * maxBarLength);
            
            chart.append(String.format("%-3d hilos [%-8.0f ms] ", threads, time));
            chart.append("█".repeat(barLength));
            chart.append("\n");
        }
        
        chart.append("\nEscala: █ = ").append(String.format("%.0f", maxTime / maxBarLength)).append(" ms\n");
        
        String fileName = "bar_chart_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".txt";
        FileManager.saveReport(chart.toString(), fileName);
    }
    
    private static void generateRegressionAnalysis(List<BenchmarkData> allData) {
        StringBuilder analysis = new StringBuilder();
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        
        analysis.append("=".repeat(80)).append("\n");
        analysis.append("ANÁLISIS DE REGRESIÓN LINEAL\n");
        analysis.append("=".repeat(80)).append("\n");
        analysis.append("Generado: ").append(timestamp).append("\n\n");
        
        // Calcular promedios para regresión
        List<Double> xValues = new ArrayList<>(); // Hilos
        List<Double> yValues = new ArrayList<>(); // Tiempos promedio
        
        for (int threads = 1; threads <= 16; threads++) {
            List<Long> times = allData.stream()
                .map(data -> data.results.get(threads))
                .filter(Objects::nonNull)
                .map(result -> result.timeMs)
                .collect(Collectors.toList());
            
            if (!times.isEmpty()) {
                double avg = times.stream().mapToLong(Long::longValue).average().orElse(0.0);
                xValues.add((double) threads);
                yValues.add(avg);
            }
        }
        
        if (xValues.size() >= 2) {
            LinearRegression regression = calculateLinearRegression(xValues, yValues);
            
            analysis.append("ECUACIÓN DE REGRESIÓN LINEAL:\n");
            analysis.append("y = ").append(String.format("%.2f", regression.slope))
                .append("x + ").append(String.format("%.2f", regression.intercept)).append("\n");
            analysis.append("Coeficiente de correlación (r): ").append(String.format("%.4f", regression.correlation)).append("\n\n");
            
            analysis.append("INTERPRETACIÓN:\n");
            if (regression.correlation < -0.7) {
                analysis.append("- Correlación fuerte negativa: Mayor número de hilos reduce significativamente el tiempo\n");
            } else if (regression.correlation < -0.3) {
                analysis.append("- Correlación moderada negativa: Mayor número de hilos reduce el tiempo\n");
            } else {
                analysis.append("- Correlación débil: El número de hilos tiene poco impacto en el rendimiento\n");
            }
            
            analysis.append("\nPREDICCIONES:\n");
            for (int threads = 1; threads <= 16; threads += 3) {
                double predicted = regression.slope * threads + regression.intercept;
                analysis.append(String.format("%-3d hilos: %.0f ms (predicción)\n", threads, predicted));
            }
        }
        
        String fileName = "regression_analysis_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".txt";
        FileManager.saveReport(analysis.toString(), fileName);
    }
    
    private static void generateSummaryReport(List<BenchmarkData> allData) {
        StringBuilder summary = new StringBuilder();
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        
        summary.append("=".repeat(80)).append("\n");
        summary.append("RESUMEN EJECUTIVO - ANÁLISIS DE RENDIMIENTO\n");
        summary.append("=".repeat(80)).append("\n");
        summary.append("Generado: ").append(timestamp).append("\n\n");
        
        summary.append("INFORMACIÓN GENERAL:\n");
        summary.append("- Total de pruebas analizadas: ").append(allData.size()).append("\n");
        summary.append("- Período de análisis: ");
        if (!allData.isEmpty()) {
            summary.append(allData.get(0).timestamp).append(" - ").append(allData.get(allData.size()-1).timestamp);
        }
        summary.append("\n\n");
        
        // Encontrar configuración óptima promedio
        Map<Integer, Double> averages = new LinkedHashMap<>();
        for (int threads = 1; threads <= 16; threads++) {
            List<Long> times = allData.stream()
                .map(data -> data.results.get(threads))
                .filter(Objects::nonNull)
                .map(result -> result.timeMs)
                .collect(Collectors.toList());
            
            if (!times.isEmpty()) {
                double avg = times.stream().mapToLong(Long::longValue).average().orElse(0.0);
                averages.put(threads, avg);
            }
        }
        
        if (!averages.isEmpty()) {
            int optimalThreads = averages.entrySet().stream()
                .min(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(1);
            
            double minTime = averages.get(optimalThreads);
            double baselineTime = averages.get(1);
            double improvement = baselineTime / minTime;
            
            summary.append("CONCLUSIONES PRINCIPALES:\n");
            summary.append("- Configuración óptima: ").append(optimalThreads).append(" hilos\n");
            summary.append("- Tiempo promedio óptimo: ").append(String.format("%.0f ms", minTime)).append("\n");
            summary.append("- Mejora sobre 1 hilo: ").append(String.format("%.2fx", improvement)).append("\n");
            summary.append("- Eficiencia por hilo: ").append(String.format("%.1f%%", (improvement / optimalThreads) * 100)).append("\n");
        }
        
        String fileName = "summary_report_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".txt";
        FileManager.saveReport(summary.toString(), fileName);
        
        System.out.println("\n=== Reportes generados ===");
        System.out.println("- Tabla comparativa");
        System.out.println("- Gráfico de barras");
        System.out.println("- Análisis de regresión");
        System.out.println("- Resumen ejecutivo");
    }
    
    private static double calculateStandardDeviation(List<Long> values, double mean) {
        double sum = 0.0;
        for (Long value : values) {
            sum += Math.pow(value - mean, 2);
        }
        return Math.sqrt(sum / values.size());
    }
    
    private static LinearRegression calculateLinearRegression(List<Double> xValues, List<Double> yValues) {
        int n = xValues.size();
        double sumX = xValues.stream().mapToDouble(Double::doubleValue).sum();
        double sumY = yValues.stream().mapToDouble(Double::doubleValue).sum();
        double sumXY = 0.0;
        double sumXX = 0.0;
        double sumYY = 0.0;
        
        for (int i = 0; i < n; i++) {
            double x = xValues.get(i);
            double y = yValues.get(i);
            sumXY += x * y;
            sumXX += x * x;
            sumYY += y * y;
        }
        
        double slope = (n * sumXY - sumX * sumY) / (n * sumXX - sumX * sumX);
        double intercept = (sumY - slope * sumX) / n;
        
        double correlation = (n * sumXY - sumX * sumY) / 
            Math.sqrt((n * sumXX - sumX * sumX) * (n * sumYY - sumY * sumY));
        
        return new LinearRegression(slope, intercept, correlation);
    }
}