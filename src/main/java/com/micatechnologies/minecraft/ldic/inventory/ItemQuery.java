package com.micatechnologies.minecraft.ldic.inventory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * A creative-inventory search query. Words are separated by spaces and must all match:
 *
 * <ul>
 *   <li>{@code oak} matches the item's name or registry id ({@code minecraft:log});</li>
 *   <li>{@code @quark} matches the mod's id or name;</li>
 *   <li>{@code #fuel} matches any tooltip line;</li>
 *   <li>a leading {@code -} excludes instead: {@code -@minecraft}, {@code -slab}.</li>
 * </ul>
 *
 * Matching ignores case and {@code §} formatting codes. A query of a single plain word is
 * "simple", so callers can keep vanilla's indexed search for it.
 */
public final class ItemQuery {

    /** What a query can look at for one item; tooltip is asked for only when a term needs it. */
    public interface Facts {
        String name();

        String id();

        String modId();

        String modName();

        List<String> tooltip();
    }

    private enum Kind {
        TEXT, MOD, TOOLTIP
    }

    private static final class Term {
        final Kind kind;
        final String text;
        final boolean negated;

        Term(Kind kind, String text, boolean negated) {
            this.kind = kind;
            this.text = text;
            this.negated = negated;
        }
    }

    private static final ItemQuery EMPTY = new ItemQuery(Collections.emptyList());

    private final List<Term> terms;

    private ItemQuery(List<Term> terms) {
        this.terms = terms;
    }

    public static ItemQuery parse(String text) {
        if (text == null || text.trim().isEmpty()) {
            return EMPTY;
        }
        List<Term> terms = new ArrayList<>();
        for (String word : text.trim().toLowerCase(Locale.ROOT).split("\\s+")) {
            boolean negated = word.startsWith("-");
            if (negated) {
                word = word.substring(1);
            }
            Kind kind = Kind.TEXT;
            if (word.startsWith("@")) {
                kind = Kind.MOD;
                word = word.substring(1);
            } else if (word.startsWith("#")) {
                kind = Kind.TOOLTIP;
                word = word.substring(1);
            }
            if (!word.isEmpty()) {
                terms.add(new Term(kind, word, negated));
            }
        }
        return terms.isEmpty() ? EMPTY : new ItemQuery(terms);
    }

    public boolean isEmpty() {
        return terms.isEmpty();
    }

    /** True for one plain, positive word: the kind of query vanilla's search already handles. */
    public boolean isSimple() {
        return terms.size() == 1 && terms.get(0).kind == Kind.TEXT && !terms.get(0).negated;
    }

    public boolean matches(Facts item) {
        for (Term term : terms) {
            if (matches(term, item) == term.negated) {
                return false;
            }
        }
        return true;
    }

    private static boolean matches(Term term, Facts item) {
        switch (term.kind) {
            case MOD:
                return contains(item.modId(), term.text) || contains(item.modName(), term.text);
            case TOOLTIP:
                for (String line : item.tooltip()) {
                    if (contains(line, term.text)) {
                        return true;
                    }
                }
                return false;
            default:
                return contains(item.name(), term.text) || contains(item.id(), term.text);
        }
    }

    private static boolean contains(String haystack, String needle) {
        return haystack != null && stripFormatting(haystack).toLowerCase(Locale.ROOT).contains(needle);
    }

    static String stripFormatting(String s) {
        if (s.indexOf('§') < 0) {
            return s;
        }
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '§' && i + 1 < s.length()) {
                i++;
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }
}
