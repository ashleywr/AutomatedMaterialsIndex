package com.sanhiruzu.ami.client.tooltip;

final class TooltipPositioning {
    private static final int SCREEN_MARGIN = 4;
    private static final int CURSOR_OFFSET = 12;
    private static final int BOTTOM_PADDING = 3;

    private TooltipPositioning() {}

    static int chooseX(int screenWidth, int mouseX, int tooltipWidth, boolean preferLeft) {
        int leftX = mouseX - CURSOR_OFFSET - tooltipWidth;
        int rightX = mouseX + CURSOR_OFFSET;

        boolean fitsLeft = leftX >= SCREEN_MARGIN;
        boolean fitsRight = rightX + tooltipWidth <= screenWidth - SCREEN_MARGIN;

        if (preferLeft) {
            if (fitsLeft) return leftX;
            if (fitsRight) return rightX;
        } else {
            if (fitsRight) return rightX;
            if (fitsLeft) return leftX;
        }

        // Neither side fits perfectly. Try to clamp to the screen edges.
        int clampedRight = Math.max(SCREEN_MARGIN, screenWidth - tooltipWidth - SCREEN_MARGIN);
        int clampedLeft = SCREEN_MARGIN;
        
        // Choose the clamped position that covers less of the cursor
        if (preferLeft) {
            return clampedLeft; // Prefer pushing off left over right
        } else {
            return clampedRight; // Prefer pushing off right over left
        }
    }

    static int chooseY(int screenHeight, int mouseY, int tooltipHeight) {
        int y = mouseY - CURSOR_OFFSET;
        // If it goes off the bottom, try to push it up so it's above the cursor
        if (y + tooltipHeight + BOTTOM_PADDING > screenHeight) {
            y = mouseY - CURSOR_OFFSET - tooltipHeight;
        }
        // If it's now off the top, clamp it to the top
        if (y < SCREEN_MARGIN) {
            y = SCREEN_MARGIN;
        }
        // Final bottom clamp
        if (y + tooltipHeight + BOTTOM_PADDING > screenHeight) {
            y = Math.max(SCREEN_MARGIN, screenHeight - tooltipHeight - BOTTOM_PADDING);
        }
        return y;
    }
}
