package cityair.view.design;

import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;

/**
 * The application's icon set, drawn as vectors on a 24 by 24 grid. Every glyph is stroked with the
 * same weight and cap, so the set reads as one family, and because nothing is a bitmap the icons
 * stay sharp at any size or display scale.
 */
public enum Glyph {
    /** Overview: a compact dashboard of tiles. */
    OVERVIEW {
        @Override void draw(Graphics2D g) {
            g.draw(rounded(3, 3, 7.5, 7.5));
            g.draw(rounded(13.5, 3, 7.5, 4.5));
            g.draw(rounded(3, 14, 7.5, 7));
            g.draw(rounded(13.5, 10.5, 7.5, 10.5));
        }
    },
    /** Live monitoring: a measurement trace. */
    PULSE {
        @Override void draw(Graphics2D g) {
            var path = new Path2D.Double();
            path.moveTo(2.5, 15);
            path.lineTo(7, 15);
            path.lineTo(9.5, 7.5);
            path.lineTo(13, 18.5);
            path.lineTo(15.5, 12);
            path.lineTo(21.5, 12);
            g.draw(path);
        }
    },
    /** Activity planner: a compass rose. */
    PLANNER {
        @Override void draw(Graphics2D g) {
            g.draw(new Ellipse2D.Double(3, 3, 18, 18));
            var needle = new Path2D.Double();
            needle.moveTo(15.8, 8.2);
            needle.lineTo(13.2, 13.2);
            needle.lineTo(8.2, 15.8);
            needle.lineTo(10.8, 10.8);
            needle.closePath();
            g.fill(needle);
        }
    },
    /** Citizen reports: a written note. */
    REPORTS {
        @Override void draw(Graphics2D g) {
            g.draw(rounded(3.5, 3.5, 17, 14));
            g.draw(new Line2D.Double(7.5, 8.5, 16.5, 8.5));
            g.draw(new Line2D.Double(7.5, 12.5, 13.5, 12.5));
            g.draw(new Line2D.Double(8, 17.5, 11.5, 21));
        }
    },
    /** Alert centre: a notification bell. */
    BELL {
        @Override void draw(Graphics2D g) {
            var bell = new Path2D.Double();
            bell.moveTo(5.5, 17);
            bell.curveTo(7.5, 15, 6.6, 13.5, 6.9, 10.6);
            bell.curveTo(7.3, 6.6, 9.4, 4.5, 12, 4.5);
            bell.curveTo(14.6, 4.5, 16.7, 6.6, 17.1, 10.6);
            bell.curveTo(17.4, 13.5, 16.5, 15, 18.5, 17);
            bell.closePath();
            g.draw(bell);
            g.draw(new Line2D.Double(10.2, 20, 13.8, 20));
        }
    },
    /** Data and impact: stacked records. */
    DATABASE {
        @Override void draw(Graphics2D g) {
            g.draw(new Ellipse2D.Double(4, 3.5, 16, 5));
            var side = new Path2D.Double();
            side.moveTo(4, 6);
            side.lineTo(4, 18);
            side.curveTo(4, 20.2, 20, 20.2, 20, 18);
            side.lineTo(20, 6);
            g.draw(side);
            g.draw(new Line2D.Double(4, 12, 4, 12.1));
            var middle = new Path2D.Double();
            middle.moveTo(4, 12);
            middle.curveTo(4, 14.2, 20, 14.2, 20, 12);
            g.draw(middle);
        }
    },
    /** Settings: labelled controls. */
    SETTINGS {
        @Override void draw(Graphics2D g) {
            for (int row = 0; row < 3; row++) {
                double y = 6.5 + row * 5.5;
                g.draw(new Line2D.Double(3.5, y, 20.5, y));
                g.fill(new Ellipse2D.Double(row == 1 ? 14 : 7.5, y - 2.2, 4.4, 4.4));
            }
        }
    },
    /** Refresh: a request for new data. */
    REFRESH {
        @Override void draw(Graphics2D g) {
            g.draw(new java.awt.geom.Arc2D.Double(4, 4, 16, 16, 60, 250, java.awt.geom.Arc2D.OPEN));
            var head = new Path2D.Double();
            head.moveTo(18, 2.8);
            head.lineTo(19.2, 8.4);
            head.lineTo(13.6, 8.0);
            head.closePath();
            g.fill(head);
        }
    },
    /** Compare: measurements of different magnitude. */
    COMPARE {
        @Override void draw(Graphics2D g) {
            g.fill(bar(4, 13, 4, 7.5));
            g.fill(bar(10, 7.5, 4, 13));
            g.fill(bar(16, 10.5, 4, 10));
        }
    },
    /** Light theme. */
    SUN {
        @Override void draw(Graphics2D g) {
            g.draw(new Ellipse2D.Double(8, 8, 8, 8));
            for (int i = 0; i < 8; i++) {
                double angle = Math.PI / 4 * i;
                g.draw(new Line2D.Double(12 + Math.cos(angle) * 10.4, 12 + Math.sin(angle) * 10.4,
                        12 + Math.cos(angle) * 7.6, 12 + Math.sin(angle) * 7.6));
            }
        }
    },
    /** Dark theme. */
    MOON {
        @Override void draw(Graphics2D g) {
            var moon = new Path2D.Double();
            moon.moveTo(19.5, 15.2);
            moon.curveTo(13.5, 17.5, 7.2, 13.2, 8.6, 6.9);
            moon.curveTo(9, 5.2, 10, 4, 10.6, 3.4);
            moon.curveTo(4.2, 4.6, 1.6, 12.8, 6.8, 17.6);
            moon.curveTo(11.2, 21.7, 18, 19.9, 19.5, 15.2);
            moon.closePath();
            g.fill(moon);
        }
    },
    /** Collapse the navigation rail. */
    CHEVRON_LEFT {
        @Override void draw(Graphics2D g) { chevron(g, true); }
    },
    /** Expand the navigation rail. */
    CHEVRON_RIGHT {
        @Override void draw(Graphics2D g) { chevron(g, false); }
    },
    /** Confirmation. */
    CHECK {
        @Override void draw(Graphics2D g) {
            var tick = new Path2D.Double();
            tick.moveTo(4.5, 12.5);
            tick.lineTo(9.8, 18);
            tick.lineTo(19.5, 6.5);
            g.draw(tick);
        }
    },
    /** Caution or a breached threshold. */
    WARNING {
        @Override void draw(Graphics2D g) {
            var triangle = new Path2D.Double();
            triangle.moveTo(12, 3.2);
            triangle.lineTo(22, 20.5);
            triangle.lineTo(2, 20.5);
            triangle.closePath();
            g.draw(triangle);
            g.draw(new Line2D.Double(12, 9.5, 12, 14.5));
            g.fill(new Ellipse2D.Double(10.9, 16.3, 2.2, 2.2));
        }
    },
    /** Neutral context. */
    INFO {
        @Override void draw(Graphics2D g) {
            g.draw(new Ellipse2D.Double(3, 3, 18, 18));
            g.draw(new Line2D.Double(12, 11, 12, 17));
            g.fill(new Ellipse2D.Double(10.9, 6.6, 2.2, 2.2));
        }
    },
    /** Dismiss. */
    CLOSE {
        @Override void draw(Graphics2D g) {
            g.draw(new Line2D.Double(5.5, 5.5, 18.5, 18.5));
            g.draw(new Line2D.Double(18.5, 5.5, 5.5, 18.5));
        }
    },
    /** Opens a published source in the system browser. */
    EXTERNAL {
        @Override void draw(Graphics2D g) {
            var frame = new Path2D.Double();
            frame.moveTo(13, 4.5);
            frame.lineTo(4.5, 4.5);
            frame.lineTo(4.5, 19.5);
            frame.lineTo(19.5, 19.5);
            frame.lineTo(19.5, 11);
            g.draw(frame);
            g.draw(new Line2D.Double(10.5, 13.5, 19.5, 4.5));
            g.draw(new Line2D.Double(13.5, 4.5, 19.5, 4.5));
            g.draw(new Line2D.Double(19.5, 4.5, 19.5, 10.5));
        }
    },
    /** Green space. */
    LEAF {
        @Override void draw(Graphics2D g) {
            var leaf = new Path2D.Double();
            leaf.moveTo(5, 19);
            leaf.curveTo(4, 10, 9, 4.5, 19.5, 4.5);
            leaf.curveTo(19.5, 15, 13.5, 19.5, 6.5, 17.5);
            g.draw(leaf);
            g.draw(new Line2D.Double(6.5, 17.5, 15.5, 8.5));
        }
    },
    /** Model hour or elapsed time. */
    CLOCK {
        @Override void draw(Graphics2D g) {
            g.draw(new Ellipse2D.Double(3, 3, 18, 18));
            g.draw(new Line2D.Double(12, 7.5, 12, 12.4));
            g.draw(new Line2D.Double(12, 12.4, 16, 14.6));
        }
    },
    /** A pilot location. */
    PIN {
        @Override void draw(Graphics2D g) {
            var pin = new Path2D.Double();
            pin.moveTo(12, 21.2);
            pin.curveTo(6.2, 14.6, 4.8, 12.2, 4.8, 9.6);
            pin.curveTo(4.8, 5.8, 8, 3, 12, 3);
            pin.curveTo(16, 3, 19.2, 5.8, 19.2, 9.6);
            pin.curveTo(19.2, 12.2, 17.8, 14.6, 12, 21.2);
            pin.closePath();
            g.draw(pin);
            g.draw(new Ellipse2D.Double(9.4, 7, 5.2, 5.2));
        }
    },
    /** Filtering a list. */
    FILTER {
        @Override void draw(Graphics2D g) {
            var funnel = new Path2D.Double();
            funnel.moveTo(3.5, 5);
            funnel.lineTo(20.5, 5);
            funnel.lineTo(14, 12.6);
            funnel.lineTo(14, 20);
            funnel.lineTo(10, 17.6);
            funnel.lineTo(10, 12.6);
            funnel.closePath();
            g.draw(funnel);
        }
    },
    /** Nothing to show yet. */
    EMPTY {
        @Override void draw(Graphics2D g) {
            g.draw(rounded(3, 6, 18, 14));
            g.draw(new Line2D.Double(3, 12.5, 21, 12.5));
            g.draw(new Line2D.Double(9, 16.5, 15, 16.5));
        }
    };

    /** Side of the grid every glyph is drawn on. */
    public static final double GRID = 24;
    private static final float WEIGHT = 1.8f;

    abstract void draw(Graphics2D g);

    /**
     * Paints the glyph at {@code (x, y)} scaled to {@code size}, in the current colour.
     * The caller keeps its own graphics state: this method works on a copy.
     */
    public void paint(Graphics2D graphics, double x, double y, double size) {
        var g = (Graphics2D) graphics.create();
        try {
            Paints.quality(g);
            g.translate(x, y);
            double scale = size / GRID;
            g.scale(scale, scale);
            g.setStroke(new BasicStroke(WEIGHT, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            draw(g);
        } finally {
            g.dispose();
        }
    }

    private static RoundRectangle2D rounded(double x, double y, double w, double h) {
        return new RoundRectangle2D.Double(x, y, w, h, 3.4, 3.4);
    }

    private static RoundRectangle2D bar(double x, double y, double w, double h) {
        return new RoundRectangle2D.Double(x, y, w, h, 2.2, 2.2);
    }

    private static void chevron(Graphics2D g, boolean pointingLeft) {
        double near = pointingLeft ? 15 : 9.5;
        double far = pointingLeft ? 9.5 : 15;
        var path = new Path2D.Double();
        path.moveTo(near, 5.5);
        path.lineTo(far, 12);
        path.lineTo(near, 18.5);
        g.draw(path);
    }
}
