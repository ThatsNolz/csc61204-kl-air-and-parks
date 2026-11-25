package cityair.view;

import cityair.service.InsightEngine;
import cityair.service.InsightEngine.Insight;
import cityair.view.component.AuroraLabel;
import cityair.view.component.Panels;
import cityair.view.component.Paragraph;
import cityair.view.component.Text;
import cityair.view.design.Glyph;
import cityair.view.design.Icons;
import cityair.view.design.Metrics;
import cityair.view.design.Paints;
import cityair.view.design.Skin;
import cityair.view.design.Themed;
import cityair.view.design.Tone;
import cityair.view.design.Type;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.util.List;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

/**
 * Renders the statements produced by {@link InsightEngine}.
 *
 * <p>The list is presentation only: it chooses an icon and a colour for a level and lays the
 * sentences out. Deciding what is worth saying happens in the service layer, which is why this
 * class contains no rules about air quality.</p>
 */
final class InsightList extends JPanel implements Themed {
    private static final int MARKER = 26;

    InsightList() {
        super();
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setOpaque(false);
        Text.named(this, "insights");
    }

    /** Replaces the statements on display. */
    void render(List<Insight> insights) {
        removeAll();
        for (int i = 0; i < insights.size(); i++) {
            if (i > 0) add(Panels.gap(Metrics.SPACE_4));
            add(Panels.rigid(row(insights.get(i))));
        }
        add(javax.swing.Box.createVerticalGlue());
        getAccessibleContext().setAccessibleDescription(describe(insights));
        revalidate();
        repaint();
    }

    /** Re-reads the colours this component stores from the active theme. */
    @Override public void applyTheme() { repaint(); }

    private static String describe(List<Insight> insights) {
        return insights.stream().map(i -> i.title() + ": " + i.detail()).reduce("", (a, b) -> a + b + " ").strip();
    }

    private JComponent row(Insight insight) {
        var row = Panels.row(Metrics.SPACE_3);
        row.setAlignmentX(LEFT_ALIGNMENT);
        row.add(marker(insight), BorderLayout.WEST);
        var text = Panels.column(2);
        var title = new AuroraLabel(insight.title(), Type.strong(Type.LABEL), tone(insight));
        text.add(title, BorderLayout.NORTH);
        text.add(new Paragraph(insight.detail(), Tone.INK_MUTED, Type.LABEL), BorderLayout.CENTER);
        row.add(text, BorderLayout.CENTER);
        return row;
    }

    private JComponent marker(Insight insight) {
        var holder = Panels.of(new BorderLayout());
        holder.setBorder(new EmptyBorder(1, 0, 0, 0));
        holder.add(new LevelDot(glyph(insight.level()), tone(insight)), BorderLayout.NORTH);
        return holder;
    }

    /** The circular icon that carries a statement's level in both a shape and a colour. */
    private static final class LevelDot extends cityair.view.component.AuroraComponent {
        private final transient Glyph glyph;
        private final transient Tone tone;

        LevelDot(Glyph glyph, Tone tone) {
            this.glyph = glyph;
            this.tone = tone;
        }

        /** Returns the fixed size of the marker. */
        @Override public Dimension getPreferredSize() { return new Dimension(MARKER, MARKER); }

        /** Draws the halo and the glyph in the level's colour. */
        @Override protected void paintComponent(Graphics graphics) {
            var g = Paints.quality((Graphics2D) graphics.create());
            try {
                g.setColor(Paints.alpha(Skin.color(tone), 36));
                g.fill(new Ellipse2D.Double(0, 0, MARKER, MARKER));
                g.setColor(Skin.color(tone));
                glyph.paint(g, (MARKER - Icons.SMALL) / 2.0, (MARKER - Icons.SMALL) / 2.0, Icons.SMALL);
            } finally {
                g.dispose();
            }
        }
    }

    private static Tone tone(Insight insight) {
        return switch (insight.level()) {
            case POSITIVE -> Tone.POSITIVE;
            case CAUTION -> Tone.CAUTION;
            case CRITICAL -> Tone.DANGER;
            case NEUTRAL -> Tone.INFO;
        };
    }

    private static Glyph glyph(InsightEngine.Level level) {
        return switch (level) {
            case POSITIVE -> Glyph.CHECK;
            case CAUTION, CRITICAL -> Glyph.WARNING;
            case NEUTRAL -> Glyph.INFO;
        };
    }
}
