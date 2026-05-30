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

        int maxX = Math.max(SCREEN_MARGIN, screenWidth - tooltipWidth - SCREEN_MARGIN);
        int clampedX = preferLeft ? leftX : maxX;
        return Math.max(SCREEN_MARGIN, Math.min(clampedX, maxX));
    }

    static int chooseY(int screenHeight, int mouseY, int tooltipHeight) {
        int y = mouseY - CURSOR_OFFSET;
        if (y + tooltipHeight + BOTTOM_PADDING > screenHeight) {
            y = Math.max(screenHeight - tooltipHeight - BOTTOM_PADDING, SCREEN_MARGIN);
        }
        return y;
    }
}
