package com.sanhiruzu.ami.index;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Builds a bounded plain-search token set from item tooltips.
 */
public final class TooltipSearchTokens {
    private static final int MAX_TOOLTIP_LINES = 16;
    private static final int MAX_TOKENS = 48;
    private static final int MAX_TOKEN_LENGTH = 40;
    private static final Set<String> TWO_LETTER_ALLOWLIST = Set.of(
            "tm", "hm", "hp", "xp", "fe", "rf", "eu", "su"
    );
    private static final Set<String> STOP_WORDS = Set.of(
            "a", "an", "and", "are", "as", "be", "by", "can", "ctrl", "control",
            "details", "empty", "false", "for", "from", "hold", "in", "info", "into",
            "is", "item", "left", "minecraft", "mod", "more", "none", "of", "on",
            "onto", "or", "press", "right", "shift", "show", "that", "the", "this",
            "to", "tooltip", "true", "use", "used", "uses", "using", "when", "while",
            "will", "with"
    );

    private TooltipSearchTokens() {
    }

    public static String extract(Collection<Component> tooltipLines, String displayName,
                                 ResourceLocation id, String modName) {
        if (tooltipLines == null || tooltipLines.isEmpty()) {
            return "";
        }

        LinkedHashSet<String> excluded = new LinkedHashSet<>();
        addExcludedTokens(excluded, displayName);
        if (id != null) {
            addExcludedTokens(excluded, id.toString());
            addExcludedTokens(excluded, id.getNamespace());
            addExcludedTokens(excluded, id.getPath());
        }
        addExcludedTokens(excluded, modName);

        LinkedHashSet<String> tokens = new LinkedHashSet<>();
        int linesRead = 0;
        for (Component line : tooltipLines) {
            if (line == null) {
                continue;
            }
            if (linesRead++ >= MAX_TOOLTIP_LINES) {
                break;
            }
            String text = line.getString();
            if (!shouldIndexLine(text)) {
                continue;
            }
            for (String token : tokenize(text)) {
                addToken(tokens, excluded, token);
                if (tokens.size() >= MAX_TOKENS) {
                    return String.join(" ", tokens);
                }
            }
        }
        return String.join(" ", tokens);
    }

    private static boolean shouldIndexLine(String rawLine) {
        if (rawLine == null || rawLine.isBlank()) {
            return false;
        }
        String line = stripFormatting(rawLine).trim();
        if (line.isBlank()) {
            return false;
        }
        String lower = line.toLowerCase(Locale.ROOT);
        if (line.startsWith("#")
                || line.startsWith("[")
                || lower.startsWith("id:")
                || lower.startsWith("comp:")
                || lower.startsWith("ami group:")
                || lower.startsWith("right-click")
                || lower.startsWith("alt +")) {
            return false;
        }
        return !(line.startsWith("+") && lower.contains("more") && lower.contains("hold shift"));
    }

    private static void addExcludedTokens(Set<String> excluded, String value) {
        for (String token : tokenize(value)) {
            excluded.add(token);
            foldedLatinVariant(token).ifPresent(excluded::add);
        }
    }

    private static void addToken(LinkedHashSet<String> tokens, Set<String> excluded, String token) {
        if (isAllowedToken(token, excluded)) {
            tokens.add(token);
        }
        foldedLatinVariant(token).ifPresent(folded -> {
            if (isAllowedToken(folded, excluded)) {
                tokens.add(folded);
            }
        });
    }

    private static boolean isAllowedToken(String token, Set<String> excluded) {
        if (token == null || token.isBlank()) {
            return false;
        }
        int length = token.length();
        if (length < 3 && !TWO_LETTER_ALLOWLIST.contains(token)) {
            return false;
        }
        if (length > MAX_TOKEN_LENGTH || STOP_WORDS.contains(token) || excluded.contains(token)) {
            return false;
        }
        return !token.chars().allMatch(Character::isDigit);
    }

    static List<String> tokenize(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        String withoutFormatting = stripFormatting(raw);
        String separatedCamelCase = withoutFormatting.replaceAll("([a-z])([A-Z])", "$1 $2");
        String normalized = Normalizer.normalize(separatedCamelCase, Normalizer.Form.NFKC)
                .toLowerCase(Locale.ROOT);
        String[] parts = normalized.split("[^\\p{L}\\p{N}]+");
        List<String> result = new ArrayList<>(parts.length);
        for (String part : parts) {
            if (!part.isBlank()) {
                result.add(part);
            }
        }
        return result;
    }

    private static String stripFormatting(String raw) {
        return raw.replaceAll("(?i)\u00A7[0-9A-FK-OR]", " ");
    }

    private static java.util.Optional<String> foldedLatinVariant(String token) {
        if (token == null || token.isBlank() || !containsLatinLetter(token)) {
            return java.util.Optional.empty();
        }
        String folded = Normalizer.normalize(token, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        return folded.equals(token) ? java.util.Optional.empty() : java.util.Optional.of(folded);
    }

    private static boolean containsLatinLetter(String value) {
        for (int offset = 0; offset < value.length(); ) {
            int codePoint = value.codePointAt(offset);
            if (Character.UnicodeScript.of(codePoint) == Character.UnicodeScript.LATIN) {
                return true;
            }
            offset += Character.charCount(codePoint);
        }
        return false;
    }
}
