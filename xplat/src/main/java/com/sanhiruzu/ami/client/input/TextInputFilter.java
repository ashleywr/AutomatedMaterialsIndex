package com.sanhiruzu.ami.client.input;

/**
 * Shared validation used by text input fields that should accept non-ASCII scripts.
 */
public final class TextInputFilter {

    private TextInputFilter() {
    }

    public static boolean isAllowedInput(String value) {
        return value.codePoints().allMatch(TextInputFilter::isAllowedCodePoint);
    }

    private static boolean isAllowedCodePoint(int codePoint) {
        if (codePoint <= Character.MAX_VALUE) {
            return !Character.isISOControl(codePoint);
        }
        return Character.isValidCodePoint(codePoint) && !Character.isISOControl(codePoint);
    }
}
