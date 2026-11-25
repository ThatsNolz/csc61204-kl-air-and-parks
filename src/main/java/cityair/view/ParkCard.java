package cityair.view;

import cityair.model.AirCategory;
import cityair.model.CityCatalog;
import cityair.model.District;
import cityair.model.Reading;
import cityair.view.component.AuroraButton;
import cityair.view.component.AuroraLabel;
import cityair.view.component.Badge;
import cityair.view.component.Card;
import cityair.view.component.Panels;
import cityair.view.component.Paragraph;
import cityair.view.component.Text;
import cityair.view.design.Glyph;
import cityair.view.design.Metrics;
import cityair.view.design.Skin;
import cityair.view.design.Tone;
import cityair.view.design.Type;
import java.awt.BorderLayout;
import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JComponent;

/**
 * A verified green space near the selected area, with the district's air quality as context.
 *
 * <p>Only what the catalogue actually records is shown: the name, the area it is in and the
 * published page it was taken from. No opening hours, facilities or park-level measurements are
 * displayed, because the application holds none and a park is not where the model estimate was
 * produced. The suitability line is explicitly about the district estimate for that reason.</p>
 */
final class ParkCard extends Card {
    private static final Logger LOGGER = Logger.getLogger(ParkCard.class.getName());
    private final AuroraLabel name = Text.named(Text.heading(""), "parkName");
    private final AuroraLabel place = Text.muted("");
    private final Badge suitability = new Badge("No verified reading", Tone.INK_MUTED);
    private final Paragraph context = Text.note("");
    private final AuroraLabel attribution = Text.micro("");
    private final AuroraButton open = new AuroraButton("Open source page", AuroraButton.Kind.GHOST, Glyph.EXTERNAL);
    private transient URI source;

    ParkCard(String name) {
        super(new BorderLayout(0, Metrics.SPACE_3));
        Text.named(this, name);
        add(header(), BorderLayout.NORTH);
        add(body(), BorderLayout.CENTER);
        add(footer(), BorderLayout.SOUTH);
        open.addActionListener(event -> openSource());
    }

    /** Shows the green space listed for an area, with the current reading as context. */
    void render(District district, Reading reading) {
        var parks = CityCatalog.nearby(district);
        if (parks.isEmpty()) return;
        CityCatalog.Park park = parks.get(0);
        source = park.source();
        name.setText(park.name());
        place.setText(park.area());
        renderSuitability(reading);
        attribution.setText("Listed by " + park.source().getHost());
        open.setToolTipText(park.source().toString());
        getAccessibleContext().setAccessibleDescription(park.name() + " in " + park.area()
                + ". " + suitability.text() + ". " + context.getText());
    }

    private void renderSuitability(Reading reading) {
        if (reading == null) {
            suitability.set("No verified reading", () -> Skin.color(Tone.INK_MUTED));
            context.setText("Refresh this area to see how its modeled estimate relates to time outdoors.");
            return;
        }
        AirCategory level = reading.category();
        suitability.set(level.toString(), () -> CategoryPalette.fill(level));
        context.setText("District estimate for this model hour, not a measurement taken at the park. "
                + "Use it as context for how long to stay outdoors.");
    }

    private JComponent header() {
        var row = Panels.row(Metrics.SPACE_3);
        row.add(Text.caption("Nearby green space"), BorderLayout.CENTER);
        row.add(new AuroraLabel("", Type.text(Type.BODY), Tone.ACCENT), BorderLayout.EAST);
        return row;
    }

    private JComponent body() {
        var stack = Panels.column(Metrics.SPACE_2);
        stack.add(name, BorderLayout.NORTH);
        var middle = Panels.column(Metrics.SPACE_3);
        middle.add(place, BorderLayout.NORTH);
        middle.add(Panels.leading(suitability), BorderLayout.CENTER);
        stack.add(middle, BorderLayout.CENTER);
        stack.add(context, BorderLayout.SOUTH);
        return stack;
    }

    private JComponent footer() {
        var row = Panels.row(Metrics.SPACE_3);
        row.add(attribution, BorderLayout.CENTER);
        row.add(open, BorderLayout.EAST);
        return row;
    }

    private void openSource() {
        if (source == null) return;
        if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            attribution.setText("Open " + source + " in a browser.");
            return;
        }
        try {
            Desktop.getDesktop().browse(source);
        } catch (IOException | UnsupportedOperationException | SecurityException ex) {
            LOGGER.log(Level.INFO, "Could not open the park source page", ex);
            attribution.setText("Could not open a browser. The source is " + source);
        }
    }

    /** Re-reads the card's colours, including its suitability badge. */
    @Override public void applyTheme() {
        super.applyTheme();
        suitability.applyTheme();
    }
}
