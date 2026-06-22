package com.sanhiruzu.ami.client.tooltip;

public final class HeartBarTooltipLayout {
    public static final int MAX_HEARTS = 10;
    public static final int HEART_SIZE = 9;
    public static final int HEART_GAP = 1;
    public static final int TEXT_LINE_HEIGHT = 10;
    public static final int LABEL_TOP_PAD = 1;
    public static final int ROW_HEIGHT = HEART_SIZE + LABEL_TOP_PAD + TEXT_LINE_HEIGHT;

    private final int maxHealth;

    public HeartBarTooltipLayout(int maxHealth) {
        this.maxHealth = Math.max(0, maxHealth);
    }

    public int maxHealth() {
        return maxHealth;
    }

    public int shownHalfHearts() {
        return Math.min(MAX_HEARTS * 2, maxHealth);
    }

    public int heartCount() {
        return (shownHalfHearts() + 1) / 2;
    }

    public boolean hasOverflow() {
        return maxHealth > MAX_HEARTS * 2;
    }

    public int overflowHearts() {
        return maxHealth / 2 - MAX_HEARTS;
    }

    public int overflowLabelXOffset() {
        return MAX_HEARTS * (HEART_SIZE + HEART_GAP) + 2;
    }

    public int heartXOffset(int index) {
        return index * (HEART_SIZE + HEART_GAP);
    }

    public boolean isFullHeart(int index) {
        return shownHalfHearts() - index * 2 >= 2;
    }

    public String healthLabelKey() {
        return maxHealth % 2 == 0 ? "ami.tooltip.heart.even" : "ami.tooltip.heart.odd";
    }

    public int healthLabelHearts() {
        return maxHealth / 2;
    }
}
