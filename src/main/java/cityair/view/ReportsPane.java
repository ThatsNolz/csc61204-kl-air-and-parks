package cityair.view;

import cityair.model.CitizenReport;
import cityair.model.CitizenReport.Issue;
import cityair.model.District;
import java.awt.*;
import java.util.List;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import static cityair.view.Theme.*;

/** Citizen reports screen: a validated entry form and a sortable, filterable local history. */
final class ReportsPane extends JPanel {
    static final String ALL_AREAS = "All areas";
    private static final int FORM_WIDTH = 360, DISTRICT_COLUMN = 1, DESCRIPTION_ROWS = 6;
    private static final int[] WIDTHS = {150, 110, 80, 420};
    private final JComboBox<District> district = named(new JComboBox<>(District.values()), "reportDistrict");
    private final JComboBox<Issue> issue = named(new JComboBox<>(Issue.values()), "issue");
    private final JTextArea description = named(new JTextArea(DESCRIPTION_ROWS, 0), "description");
    private final JLabel counter = label("", SMALL, Font.PLAIN, MUTED);
    private final JButton save = named(new JButton("Save local report"), "save");
    private final JComboBox<Object> filter = named(new JComboBox<>(), "reportFilter");
    private final JLabel total = label("", SMALL, Font.PLAIN, MUTED);
    private final DefaultTableModel model = new DefaultTableModel(new String[]{"Saved (MYT)", "Area", "Issue", "Observation"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable table = named(new JTable(model), "reportTable");
    private final TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);

    ReportsPane() {
        super(new BorderLayout(GAP, 0));
        setOpaque(false);
        add(form(), BorderLayout.WEST);
        add(history(), BorderLayout.CENTER);
        updateCount();
    }

    private JPanel form() {
        var form = card(new BorderLayout(0, GAP));
        form.setPreferredSize(new Dimension(FORM_WIDTH, 0));
        var fields = panel(new GridLayout(0, 1, 0, 6));
        styleCombo(district);
        styleCombo(issue);
        fields.add(caption("Area"));
        fields.add(district);
        fields.add(caption("Issue type"));
        fields.add(issue);
        fields.add(caption("Observation (" + CitizenReport.DESCRIPTION_RANGE + " characters)"));
        form.add(fields, BorderLayout.NORTH);
        form.add(descriptionField(), BorderLayout.CENTER);
        form.add(actions(), BorderLayout.SOUTH);
        return form;
    }

    private JComponent descriptionField() {
        description.setLineWrap(true);
        description.setWrapStyleWord(true);
        description.setFont(font(BODY, Font.PLAIN));
        description.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        description.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { updateCount(); }
            @Override public void removeUpdate(DocumentEvent e) { updateCount(); }
            @Override public void changedUpdate(DocumentEvent e) { updateCount(); }
        });
        return scroll(description);
    }

    private JPanel actions() {
        var bar = panel(new BorderLayout(GAP, 6));
        bar.add(counter, BorderLayout.NORTH);
        bar.add(label("<html><body style='width:300px'>Saved on this device only and never sent to an authority. Avoid personal details.</body></html>",
                SMALL, Font.PLAIN, MUTED), BorderLayout.CENTER);
        styleButton(save, true);
        bar.add(save, BorderLayout.SOUTH);
        return bar;
    }

    private JPanel history() {
        styleTable(table);
        table.setRowSorter(sorter);
        for (int i = 0; i < WIDTHS.length; i++) table.getColumnModel().getColumn(i).setPreferredWidth(WIDTHS[i]);
        filter.addItem(ALL_AREAS);
        for (District item : District.values()) filter.addItem(item);
        styleCombo(filter);
        filter.addActionListener(e -> applyFilter());
        var bar = panel(new BorderLayout(GAP, 0));
        bar.add(filter, BorderLayout.WEST);
        bar.add(total, BorderLayout.CENTER);
        var block = panel(new BorderLayout(0, GAP));
        block.add(bar, BorderLayout.NORTH);
        block.add(scroll(table), BorderLayout.CENTER);
        return titled("Local report history", block);
    }

    private void applyFilter() {
        Object choice = filter.getSelectedItem();
        sorter.setRowFilter(choice instanceof District area
                ? RowFilter.regexFilter("^" + java.util.regex.Pattern.quote(area.toString()) + "$", DISTRICT_COLUMN) : null);
        updateTotal();
    }

    private void updateTotal() {
        total.setText(table.getRowCount() + " of " + model.getRowCount() + " reports shown");
    }

    private void updateCount() {
        int length = description.getText().strip().length();
        counter.setText(length + " / " + CitizenReport.MAX_DESCRIPTION + " input units");
        counter.setForeground(length > CitizenReport.MAX_DESCRIPTION ? ERROR_TEXT : MUTED);
    }

    /** Replaces the history rows, newest first, keeping the active filter. */
    void render(List<CitizenReport> entries) {
        model.setRowCount(0);
        for (var entry : entries) {
            model.addRow(new Object[]{Formats.shortTime(entry.createdAt()), entry.district().toString(), entry.issue(), entry.description()});
        }
        updateTotal();
    }

    /** Locks the form while a save is pending, so a second click cannot write a duplicate. */
    void setSaving(boolean value) {
        save.setEnabled(!value);
        description.setEditable(!value);
        district.setEnabled(!value);
        issue.setEnabled(!value);
    }

    void onSave(Runnable action) { save.addActionListener(e -> action.run()); }
    void selectDistrict(District value) { district.setSelectedItem(value); }
    void clearDescription() { description.setText(""); }
    District district() { return (District) district.getSelectedItem(); }
    Issue issue() { return (Issue) issue.getSelectedItem(); }
    String description() { return description.getText(); }
}
