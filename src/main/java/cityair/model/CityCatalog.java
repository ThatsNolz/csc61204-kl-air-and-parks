package cityair.model;

import java.net.URI;
import java.util.List;
import java.util.Objects;

/** Verified parks mapped to nearby pilot areas. */
public final class CityCatalog {
    private static final String DBKL_PARKS = "https://www.dbkl.gov.my/berita-acara/taman-awam-dbkl-kini-dibuka-lebih-awal-dan-lebih-lama";

    private CityCatalog() {}

    /** One verified green space with its published source. */
    public record Park(String name, String area, URI source) {}

    /** Returns curated nearby destinations, rather than routing or measured park air quality. */
    public static List<Park> nearby(District district) {
        return switch (Objects.requireNonNull(district)) {
            case TITIWANGSA -> List.of(new Park("Titiwangsa Lake Park", "Jalan Kuantan, Titiwangsa",
                    URI.create("https://www.dbkl.gov.my/en/fasiliti-awam/taman-awam/taman-tasik-titiwangsa")));
            case CITY_CENTRE -> List.of(new Park("KLCC Park", "Kuala Lumpur City Centre",
                    URI.create("https://www.suriaklcc.com.my/attractions/klcc-park/")));
            case PERDANA -> List.of(new Park("Perdana Botanical Garden", "Jalan Kebun Bunga, Tasik Perdana",
                    URI.create(DBKL_PARKS)));
        };
    }
}
