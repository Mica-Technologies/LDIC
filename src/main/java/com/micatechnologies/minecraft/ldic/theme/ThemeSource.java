package com.micatechnologies.minecraft.ldic.theme;

import java.io.IOException;

/**
 * Where theme documents come from. The client implements this over the {@code config/ldic}
 * folder and the built-in themes in the LDIC jar; tests implement it over a map.
 */
public interface ThemeSource {

    /** A loaded theme document. */
    final class Document {
        /**
         * The asset base for paths written relative to this document, such as
         * {@code builtin/classic} or {@code config/themes/alto}. Relative asset paths in the
         * document become {@code theme://<location>/<path>}.
         */
        public final String location;
        /** A human-readable name for problem reports, such as the file path. */
        public final String name;
        public final String text;

        public Document(String location, String name, String text) {
            this.location = location;
            this.name = name;
            this.text = text;
        }
    }

    /**
     * Opens the theme {@code reference} names: {@code builtin:<id>} for a theme in the jar, or
     * {@code <id>} for {@code config/ldic/themes/<id>/theme.json}.
     *
     * @throws IOException when the theme does not exist or cannot be read
     */
    Document open(String reference) throws IOException;
}
