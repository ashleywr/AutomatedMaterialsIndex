package com.sanhiruzu.ami.index;

import org.junit.jupiter.api.Test;
import java.io.File;
import java.nio.file.Files;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TranslationKeysTest {

    @Test
    public void verifyNoUnlocalizedStrings() throws Exception {
        // 1. Load en_us.json keys
        File langFile = new File("src/main/resources/assets/ami/lang/en_us.json");
        assertTrue(langFile.exists(), "Language file en_us.json must exist");

        Set<String> langKeys = new HashSet<>();
        List<String> lines = Files.readAllLines(langFile.toPath());
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("\"ami.")) {
                int secondQuote = trimmed.indexOf("\"", 1);
                if (secondQuote > 1) {
                    langKeys.add(trimmed.substring(1, secondQuote));
                }
            }
        }

        assertTrue(!langKeys.isEmpty(), "Loaded keys from en_us.json should not be empty");

        // 2. Scan all Java files recursively
        File srcDir = new File("src/main/java");
        assertTrue(srcDir.exists(), "Source directory src/main/java must exist");

        List<File> javaFiles = new ArrayList<>();
        findJavaFiles(srcDir, javaFiles);

        // Regex to capture double-quoted string literals starting with "ami."
        Pattern pattern = Pattern.compile("\"(ami\\.[a-zA-Z0-9_\\-\\.]+)\"");
        List<String> failures = new ArrayList<>();

        for (File file : javaFiles) {
            String content = Files.readString(file.toPath());
            Matcher matcher = pattern.matcher(content);
            while (matcher.find()) {
                String literal = matcher.group(1);
                // Check if this literal is exactly a key, or is a prefix of any registered key (for dynamic concats)
                boolean matched = langKeys.contains(literal);
                if (!matched) {
                    // Check if it's a dynamic prefix
                    for (String key : langKeys) {
                        if (key.startsWith(literal)) {
                            matched = true;
                            break;
                        }
                    }
                }

                if (!matched) {
                    failures.add("Unlocalized string literal found: \"" + literal + "\" in " + file.getPath());
                }
            }
        }

        // 3. Assert no failures
        if (!failures.isEmpty()) {
            StringBuilder sb = new StringBuilder("\nTranslation verification failed! Found unlocalized string(s):\n");
            for (String fail : failures) {
                sb.append("  - ").append(fail).append("\n");
            }
            sb.append("\nPlease add these keys to en_us.json to ensure full localization.\n");
            assertTrue(failures.isEmpty(), sb.toString());
        }
    }

    @Test
    public void verifyAllRegisteredRecipeTypesAreLocalized() throws Exception {
        // 1. Load en_us.json keys
        File langFile = new File("src/main/resources/assets/ami/lang/en_us.json");
        assertTrue(langFile.exists(), "Language file en_us.json must exist");

        Set<String> langKeys = new HashSet<>();
        List<String> lines = Files.readAllLines(langFile.toPath());
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("\"ami.")) {
                int secondQuote = trimmed.indexOf("\"", 1);
                if (secondQuote > 1) {
                    langKeys.add(trimmed.substring(1, secondQuote));
                }
            }
        }

        // 2. Exact list of all supported recipe types that can be displayed as tabs in AMI
        List<String> recipeTypes = List.of(
            "crafting",
            "smelting",
            "blasting",
            "smoking",
            "campfire_cooking",
            "stonecutting",
            "smithing",
            "brewing",
            "grinding",
            "anvil_repairing",
            "composting",
            "fuel"
        );

        List<String> failures = new ArrayList<>();
        for (String type : recipeTypes) {
            String tabKey = "ami.recipe_viewer.tab." + type;
            String tabShortKey = "ami.recipe_viewer.tab.short." + type;

            if (!langKeys.contains(tabKey)) {
                failures.add("Missing localization key for recipe tab: \"" + tabKey + "\"");
            }
            if (!langKeys.contains(tabShortKey)) {
                failures.add("Missing localization key for recipe tab short label: \"" + tabShortKey + "\"");
            }
        }

        // 3. Assert no failures
        if (!failures.isEmpty()) {
            StringBuilder sb = new StringBuilder("\nRecipe type localization verification failed! Missing key(s):\n");
            for (String fail : failures) {
                sb.append("  - ").append(fail).append("\n");
            }
            sb.append("\nPlease add these keys to en_us.json to ensure they render properly in the UI without dynamic fallback raw strings.\n");
            assertTrue(failures.isEmpty(), sb.toString());
        }
    }

    private void findJavaFiles(File dir, List<File> accumulator) {
        File[] children = dir.listFiles();
        if (children == null) return;
        for (File child : children) {
            if (child.isDirectory()) {
                findJavaFiles(child, accumulator);
            } else if (child.getName().endsWith(".java")) {
                accumulator.add(child);
            }
        }
    }
}
