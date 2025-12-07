package com.github.ragxar.wurm.client.mod.borabora;

import com.github.ragxar.wurm.client.mod.I18n;

public class Strings {
    private static I18n I18n;

    public static void init(String language, String bundle) {
        I18n = new I18n(language, bundle);
    }

    public enum window {
        title;

        public String localized() {
            return I18n.get("window." + name());
        }
    }

    public enum items {
        startedHint,
        selectedFormat,
        statusFormat,
        emptyCompatible;

        public String localized() {
            return I18n.get("items." + name());
        }
    }

    public enum tooltip {
        clear,
        quest,
        items;

        public String localized() {
            return I18n.get("tooltip." + name());
        }
    }
}
