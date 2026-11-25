package cityair.view;

import cityair.model.CityCatalog;
import cityair.model.District;
import cityair.model.Reading;
import cityair.service.ReadingService.Result;
import cityair.strategy.AdviceProfile;
import java.awt.*;
import java.util.EnumMap;
import java.util.Map;
import javax.swing.*;
import static cityair.view.Theme.*;

/**
 * Advice screen. The selected profile's guidance is shown in full, and a comparison row applies
 * every {@link cityair.strategy.AdviceStrategy} to the same category so the policies can be compared.
 */
final class AdvicePane extends JPanel {
    private static final int TEXT_WIDTH = 210;
    private final JTextArea advice;
    private final Map<AdviceProfile, JPanel> cards = new EnumMap<>(AdviceProfile.class);
    private final Map<AdviceProfile, JLabel> texts = new EnumMap<>(AdviceProfile.class);

    /** Builds the screen around the profile selector and the detailed advice text. */
    AdvicePane(JComboBox<AdviceProfile> profile, JTextArea advice) {
        super(new BorderLayout());
        setOpaque(false);
        this.advice = advice;
        var top = panel(new BorderLayout(GAP, 0));
        top.add(selector(profile), BorderLayout.WEST);
        top.add(titled("Selected guidance and nearby park", advice), BorderLayout.CENTER);
        var stack = panel(new BorderLayout(0, GAP));
        stack.add(top, BorderLayout.NORTH);
        stack.add(comparison(), BorderLayout.CENTER);
        add(stack, BorderLayout.NORTH);
    }

    private static JPanel selector(JComboBox<AdviceProfile> profile) {
        var box = card(new BorderLayout(0, GAP));
        box.add(caption("Audience"), BorderLayout.NORTH);
        styleCombo(profile);
        profile.setPreferredSize(new Dimension(190, 36));
        var holder = panel(new BorderLayout());
        holder.add(profile, BorderLayout.NORTH);
        box.add(holder, BorderLayout.CENTER);
        box.add(label("<html><body style='width:150px'>Each audience uses its own advice strategy.</body></html>",
                SMALL, Font.PLAIN, MUTED), BorderLayout.SOUTH);
        return box;
    }

    private JPanel comparison() {
        var row = panel(new GridLayout(1, AdviceProfile.values().length, GAP, 0));
        for (AdviceProfile item : AdviceProfile.values()) {
            var text = label("", BODY, Font.PLAIN, TEXT);
            text.setVerticalAlignment(SwingConstants.TOP);
            var card = card(new BorderLayout(0, GAP));
            card.add(caption(item.toString()), BorderLayout.NORTH);
            card.add(text, BorderLayout.CENTER);
            cards.put(item, card);
            texts.put(item, text);
            row.add(card);
        }
        return titled("Compare audiences for the same reading", row);
    }

    /** Updates the detailed text, the comparison cards and the highlighted audience. */
    void render(District district, Result result, String source, AdviceProfile selected) {
        Reading reading = result == null ? null : result.reading();
        advice.setText(detail(district, reading, source, selected));
        advice.setCaretPosition(0);
        for (AdviceProfile item : AdviceProfile.values()) {
            String body = reading == null ? "No verified reading yet." : item.strategy().advice(reading.category());
            texts.get(item).setText("<html><body style='width:" + TEXT_WIDTH + "px'>" + body + "</body></html>");
            cards.get(item).setBorder(item == selected ? accentBorder(TEAL) : cardBorder(LINE));
        }
    }

    private static String detail(District district, Reading reading, String source, AdviceProfile selected) {
        var text = new StringBuilder(selected + " · " + district + "\n");
        if (reading == null) text.append("No verified reading. Check local guidance before planning.");
        else text.append(source).append(" · ").append(Formats.time(reading.validAt())).append(" · ")
                .append(reading.category()).append("\n").append(selected.strategy().advice(reading.category()));
        for (CityCatalog.Park park : CityCatalog.nearby(district)) {
            text.append("\n\nNearby green space: ").append(park.name()).append(" (").append(park.area())
                    .append(")\nSource: ").append(park.source());
        }
        return text.append("\n\nDistrict model context only. This guidance is informational.").toString();
    }
}
