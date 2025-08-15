import javax.swing.*;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

public class ExpenseTrackerPro {

    private static final String FILE_NAME = "expenses.dat";
    private final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    // Data Models
    private List<Expense> expenses;
    private List<String> categories;

    // UI Components
    private JFrame frame;
    private JTabbedPane tabbedPane;
    private JMenuItem exportMenuItem;

    // Expense Panel Components
    private DefaultTableModel tableModel;
    private JTable expenseTable;
    private JTextField amountField;
    private JComboBox<String> categoryComboBox;
    private JTextField dateField;
    private JTextArea noteArea;
    private JButton mainActionButton;
    private JButton editCancelButton;
    private JButton deleteButton;
    private JTextField searchField;
    private TableRowSorter<DefaultTableModel> sorter;
    private int editingIndex = -1;

    // Reports Panel Components
    private JTextArea summaryArea;
    private JTextField startDateField;
    private JTextField endDateField;
    private JPanel chartPanel;
    private LocalDate reportStartDate;
    private LocalDate reportEndDate;
    private Map<String, Double> currentCategorySummary;

    // Settings Panel Components
    private DefaultListModel<String> categoryListModel;
    private JList<String> categoryList;
    private JTextField newCategoryField;
    private JButton addCategoryButton;
    private JButton removeCategoryButton;

    public ExpenseTrackerPro() {
        this.expenses = new ArrayList<>();
        this.categories = new ArrayList<>();
        loadData();
        initializeGUI();
    }

    private void initializeGUI() {
        frame = new JFrame("Professional Expense Tracker");
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.setSize(1200, 800);
        frame.setLocationRelativeTo(null);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                saveData();
                frame.dispose();
            }
        });

        createMenuBar();

        tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Manage Expenses", createExpensePanel());
        tabbedPane.addTab("Reports", createReportsPanel());
        tabbedPane.addTab("Settings", createSettingsPanel());

        frame.add(tabbedPane);
        frame.setVisible(true);
    }

    private void createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        JMenu helpMenu = new JMenu("Help");

        exportMenuItem = new JMenuItem("Export to CSV");
        exportMenuItem.addActionListener(e -> exportToCsv());
        fileMenu.add(exportMenuItem);

        JMenuItem aboutMenuItem = new JMenuItem("About");
        aboutMenuItem.addActionListener(
                e -> JOptionPane.showMessageDialog(frame, "Professional Expense Tracker v1.0\nCreated with Java Swing",
                        "About", JOptionPane.INFORMATION_MESSAGE));
        helpMenu.add(aboutMenuItem);

        menuBar.add(fileMenu);
        menuBar.add(helpMenu);

        frame.setJMenuBar(menuBar);
    }

    private JPanel createExpensePanel() {
        JPanel mainPanel = new JPanel(new BorderLayout(15, 15));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JPanel controlsPanel = new JPanel(new BorderLayout(10, 10));
        controlsPanel.setBorder(BorderFactory.createTitledBorder("Add/Edit Expense"));

        JPanel inputPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        amountField = new JTextField(15);
        categoryComboBox = new JComboBox<>(new Vector<>(categories));
        dateField = new JTextField(LocalDate.now().format(DATE_FORMATTER), 15);
        noteArea = new JTextArea(3, 20);
        noteArea.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        JScrollPane noteScrollPane = new JScrollPane(noteArea);

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        inputPanel.add(new JLabel("Amount:"), gbc);
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 1;
        inputPanel.add(amountField, gbc);
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0;
        inputPanel.add(new JLabel("Category:"), gbc);
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.weightx = 1;
        inputPanel.add(categoryComboBox, gbc);
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0;
        inputPanel.add(new JLabel("Date (yyyy-MM-dd):"), gbc);
        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.weightx = 1;
        inputPanel.add(dateField, gbc);
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.weightx = 0;
        inputPanel.add(new JLabel("Note:"), gbc);
        gbc.gridx = 1;
        gbc.gridy = 3;
        gbc.weightx = 1;
        inputPanel.add(noteScrollPane, gbc);

        JPanel buttonPanel = new JPanel();
        mainActionButton = new JButton("Add Expense");
        editCancelButton = new JButton("Cancel Edit");
        deleteButton = new JButton("Delete Selected");
        editCancelButton.setVisible(false);

        buttonPanel.add(mainActionButton);
        buttonPanel.add(editCancelButton);
        buttonPanel.add(deleteButton);

        controlsPanel.add(inputPanel, BorderLayout.CENTER);
        controlsPanel.add(buttonPanel, BorderLayout.SOUTH);
        mainPanel.add(controlsPanel, BorderLayout.NORTH);

        JPanel tablePanel = new JPanel(new BorderLayout(5, 5));

        JPanel searchPanel = new JPanel(new BorderLayout());
        searchField = new JTextField();
        searchPanel.add(new JLabel("Search: "), BorderLayout.WEST);
        searchPanel.add(searchField, BorderLayout.CENTER);
        tablePanel.add(searchPanel, BorderLayout.NORTH);

        String[] columnNames = { "Amount", "Category", "Date", "Note" };
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return switch (columnIndex) {
                    case 0 -> Double.class;
                    case 1, 2, 3 -> String.class;
                    default -> super.getColumnClass(columnIndex);
                };
            }
        };
        expenseTable = new JTable(tableModel);

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        expenseTable.getTableHeader().setDefaultRenderer(centerRenderer);

        sorter = new TableRowSorter<>(tableModel);
        expenseTable.setRowSorter(sorter);
        expenseTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane tableScrollPane = new JScrollPane(expenseTable);
        tablePanel.add(tableScrollPane, BorderLayout.CENTER);
        mainPanel.add(tablePanel, BorderLayout.CENTER);

        mainActionButton.addActionListener(e -> handleMainAction());
        editCancelButton.addActionListener(e -> cancelEdit());
        deleteButton.addActionListener(e -> deleteExpense());

        expenseTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && expenseTable.getSelectedRow() != -1 && editingIndex == -1) {
                setEditMode(expenseTable.getSelectedRow());
            }
        });

        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                filterTable();
            }

            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                filterTable();
            }

            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                filterTable();
            }
        });

        updateExpenseTable();

        return mainPanel;
    }

    private void handleMainAction() {
        if (editingIndex != -1) {
            updateExpense();
        } else {
            addExpense();
        }
    }

    private void setEditMode(int viewRow) {
        editingIndex = expenseTable.convertRowIndexToModel(viewRow);

        Expense expense = expenses.get(editingIndex);
        amountField.setText(String.valueOf(expense.getAmount()));
        categoryComboBox.setSelectedItem(expense.getCategory());
        dateField.setText(expense.getDate().format(DATE_FORMATTER));
        noteArea.setText(expense.getNote());

        mainActionButton.setText("Update Expense");
        editCancelButton.setVisible(true);
        // FIX: The delete button should remain enabled even in edit mode.
        deleteButton.setEnabled(true);
    }

    private void cancelEdit() {
        editingIndex = -1;
        mainActionButton.setText("Add Expense");
        editCancelButton.setVisible(false);
        // FIX: Re-enabling the delete button for consistency
        deleteButton.setEnabled(true);
        clearInputFields();
        expenseTable.clearSelection();
    }

    private void addExpense() {
        if (categories.isEmpty()) {
            JOptionPane.showMessageDialog(frame,
                    "Please add at least one category in the 'Settings' tab before adding an expense.",
                    "No Categories Found", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            double amount = Double.parseDouble(amountField.getText());
            if (amount <= 0) {
                JOptionPane.showMessageDialog(frame, "Amount must be a positive number.", "Invalid Input",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
            String category = (String) categoryComboBox.getSelectedItem();
            if (category == null || category.isEmpty()) {
                JOptionPane.showMessageDialog(frame, "Category cannot be empty.", "Invalid Input",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
            LocalDate date = parseDate(dateField.getText());
            String note = noteArea.getText();

            expenses.add(new Expense(amount, category, date, note));

            updateUI();
            clearInputFields();
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(frame, "Invalid amount. Please enter a number.", "Invalid Input",
                    JOptionPane.ERROR_MESSAGE);
        } catch (DateTimeParseException e) {
            JOptionPane.showMessageDialog(frame, "Invalid date format. Please use yyyy-MM-dd.", "Invalid Date",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateExpense() {
        try {
            double amount = Double.parseDouble(amountField.getText());
            if (amount <= 0) {
                JOptionPane.showMessageDialog(frame, "Amount must be a positive number.", "Invalid Input",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
            String category = (String) categoryComboBox.getSelectedItem();
            LocalDate date = parseDate(dateField.getText());
            String note = noteArea.getText();

            Expense expenseToUpdate = expenses.get(editingIndex);
            expenseToUpdate.setAmount(amount);
            expenseToUpdate.setCategory(category);
            expenseToUpdate.setDate(date);
            expenseToUpdate.setNote(note);

            updateUI();
            cancelEdit();

        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(frame, "Invalid amount. Please enter a number.", "Invalid Input",
                    JOptionPane.ERROR_MESSAGE);
        } catch (DateTimeParseException e) {
            JOptionPane.showMessageDialog(frame, "Invalid date format. Please use yyyy-MM-dd.", "Invalid Date",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteExpense() {
        int selectedRow = expenseTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(frame, "Please select an expense to delete.", "No Selection",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(frame, "Are you sure you want to delete this expense?",
                "Confirm Deletion", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            int modelIndex = expenseTable.convertRowIndexToModel(selectedRow);
            expenses.remove(modelIndex);
            updateUI();
            JOptionPane.showMessageDialog(frame, "Expense deleted successfully.", "Deleted",
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void filterTable() {
        try {
            RowFilter<DefaultTableModel, Object> rf = RowFilter.regexFilter("(?i)" + searchField.getText(), 1, 2, 3);
            sorter.setRowFilter(rf);
        } catch (PatternSyntaxException e) {
            // Do nothing on bad regex
        }
    }

    private JPanel createReportsPanel() {
        JPanel reportsPanel = new JPanel(new BorderLayout(15, 15));
        reportsPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JPanel filterPanel = new JPanel();
        filterPanel.add(new JLabel("Start Date:"));
        startDateField = new JTextField(LocalDate.now().minusMonths(1).format(DATE_FORMATTER), 10);
        filterPanel.add(startDateField);
        filterPanel.add(new JLabel("End Date:"));
        endDateField = new JTextField(LocalDate.now().format(DATE_FORMATTER), 10);
        filterPanel.add(endDateField);
        JButton filterButton = new JButton("Filter");
        filterButton.addActionListener(e -> updateReports());
        filterPanel.add(filterButton);
        reportsPanel.add(filterPanel, BorderLayout.NORTH);

        JPanel contentPanel = new JPanel(new GridLayout(1, 2, 15, 15));

        summaryArea = new JTextArea();
        summaryArea.setEditable(false);
        summaryArea.setBorder(BorderFactory.createTitledBorder("Summary"));
        JScrollPane summaryScrollPane = new JScrollPane(summaryArea);
        contentPanel.add(summaryScrollPane);

        chartPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                drawPieChart((Graphics2D) g);
            }
        };
        chartPanel.setBorder(BorderFactory.createTitledBorder("Category Breakdown"));
        chartPanel.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                showPieChartTooltip(e.getPoint());
            }
        });
        contentPanel.add(chartPanel);

        reportsPanel.add(contentPanel, BorderLayout.CENTER);

        updateReports();

        return reportsPanel;
    }

    private void updateReports() {
        try {
            reportStartDate = parseDate(startDateField.getText());
            reportEndDate = parseDate(endDateField.getText());

            List<Expense> filteredExpenses = expenses.stream()
                    .filter(e -> !e.getDate().isBefore(reportStartDate) && !e.getDate().isAfter(reportEndDate))
                    .collect(Collectors.toList());

            updateSummary(filteredExpenses);

            currentCategorySummary = filteredExpenses.stream()
                    .collect(Collectors.groupingBy(Expense::getCategory, Collectors.summingDouble(Expense::getAmount)));
            chartPanel.repaint();

        } catch (DateTimeParseException e) {
            JOptionPane.showMessageDialog(frame, "Invalid date format. Please use yyyy-MM-dd.", "Invalid Date",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateSummary(List<Expense> filteredExpenses) {
        if (filteredExpenses.isEmpty()) {
            summaryArea.setText("No expenses found for the selected date range.");
            return;
        }
        double total = filteredExpenses.stream().mapToDouble(Expense::getAmount).sum();

        StringBuilder summaryText = new StringBuilder();
        summaryText.append(String.format("--- Summary for %s to %s ---\n", reportStartDate, reportEndDate));
        summaryText.append(String.format("Total Expenses: $%.2f\n\n", total));
        summaryArea.setText(summaryText.toString());
    }

    private void drawPieChart(Graphics2D g) {
        if (currentCategorySummary == null || currentCategorySummary.isEmpty()) {
            g.setColor(Color.BLACK);
            g.drawString("No data for this period", 10, 20);
            return;
        }

        double total = currentCategorySummary.values().stream().mapToDouble(Double::doubleValue).sum();

        int width = chartPanel.getWidth();
        int height = chartPanel.getHeight();
        int size = Math.min(width, height) - 40;
        int x = (width - size) / 2;
        int y = (height - size) / 2;

        int startAngle = 0;
        int colorIndex = 0;
        Color[] colors = { Color.RED, Color.BLUE, Color.GREEN, Color.YELLOW, Color.CYAN, Color.MAGENTA, Color.ORANGE,
                Color.PINK };

        for (Map.Entry<String, Double> entry : currentCategorySummary.entrySet()) {
            int arcAngle = (int) Math.round((entry.getValue() / total) * 360);
            g.setColor(colors[colorIndex % colors.length]);
            g.fillArc(x, y, size, size, startAngle, arcAngle);

            g.fillRect(width - 200, 20 + colorIndex * 20, 10, 10);
            g.setColor(Color.BLACK);
            g.drawString(entry.getKey() + " (" + String.format("%.2f%%", (entry.getValue() / total) * 100) + ")",
                    width - 180, 30 + colorIndex * 20);

            startAngle += arcAngle;
            colorIndex++;
        }
    }

    private void showPieChartTooltip(Point mousePoint) {
        if (currentCategorySummary == null || currentCategorySummary.isEmpty())
            return;

        double total = currentCategorySummary.values().stream().mapToDouble(Double::doubleValue).sum();

        int width = chartPanel.getWidth();
        int height = chartPanel.getHeight();
        int size = Math.min(width, height) - 40;
        int x = (width - size) / 2;
        int y = (height - size) / 2;

        int dx = mousePoint.x - (x + size / 2);
        int dy = mousePoint.y - (y + size / 2);
        double distance = Math.sqrt(dx * dx + dy * dy);

        if (distance > size / 2) {
            chartPanel.setToolTipText(null);
            return;
        }

        double angle = Math.toDegrees(Math.atan2(dy, dx));
        if (angle < 0)
            angle += 360;

        int startAngle = 0;
        for (Map.Entry<String, Double> entry : currentCategorySummary.entrySet()) {
            int arcAngle = (int) Math.round((entry.getValue() / total) * 360);
            if (angle >= startAngle && angle < startAngle + arcAngle) {
                chartPanel.setToolTipText(String.format("<html><b>%s:</b> $%.2f (%.2f%%)</html>",
                        entry.getKey(), entry.getValue(), (entry.getValue() / total) * 100));
                return;
            }
            startAngle += arcAngle;
        }
        chartPanel.setToolTipText(null);
    }

    private JPanel createSettingsPanel() {
        JPanel settingsPanel = new JPanel(new GridLayout(1, 2, 20, 20));
        settingsPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JPanel categoryPanel = new JPanel(new BorderLayout(10, 10));
        categoryPanel.setBorder(BorderFactory.createTitledBorder("Manage Categories"));

        categoryListModel = new DefaultListModel<>();
        categories.forEach(categoryListModel::addElement);
        categoryList = new JList<>(categoryListModel);
        categoryList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane categoryScrollPane = new JScrollPane(categoryList);

        JPanel categoryControlPanel = new JPanel();
        newCategoryField = new JTextField(15);
        addCategoryButton = new JButton("Add");
        removeCategoryButton = new JButton("Remove");

        categoryControlPanel.add(newCategoryField);
        categoryControlPanel.add(addCategoryButton);
        categoryControlPanel.add(removeCategoryButton);

        categoryPanel.add(categoryScrollPane, BorderLayout.CENTER);
        categoryPanel.add(categoryControlPanel, BorderLayout.SOUTH);

        settingsPanel.add(categoryPanel);

        addCategoryButton.addActionListener(e -> addCategory());
        removeCategoryButton.addActionListener(e -> removeCategory());

        return settingsPanel;
    }

    private void addCategory() {
        String newCategory = newCategoryField.getText().trim();
        if (!newCategory.isEmpty() && !categories.contains(newCategory)) {
            categories.add(newCategory);
            categoryListModel.addElement(newCategory);
            categoryComboBox.addItem(newCategory);
            newCategoryField.setText("");
        } else {
            JOptionPane.showMessageDialog(frame, "Category is either empty or already exists.", "Invalid Input",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void removeCategory() {
        String selectedCategory = categoryList.getSelectedValue();
        if (selectedCategory != null) {
            categories.remove(selectedCategory);
            categoryListModel.removeElement(selectedCategory);
            categoryComboBox.removeItem(selectedCategory);

            for (Expense e : expenses) {
                if (e.getCategory().equals(selectedCategory)) {
                    e.setCategory("Uncategorized");
                }
            }
            if (!categories.contains("Uncategorized")) {
                categories.add("Uncategorized");
                categoryListModel.addElement("Uncategorized");
                categoryComboBox.addItem("Uncategorized");
            }
            updateUI();

            JOptionPane.showMessageDialog(frame,
                    "Category removed. Expenses from this category have been moved to 'Uncategorized'.", "Success",
                    JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(frame, "Please select a category to remove.", "No Selection",
                    JOptionPane.WARNING_MESSAGE);
        }
    }

    private void exportToCsv() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Export Expenses to CSV");
        fileChooser.setFileFilter(new FileNameExtensionFilter("CSV files (*.csv)", "csv"));

        int userSelection = fileChooser.showSaveDialog(frame);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            if (!fileToSave.getName().toLowerCase().endsWith(".csv")) {
                fileToSave = new File(fileToSave.getAbsolutePath() + ".csv");
            }
            try (FileWriter writer = new FileWriter(fileToSave)) {
                writer.append("Amount,Category,Date,Note\n");
                for (Expense expense : expenses) {
                    writer.append(String.format("%.2f,%s,%s,\"%s\"\n",
                            expense.getAmount(), expense.getCategory(), expense.getDate(),
                            expense.getNote().replace("\"", "\"\"")));
                }
                JOptionPane.showMessageDialog(frame, "Expenses exported successfully!", "Export Complete",
                        JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(frame, "Error exporting file: " + ex.getMessage(), "Export Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void saveData() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(FILE_NAME))) {
            oos.writeObject(expenses);
            oos.writeObject(categories);
            System.out.println("Data saved to " + FILE_NAME);
        } catch (IOException e) {
            System.out.println("Error saving data: " + e.getMessage());
        }
    }

    private void loadData() {
        File file = new File(FILE_NAME);
        if (file.exists()) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
                expenses = (List<Expense>) ois.readObject();
                categories = (List<String>) ois.readObject();
                System.out.println("Data loaded from " + FILE_NAME);
            } catch (IOException | ClassNotFoundException e) {
                System.out.println("Error loading data: " + e.getMessage());
            }
        } else {
            System.out.println("No existing data file found. Starting with an empty list.");
        }
    }

    private void updateUI() {
        updateExpenseTable();
        updateReports();
        if (categories.isEmpty() && tabbedPane.getSelectedIndex() != 2) {
            JOptionPane.showMessageDialog(frame, "Please add at least one category in the 'Settings' tab.",
                    "No Categories Found", JOptionPane.WARNING_MESSAGE);
            tabbedPane.setSelectedIndex(2);
        }
    }

    private void updateExpenseTable() {
        tableModel.setRowCount(0);
        for (Expense expense : expenses) {
            tableModel.addRow(new Object[] {
                    expense.getAmount(),
                    expense.getCategory(),
                    expense.getDate().toString(),
                    expense.getNote()
            });
        }
        filterTable();
    }

    private void clearInputFields() {
        amountField.setText("");
        if (!categories.isEmpty()) {
            categoryComboBox.setSelectedIndex(0);
        } else {
            categoryComboBox.setSelectedItem(null);
        }
        dateField.setText(LocalDate.now().format(DATE_FORMATTER));
        noteArea.setText("");
    }

    private LocalDate parseDate(String dateString) throws DateTimeParseException {
        return LocalDate.parse(dateString.trim(), DATE_FORMATTER);
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        SwingUtilities.invokeLater(ExpenseTrackerPro::new);
    }
}