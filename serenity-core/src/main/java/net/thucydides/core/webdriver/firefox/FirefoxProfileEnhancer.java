package net.thucydides.core.webdriver.firefox;

import com.google.common.base.Splitter;
import net.thucydides.core.ThucydidesSystemProperty;
import net.thucydides.core.util.EnvironmentVariables;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.openqa.selenium.firefox.FirefoxProfile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class FirefoxProfileEnhancer {

    private static final String FIREFOX_NETWORK_PROXY_TYPE = "network.proxy.type";
    private static final String FIREFOX_NETWORK_PROXY_HTTP = "network.proxy.http";
    private static final String FIREFOX_NETWORK_PROXY_HTTP_PORT = "network.proxy.http_port";
    private final EnvironmentVariables environmentVariables;

    public FirefoxProfileEnhancer(EnvironmentVariables environmentVariables) {
        this.environmentVariables = environmentVariables;
    }

    public void configureJavaSupport(FirefoxProfile profile) {
        boolean enableJava = environmentVariables.getPropertyAsBoolean(ThucydidesSystemProperty.SECURITY_ENABLE_JAVA, false);
        profile.setPreference("security.enable_java", enableJava);
    }

    public void allowWindowResizeFor(final FirefoxProfile profile) {
        profile.setPreference("dom.disable_window_move_resize",false);
    }

    public void activateProxy(final FirefoxProfile profile, String proxyUrl, String proxyPort) {
        profile.setPreference(FIREFOX_NETWORK_PROXY_HTTP, proxyUrl);
        profile.setPreference(FIREFOX_NETWORK_PROXY_HTTP_PORT, NumberUtils.toInt(proxyPort));
        profile.setPreference(FIREFOX_NETWORK_PROXY_TYPE, 1);
    }

    static class PreferenceValue {
        private final String key;
        private final Object value;

        PreferenceValue(String key, Object value) {
            this.key = key;
            this.value = value;
        }

        void applyTo(FirefoxProfile profile) {
            if (value instanceof Boolean) {
                profile.setPreference(key, (Boolean) value);
            } else if (value instanceof Integer) {
                profile.setPreference(key, (Integer) value);
            } else {
                profile.setPreference(key, value.toString());
            }
        }
    }
    public void addPreferences(FirefoxProfile profile) {
        String preferences = environmentVariables.getProperty(ThucydidesSystemProperty.FIREFOX_PREFERENCES);
        List<PreferenceValue> preferenceValues = getPreferenceValuesFrom(preferences);
        for (PreferenceValue preference : preferenceValues) {
            preference.applyTo(profile);
        }
    }

    private List<PreferenceValue> getPreferenceValuesFrom(String preferences) {
        List<PreferenceValue> preferenceValues = new ArrayList<>();
        String preferenceSeparator = environmentVariables.getProperty(ThucydidesSystemProperty.FIREFOX_PREFERENCE_SEPARATOR,";");
        System.out.println("Separator = " + preferenceSeparator);

        if (StringUtils.isNotEmpty(preferences)) {
            List<String> arguments = split(preferences, preferenceSeparator);
            for(String argument : arguments) {
                System.out.println("arg = " + argument);
                preferenceValues.addAll(convertToPreferenceValue(argument).map(Collections::singleton).orElse(Collections.emptySet()));
            }
        }
        return preferenceValues;
    }

    private Optional<PreferenceValue> convertToPreferenceValue(String argument) {
        List<String> arguments = split(argument, "=");
        if (arguments.size() == 1) {
            String key = arguments.get(0);
            return Optional.of(new PreferenceValue(key,Boolean.TRUE));
        } else if (arguments.size() == 2) {
            String key = arguments.get(0);
            String value = arguments.get(1);
            return Optional.of(new PreferenceValue(key,argumentValueOf(value)));
        } else {
            return Optional.empty();
        }
    }

    private Object argumentValueOf(String value) {
        if (NumberUtils.isDigits(value)) {
            return Integer.parseInt(value);
        } else if (value.toLowerCase().equals("true") || value.toLowerCase().equals("false")) {
            return Boolean.valueOf(value);
        } else {
            return value;
        }
    }

    private List<String> split(String values, String separator) {
        return Splitter.on(separator).trimResults().splitToList(values);
    }
}
