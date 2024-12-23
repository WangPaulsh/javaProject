import javax.swing.*;
import javax.swing.border.Border;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class SleepTrackerApp {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(SleepTrackerApp::createAndShowGUI);
    }

    private static void createAndShowGUI() {
        JFrame frame = new JFrame("Sleep Tracker");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 800);
        frame.setLayout(new BorderLayout());

        SleepTracker tracker = new SleepTracker();

        JPanel inputPanel = createInputPanel(tracker);
        JPanel chartPanel = createChartPanel(tracker);

        frame.add(inputPanel, BorderLayout.NORTH);
        frame.add(chartPanel, BorderLayout.CENTER);

        frame.setVisible(true);
    }

    private static JPanel createInputPanel(SleepTracker tracker) {
        JPanel panel = new JPanel(new GridLayout(3, 1));

        JPanel fieldPanel = new JPanel();
        // Input for required sleep time
        JPanel requiredSleepPanel = new JPanel();
        requiredSleepPanel.add(new JLabel("Required Sleep Hours:"));
        JTextField requiredSleepField = new JTextField(6);
        requiredSleepPanel.add(requiredSleepField);
        fieldPanel.add(requiredSleepPanel);

        String requireCompensationdDays = "0";
        JPanel lostSleepPanel = new JPanel(new GridLayout(3, 2));
        lostSleepPanel.add(new JLabel("Enter lost sleep hours:"));
        JTextField lostSleepField = new JTextField(1);
        lostSleepPanel.add(lostSleepField);
        lostSleepPanel.add(new JLabel("Required compensation sleep days: "));
        JLabel requireCompensationdLabel = new JLabel(requireCompensationdDays);
        lostSleepPanel.add(requireCompensationdLabel);       
        lostSleepPanel.add(new JLabel("Sleep Status:"));
        JPanel lightPanel = new JPanel();
        lightPanel.setPreferredSize(new Dimension(50, 50));
        lightPanel.setBackground(Color.GRAY); // 初始顏色
        lostSleepPanel.add(lightPanel);
        fieldPanel.add(lostSleepPanel);
        panel.add(fieldPanel);

        // Input for daily sleep hours
        JPanel dailySleepPanel = new JPanel(new GridLayout(9, 2));
        
        dailySleepPanel.add(new JLabel("Enter daily sleep hours:"));
        dailySleepPanel.add(new JLabel()); // 控制板型
        
        JTextField[] dailyFields = new JTextField[7];
        String[] days = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};

        for (int i = 0; i < 7; i++) {
            dailySleepPanel.add(new JLabel(days[i] + ":"));
            dailyFields[i] = new JTextField(5);
            dailySleepPanel.add(dailyFields[i]);
        }
        panel.add(dailySleepPanel);

        // Buttons for actions
        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton calculateButton = new JButton("Calculate");
        JButton saveButton = new JButton("Save");
        JButton loadButton = new JButton("Load");
        JButton resetButton = new JButton("Reset");

        buttonPanel.add(calculateButton);
        buttonPanel.add(saveButton);
        buttonPanel.add(loadButton);
        buttonPanel.add(resetButton);
        panel.add(buttonPanel);

        // Action listeners
        calculateButton.addActionListener(e -> {
            try {
                int requiredSleep = Integer.parseInt(requiredSleepField.getText());
                int lostSleep = Integer.parseInt(lostSleepField.getText());
                int lightColor = lightPanel.getBackground().getRGB();
                int[] dailySleep = new int[7];
                for (int i = 0; i < 7; i++) {
                    dailySleep[i] = Integer.parseInt(dailyFields[i].getText());
                }
                tracker.setSleepData(requiredSleep, lostSleep, lightColor, dailySleep);
                int totalLoseSleep = tracker.calculateDeficits();               
                requireCompensationdLabel.setText(Integer.toString((totalLoseSleep += lostSleep) * 2));

                int totalSleep = 0;
                for (int sleep : dailySleep) {
                    totalSleep += sleep;
                }
                int averageSleep = totalSleep / dailySleep.length;

                if (averageSleep < requiredSleep / 2) {
                    lightPanel.setBackground(Color.RED); // 小於一半顯示紅燈
                } else if (averageSleep < requiredSleep) {
                    lightPanel.setBackground(Color.ORANGE); // 介於一半與足夠顯示橘燈
                } else {
                    lightPanel.setBackground(Color.GREEN); // 足夠顯示綠燈
                }

                lightColor = lightPanel.getBackground().getRGB();
                tracker.setSleepData(requiredSleep, lostSleep, lightColor, dailySleep);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(panel, "Please enter valid numeric values.", "Input Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        saveButton.addActionListener(e -> {
            String fileName = JOptionPane.showInputDialog(panel, "Enter file name to save:", "Save File", JOptionPane.PLAIN_MESSAGE);
            if (fileName != null && !fileName.trim().isEmpty()) {
                tracker.saveDataToFile(fileName.trim() + ".txt");
            }
        });

        loadButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            int result = fileChooser.showOpenDialog(panel);
            if (result == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                tracker.loadDataFromFile(file, dailyFields, requiredSleepField, lostSleepField, lightPanel, requireCompensationdLabel);
            }
        });

        resetButton.addActionListener(e -> {
            requiredSleepField.setText(null);
            lostSleepField.setText(null);
            lightPanel.setBackground(Color.GRAY);
            requireCompensationdLabel.setText("0");
            for (int i = 0; i < 7; i++) {
                dailyFields[i].setText(null);
            }
            tracker.clearChartPanel();
        });

        return panel;
    }

    private static JPanel createChartPanel(SleepTracker tracker) {
        return tracker.getChartPanel();
    }
}

class SleepTracker {
    private int requiredSleep;
    private int lostSleep;
    private int lightColor;
    private int requireCompensationdDays;
    private int[] dailySleep = new int[7];
    private List<Integer> deficits = new ArrayList<>();
    private JPanel chartPanel = new JPanel();

    public void setSleepData(int requiredSleep, int lostSleep, int lightColor, int[] dailySleep) {
        this.requiredSleep = requiredSleep;
        this.lostSleep = lostSleep;
        this.lightColor = lightColor;
        System.arraycopy(dailySleep, 0, this.dailySleep, 0, dailySleep.length);
        updateChart();
    }

    public int calculateDeficits() {
        deficits.clear();
        int totalLoseSleep = 0;
        for (int sleep : dailySleep) {
            int deficit = Math.max(0, requiredSleep - sleep);
            totalLoseSleep += deficit;
            deficits.add(deficit);
        }
        updateChart();
        return totalLoseSleep;
    }

    public JPanel getChartPanel() {
        chartPanel.setLayout(new BorderLayout());
        updateChart();
        return chartPanel;
    }

    public void clearChartPanel() {
        chartPanel.removeAll();
        
        JPanel barPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);

                // Draw horizontal line for required sleep
                int chartHeight = getHeight() - 40; // Reserve space for labels
                int requiredY = chartHeight;// - (requiredSleep * chartHeight / 24); // Adjust based on 24-hour scale
                g.setColor(Color.BLUE);
                g.drawLine(0, requiredY , getWidth(), requiredY );

                g.setColor(Color.BLACK);
                g.drawString("Required: 0 hrs", 5, requiredY - 15);
            }
        };

        int barWidth = 50;
        int spacing = 70;
        int chartHeight = 125;

        for (int i = 0; i < dailySleep.length; i++) {
            int barHeight = 0;

            JPanel bar = new JPanel();
            bar.setBackground(dailySleep[i] >= requiredSleep ? Color.GREEN : Color.RED);
            bar.setBounds(i * (barWidth + spacing)+5, chartHeight - barHeight, barWidth, barHeight);
            barPanel.add(bar);

            JLabel dayLabel = new JLabel("Day " + (i + 1));
            dayLabel.setHorizontalAlignment(SwingConstants.CENTER);
            dayLabel.setBounds(i * (barWidth + spacing)+5, chartHeight + 5, barWidth, 20);
            barPanel.add(dayLabel);
        }

        barPanel.setLayout(null); // Use absolute layout for spacing
        chartPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        chartPanel.add(barPanel, BorderLayout.CENTER);
        chartPanel.revalidate();
        chartPanel.repaint();
    }

    // 更新圖表
    private void updateChart() {
        chartPanel.removeAll();

        // Create the bar chart panel with custom painting for the horizontal line
        JPanel barPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);

                // Draw horizontal line for required sleep
                int chartHeight = getHeight() - 40; // Reserve space for labels
                int requiredY = chartHeight - (requiredSleep * chartHeight / 24); // Adjust based on 24-hour scale
                g.setColor(Color.BLUE);
                g.drawLine(0, requiredY , getWidth(), requiredY );

                g.setColor(Color.BLACK);
                g.drawString("Required: " + requiredSleep + " hrs", 5, requiredY - 15);
            }
        };
        barPanel.setLayout(null); // Use absolute layout for spacing
        chartPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK));

        int barWidth = 50;
        int spacing = 70;
        int chartHeight = 125;

        for (int i = 0; i < dailySleep.length; i++) {
            int barHeight = dailySleep[i] * chartHeight / 24;

            JPanel bar = new JPanel();
            bar.setBackground(dailySleep[i] >= requiredSleep ? Color.GREEN : Color.RED);
            bar.setBounds(i * (barWidth + spacing)+5, chartHeight - barHeight, barWidth, barHeight);
            barPanel.add(bar);

            JLabel dayLabel = new JLabel("Day " + (i + 1));
            dayLabel.setHorizontalAlignment(SwingConstants.CENTER);
            dayLabel.setBounds(i * (barWidth + spacing)+5, chartHeight + 5, barWidth, 20);
            barPanel.add(dayLabel);
        }

        barPanel.setPreferredSize(new Dimension((barWidth + spacing) * dailySleep.length, chartHeight + 40));
        chartPanel.add(barPanel, BorderLayout.CENTER);
        chartPanel.revalidate();
        chartPanel.repaint();
    }

    // 儲存資料
    public void saveDataToFile(String fileName) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
            writer.write(requiredSleep + "\n");
            writer.write(lostSleep + "\n");
            writer.write(lightColor + "\n");
            writer.write(requireCompensationdDays + "\n");
            for (int sleep : dailySleep) {
                writer.write(sleep + "\n");
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(chartPanel, "Error saving data to file.", "File Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // 載入資料
    public void loadDataFromFile(File file, JTextField[] dailyFields, JTextField requiredField, JTextField lostField, JPanel lightPanel, JLabel requireCompensationdLabel) {
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            requiredSleep = Integer.parseInt(reader.readLine());
            requiredField.setText(String.valueOf(requiredSleep));
            lostSleep = Integer.parseInt(reader.readLine());
            lostField.setText(String.valueOf(lostSleep));
            lightColor = Integer.parseInt(reader.readLine());
            lightPanel.setBackground(new Color(lightColor));
            requireCompensationdDays = Integer.parseInt(reader.readLine());
            requireCompensationdLabel.setText(String.valueOf(requireCompensationdDays));
            for (int i = 0; i < 7; i++) {
                dailySleep[i] = Integer.parseInt(reader.readLine());
                dailyFields[i].setText(String.valueOf(dailySleep[i]));
            }
            updateChart();
        } catch (IOException | NumberFormatException e) {
            JOptionPane.showMessageDialog(chartPanel, "Error loading data from file.", "File Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
