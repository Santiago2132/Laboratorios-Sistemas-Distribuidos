import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

public class GUIInterface extends JFrame {
    private JTextArea urlArea;
    private JTextField outputDirField;
    private JTextField chromePathField;
    private JSpinner threadSpinner;
    private JTextArea resultArea;
    private JButton convertButton;
    private JProgressBar progressBar;
    
    public GUIInterface() {
        setTitle("Web to PDF Converter");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(600, 500);
        setLocationRelativeTo(null);
        
        initComponents();
        layoutComponents();
    }
    
    private void initComponents() {
        urlArea = new JTextArea(5, 40);
        urlArea.setBorder(BorderFactory.createTitledBorder("URLs (una por línea)"));
        
        outputDirField = new JTextField("./pdfs", 20);
        chromePathField = new JTextField("/usr/bin/google-chrome", 20);
        threadSpinner = new JSpinner(new SpinnerNumberModel(4, 1, 32, 1));
        
        resultArea = new JTextArea(10, 40);
        resultArea.setEditable(false);
        resultArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        
        convertButton = new JButton("Convertir");
        convertButton.addActionListener(this::convertAction);
        
        progressBar = new JProgressBar();
        progressBar.setStringPainted(true);
    }
    
    private void layoutComponents() {
        setLayout(new BorderLayout());
        
        JPanel configPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        
        gbc.gridx = 0; gbc.gridy = 0;
        configPanel.add(new JLabel("Directorio salida:"), gbc);
        gbc.gridx = 1;
        configPanel.add(outputDirField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 1;
        configPanel.add(new JLabel("Ruta Chrome:"), gbc);
        gbc.gridx = 1;
        configPanel.add(chromePathField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 2;
        configPanel.add(new JLabel("Hilos:"), gbc);
        gbc.gridx = 1;
        configPanel.add(threadSpinner, gbc);
        
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(new JScrollPane(urlArea), BorderLayout.CENTER);
        topPanel.add(configPanel, BorderLayout.SOUTH);
        
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(new JScrollPane(resultArea), BorderLayout.CENTER);
        
        JPanel controlPanel = new JPanel(new FlowLayout());
        controlPanel.add(convertButton);
        controlPanel.add(progressBar);
        
        add(topPanel, BorderLayout.NORTH);
        add(bottomPanel, BorderLayout.CENTER);
        add(controlPanel, BorderLayout.SOUTH);
    }
    
    private void convertAction(ActionEvent e) {
        String[] lines = urlArea.getText().trim().split("\n");
        List<String> urls = new ArrayList<>();
        
        for (String line : lines) {
            line = line.trim();
            if (!line.isEmpty()) {
                if (!line.startsWith("http")) {
                    line = "https://" + line;
                }
                urls.add(line);
            }
        }
        
        if (urls.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Ingrese al menos una URL", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        convertButton.setEnabled(false);
        progressBar.setIndeterminate(true);
        resultArea.setText("Procesando...\n");
        
        SwingWorker<WebToPDFConverter.ConversionResult, String> worker = new SwingWorker<>() {
            @Override
            protected WebToPDFConverter.ConversionResult doInBackground() {
                WebToPDFConverter converter = new WebToPDFConverter(
                    outputDirField.getText(),
                    chromePathField.getText()
                );
                return converter.convertUrls(urls, (Integer) threadSpinner.getValue());
            }
            
            @Override
            protected void done() {
                try {
                    WebToPDFConverter.ConversionResult result = get();
                    displayResult(result);
                } catch (Exception ex) {
                    resultArea.setText("Error: " + ex.getMessage());
                }
                
                convertButton.setEnabled(true);
                progressBar.setIndeterminate(false);
            }
        };
        
        worker.execute();
    }
    
    private void displayResult(WebToPDFConverter.ConversionResult result) {
        StringBuilder sb = new StringBuilder();
        sb.append(result).append("\n\n");
        
        if (!result.successfulPdfs.isEmpty()) {
            sb.append("PDFs creados:\n");
            result.successfulPdfs.forEach(path -> sb.append("  ").append(path).append("\n"));
        }
        
        if (!result.errors.isEmpty()) {
            sb.append("\nErrores:\n");
            result.errors.forEach(error -> sb.append("  ").append(error).append("\n"));
        }
        
        resultArea.setText(sb.toString());
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getLookAndFeel());
            } catch (Exception e) {
                // Usar look and feel por defecto
            }
            new GUIInterface().setVisible(true);
        });
    }
}