package cityair.view;

import cityair.model.CitizenReport;
import cityair.model.CitizenReport.Issue;
import cityair.model.District;
import cityair.view.component.AuroraButton;
import cityair.view.component.AuroraLabel;
import cityair.view.component.Card;
import cityair.view.component.DataTable;
import cityair.view.component.Dropdown;
import cityair.view.component.EmptyState;
import cityair.view.component.Panels;
import cityair.view.component.Scrolls;
import cityair.view.component.Text;
import cityair.view.component.TextArea;
import cityair.view.design.Glyph;
import cityair.view.design.Metrics;
import cityair.view.design.Skin;
import cityair.view.design.Themed;
import cityair.view.design.Tone;
import cityair.view.design.Type;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.util.List;
import java.util.regex.Pattern;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.RowFilter;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

/**
 * Citizen reports: a validated form on the left and this device's history on the right.
 *
 * <p>The form states its limits before the reader types rather than after they submit: the
 * accepted length is in the caption, the counter turns red the moment the text passes it, and the
 * privacy notice sits beside the button and not in a dialog nobody reads. Reports never leave the
 * device, which the screen says in plain words.</p>
 */
final class ReportsPane extends JPanel implements Themed {
    /** The filter entry that clears the area filter. */
    static final String ALL_AREAS = "All areas";
    private static final int FORM_WIDTH = 380, DESCRIPTION_ROWS = 7, DISTRICT_COLUMN = 1;
    private static final int[] WIDTHS = {140, 120, 90, 420};
    private final Dropdown<District> district = Text.named(new Dropdown<>(District.values()), "reportDistrict");
    private final Dropdown<Issue> issue = Text.named(new Dropdown<>(Issue.values()), "issue");
    private final TextArea description = Text.named(new TextArea(DESCRIPTION_ROWS), "description");
    private final AuroraLabel counter = Text.micro("");
    private final AuroraButton save = Text.named(new AuroraButton("Save local report",
            AuroraButton.Kind.PRIMARY, Glyph.CHECK), "save");
    private final Dropdown<Object> filter = Text.named(new Dropdown<>(filterOptions()), "reportFilter");
    private final AuroraLabel total = Text.micro("");
    private final DefaultTableModel model = DataTable.model("Saved (MYT)", "Area", "Issue", "Observation");
    private final DataTable table = new DataTable(model);
    private final TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
    private final CardLayout historyLayout = new CardLayout();
    private final JPanel historyBody = new JPanel(historyLayout);

    ReportsPane() {
        super(new BorderLayout(Metrics.SPACE_5, 0));
        setOpaque(false);
        add(form(), BorderLayout.WEST);
        add(history(), BorderLayout.CENTER);
        updateCount();
        updateTotal();
    }

    private static Object[] filterOptions() {
        var options = new Object[District.values().length + 1];
        options[0] = ALL_AREAS;
        System.arraycopy(District.values(), 0, options, 1, District.values().length);
        return options;
    }

    private JComponent form() {
        var card = new Card(new BorderLayout(0, Metrics.SPACE_4));
        card.add(Text.caption("Record an observation"), BorderLayout.NORTH);
        card.add(fields(), BorderLayout.CENTER);
        card.add(actions(), BorderLayout.SOUTH);
        return Panels.fixedWidth(card, FORM_WIDTH);
    }

    private JComponent fields() {
        var stack = Panels.column(Metrics.SPACE_3);
        var top = Panels.rows(Metrics.SPACE_2);
        top.add(Text.caption("Area"));
        top.add(district);
        top.add(Text.caption("Issue type"));
        top.add(issue);
        top.add(Text.caption("Observation (" + CitizenReport.DESCRIPTION_RANGE + " characters)"));
        stack.add(top, BorderLayout.NORTH);
        countWhileTyping();
        stack.add(description, BorderLayout.CENTER);
        return stack;
    }

    /** Keeps the character counter in step with every edit of the observation field. */
    private void countWhileTyping() {
        description.getDocument().addDocumentListener(new DocumentListener() {
            /** Updates the character counter as the reader types. */
            @Override public void insertUpdate(DocumentEvent event) { updateCount(); }
            /** Updates the character counter as the reader deletes. */
            @Override public void removeUpdate(DocumentEvent event) { updateCount(); }
            /** Updates the character counter after a formatting change. */
            @Override public void changedUpdate(DocumentEvent event) { updateCount(); }
        });
    }

    private JComponent actions() {
        var stack = Panels.column(Metrics.SPACE_3);
        stack.add(counter, BorderLayout.NORTH);
        stack.add(Text.note("Saved on this device only and never sent to an authority. "
                + "Avoid personal details."), BorderLayout.CENTER);
        stack.add(save, BorderLayout.SOUTH);
        return stack;
    }

    private JComponent history() {
        Text.described(table, "reportTable", "Reports saved on this device, newest first");
        table.setRowSorter(sorter);
        table.widths(WIDTHS);
        historyBody.setOpaque(false);
        historyBody.add(Scrolls.of(table), "table");
        historyBody.add(EmptyState.of(Glyph.REPORTS, "No reports yet",
                "Saved observations appear here. They stay on this device and are never submitted."), "empty");
        var bar = Panels.row(Metrics.SPACE_3);
        bar.add(filter, BorderLayout.WEST);
        bar.add(total, BorderLayout.CENTER);
        var block = Panels.column(Metrics.SPACE_3);
        block.add(bar, BorderLayout.NORTH);
        block.add(historyBody, BorderLayout.CENTER);
        filter.addActionListener(event -> applyFilter());
        return Card.titled("Local report history", block);
    }

    private void applyFilter() {
        Object choice = filter.getSelectedItem();
        sorter.setRowFilter(choice instanceof District area
                ? RowFilter.regexFilter("^" + Pattern.quote(area.toString()) + "$", DISTRICT_COLUMN) : null);
        updateTotal();
    }

    private void updateTotal() {
        total.setText(table.getRowCount() + " of " + model.getRowCount() + " reports shown");
        historyLayout.show(historyBody, model.getRowCount() == 0 ? "empty" : "table");
    }

    private void updateCount() {
        int length = description.getText().strip().length();
        boolean tooLong = length > CitizenReport.MAX_DESCRIPTION;
        counter.setText(length + " / " + CitizenReport.MAX_DESCRIPTION + " input units");
        counter.setForeground(Skin.color(tooLong ? Tone.DANGER : Tone.INK_FAINT));
        counter.setFont(tooLong ? Type.strong(Type.MICRO) : Type.text(Type.MICRO));
        description.setInvalid(tooLong);
    }

    /** Replaces the history rows, newest first, keeping the active filter. */
    void render(List<CitizenReport> entries) {
        model.setRowCount(0);
        for (var entry : entries) {
            model.addRow(new Object[]{Formats.shortTime(entry.createdAt()), entry.district().toString(),
                    entry.issue(), entry.description()});
        }
        updateTotal();
    }

    /** Locks the form while a save is pending, so a second click cannot write a duplicate. */
    void setSaving(boolean value) {
        save.setLoading(value);
        description.setEditable(!value);
        district.setEnabled(!value);
        issue.setEnabled(!value);
    }

    /** Marks the description as rejected after a failed validation. */
    void setInvalid(boolean value) { description.setInvalid(value); }

    /** Reports a save request. */
    void onSave(Runnable action) { save.addActionListener(event -> action.run()); }

    /** Preselects the area, following the area chosen elsewhere in the application. */
    void selectDistrict(District value) { district.setSelectedItem(value); }

    /** Clears the description after a successful save. */
    void clearDescription() {
        description.setText("");
        description.setInvalid(false);
    }

    /** The area the form will file against. */
    District district() { return (District) district.getSelectedItem(); }

    /** The issue type the form will file. */
    Issue issue() { return (Issue) issue.getSelectedItem(); }

    /** The text the form will file. */
    String description() { return description.getText(); }

    /** Re-reads the colours this component stores from the active theme. */
    @Override public void applyTheme() { repaint(); }
}
