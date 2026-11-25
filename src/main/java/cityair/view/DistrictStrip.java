package cityair.view;

import cityair.model.District;
import cityair.model.Reading;
import cityair.service.ReadingService.Result;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Consumer;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import static cityair.view.Theme.*;

/** "All pilot areas" strip: one compact card per district; clicking a card selects that district. */
final class DistrictStrip extends JPanel {
    private final Map<District, JPanel> cards = new EnumMap<>(District.class);
    private final Map<District, JLabel> values = new EnumMap<>(District.class);
    private final Map<District, JLabel> categories = new EnumMap<>(District.class);
    private Consumer<District> chooser = district -> { };

    DistrictStrip() {
        super(new BorderLayout(0, 6));
        setOpaque(false);
        add(caption("All pilot areas · click to switch"), BorderLayout.NORTH);
        var row = panel(new GridLayout(1, District.values().length, GAP, 0));
        for (District item : District.values()) row.add(card(item));
        add(row, BorderLayout.CENTER);
    }

    private JPanel card(District item) {
        var value = named(label("—", TITLE, Font.BOLD, TEXT), "stripAqi." + item.name());
        var category = chip();
        var card = Theme.card(new BorderLayout(GAP, 0));
        card.add(names(item, category), BorderLayout.CENTER);
        card.add(value, BorderLayout.EAST);
        clickable(card, item);
        cards.put(item, card);
        values.put(item, value);
        categories.put(item, category);
        return card;
    }

    private static JLabel chip() {
        var category = label("Not loaded", SMALL, Font.BOLD, MUTED);
        category.setOpaque(true);
        category.setBackground(PALE);
        category.setBorder(new EmptyBorder(2, 8, 2, 8));
        return category;
    }

    private static JPanel names(District item, JLabel category) {
        var text = panel(new GridLayout(0, 1, 0, 4));
        text.add(label(item.toString(), BODY, Font.BOLD, TEXT));
        var holder = panel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        holder.add(category);
        text.add(holder);
        return text;
    }

    private void clickable(JPanel card, District item) {
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        card.setToolTipText("Show " + item);
        card.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent event) { chooser.accept(item); }
        });
    }

    void onChoose(Consumer<District> action) { chooser = java.util.Objects.requireNonNull(action); }

    /** Shows each district's latest verified index and highlights the selected district. */
    void render(Map<District, Result> results, District selected) {
        for (District item : District.values()) {
            Result result = results.get(item);
            Reading reading = result == null ? null : result.reading();
            values.get(item).setText(reading == null ? "—" : Integer.toString(reading.aqi()));
            paintCategory(categories.get(item), result, reading);
            cards.get(item).setBorder(item == selected ? accentBorder(TEAL) : cardBorder(LINE));
        }
    }

    private static void paintCategory(JLabel category, Result result, Reading reading) {
        if (reading == null) {
            category.setText(result == null ? "Not loaded" : Formats.MISSING);
            category.setBackground(PALE);
            category.setForeground(MUTED);
            return;
        }
        category.setText(reading.category().toString());
        category.setBackground(CategoryPalette.fill(reading.category()));
        category.setForeground(CategoryPalette.onFill(reading.category()));
    }
}
