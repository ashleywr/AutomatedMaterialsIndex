package com.sanhiruzu.ami.client.overlay;

/**
 * Immutable axis-aligned rectangle for layout computation.
 *
 * <p>Instead of scattering {@code x + margin + headerH + gap} arithmetic across
 * layout methods, build a tree of regions by splitting a root rect:
 *
 * <pre>{@code
 *   Rect screen  = Rect.of(0, 0, screenW, screenH);
 *   Rect[] parts = screen.splitBottom(BOTTOM_BAR_H, 0);
 *   Rect usable  = parts[0];            // everything above the bar
 *   Rect bar     = parts[1];            // the bar itself
 *
 *   Rect[] sides = usable.splitRight(panelW, MARGIN);
 *   Rect content = sides[0];            // main content area
 *   Rect panel   = sides[1];            // right panel strip
 *
 *   Rect[] rows  = panel.halves(GAP);   // split panel into two equal rows
 * }</pre>
 *
 * <p>All operations clamp width/height to zero — negative-size rects can't occur.
 */
public record Rect(int x, int y, int w, int h) {

    /**
     * Factory with non-negative clamping.
     */
    public static Rect of(int x, int y, int w, int h) {
        return new Rect(x, y, Math.max(0, w), Math.max(0, h));
    }

    // ── Splitting ─────────────────────────────────────────────────────────────

    /**
     * Carves a strip of height {@code stripH} from the top.
     *
     * @return [0] top strip, [1] remainder ({@code h - stripH - gap} tall)
     */
    public Rect[] splitTop(int stripH, int gap) {
        return new Rect[]{
                Rect.of(x, y, w, stripH),
                Rect.of(x, y + stripH + gap, w, h - stripH - gap)
        };
    }

    /**
     * Carves a strip of height {@code stripH} from the bottom.
     *
     * @return [0] remainder, [1] bottom strip
     */
    public Rect[] splitBottom(int stripH, int gap) {
        return new Rect[]{
                Rect.of(x, y, w, h - stripH - gap),
                Rect.of(x, y + h - stripH, w, stripH)
        };
    }

    /**
     * Carves a strip of width {@code stripW} from the right.
     *
     * @return [0] left remainder, [1] right strip
     */
    public Rect[] splitRight(int stripW, int gap) {
        return new Rect[]{
                Rect.of(x, y, w - stripW - gap, h),
                Rect.of(x + w - stripW, y, stripW, h)
        };
    }

    /**
     * Carves a strip of width {@code stripW} from the left.
     *
     * @return [0] left strip, [1] right remainder
     */
    public Rect[] splitLeft(int stripW, int gap) {
        return new Rect[]{
                Rect.of(x, y, stripW, h),
                Rect.of(x + stripW + gap, y, w - stripW - gap, h)
        };
    }

    /**
     * Splits into two equal halves vertically (top / bottom) separated by {@code gap}.
     * The top half absorbs any odd pixel.
     *
     * @return [0] top half, [1] bottom half
     */
    public Rect[] halves(int gap) {
        int h1 = (h - gap) / 2;
        int h2 = h - h1 - gap;
        return new Rect[]{
                Rect.of(x, y, w, h1),
                Rect.of(x, y + h1 + gap, w, h2)
        };
    }

    // ── Padding / repositioning ───────────────────────────────────────────────

    /**
     * Shrinks all four sides by {@code px}.
     */
    public Rect pad(int px) {
        return Rect.of(x + px, y + px, w - 2 * px, h - 2 * px);
    }

    /**
     * Returns a copy vertically centred within a container spanning
     * [{@code containerY}, {@code containerY + containerH}).
     * The width and x are unchanged.
     */
    public Rect centreInHeight(int containerY, int containerH) {
        return Rect.of(x, containerY + (containerH - h) / 2, w, h);
    }

    // ── Conversion ────────────────────────────────────────────────────────────

    /**
     * Converts to a {@link WidgetBounds} for use with overlay widgets.
     */
    public WidgetBounds toWidgetBounds() {
        return new WidgetBounds(x, y, w, h);
    }

    public boolean contains(double mx, double my) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
}
