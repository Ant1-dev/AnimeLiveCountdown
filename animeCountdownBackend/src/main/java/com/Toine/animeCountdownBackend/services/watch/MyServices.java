package com.Toine.animeCountdownBackend.services.watch;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

/**
 * A user's "My services": the catalog services they use plus their own platforms (a name and a link).
 * {@link #validate} normalizes a request or rejects it. Pure functions (unit tested).
 */
public final class MyServices {
    public static final int MAX_CUSTOM = 10;
    public static final int MAX_NAME_LENGTH = 40;
    public static final int MAX_URL_LENGTH = 500;
    /** Replaced by the show's URL-encoded title, turning a site URL into a search link. */
    public static final String TITLE_PLACEHOLDER = "{title}";

    private MyServices() {
    }

    public record CustomPlatform(String name, String url) {
    }

    /** Both the request and response body of /api/auth/my-services. */
    public record Selection(List<String> services, List<CustomPlatform> customPlatforms) {
        public static Selection empty() {
            return new Selection(List.of(), List.of());
        }
    }

    public static class InvalidSelectionException extends RuntimeException {
        public InvalidSelectionException(String message) {
            super(message);
        }
    }

    /**
     * Trims, drops duplicate services (keeping catalog order) and checks every rule.
     *
     * @throws InvalidSelectionException with a message fit to show the user
     */
    public static Selection validate(Selection request) {
        if (request == null) {
            return Selection.empty();
        }
        Set<String> picked = new HashSet<>();
        for (String key : Objects.requireNonNullElse(request.services(), List.<String>of())) {
            if (!StreamingServices.isKnown(key)) {
                throw new InvalidSelectionException("Unknown streaming service: " + key);
            }
            picked.add(key);
        }
        List<String> services = StreamingServices.all().stream()
                .map(StreamingServices.Service::key)
                .filter(picked::contains)
                .toList();

        List<CustomPlatform> customs = Objects.requireNonNullElse(request.customPlatforms(), List.of());
        if (customs.size() > MAX_CUSTOM) {
            throw new InvalidSelectionException("You can add up to " + MAX_CUSTOM + " of your own platforms");
        }
        Set<String> names = new HashSet<>();
        List<CustomPlatform> cleaned = new ArrayList<>();
        for (CustomPlatform c : customs) {
            if (c == null) {
                continue;
            }
            String name = c.name() == null ? "" : c.name().trim().replaceAll("\\s+", " ");
            String url = c.url() == null ? "" : c.url().trim();
            if (name.isEmpty()) {
                throw new InvalidSelectionException("Give each of your platforms a name");
            }
            if (name.length() > MAX_NAME_LENGTH) {
                throw new InvalidSelectionException("Platform names can be up to " + MAX_NAME_LENGTH + " characters");
            }
            if (!names.add(name.toLowerCase(Locale.ROOT))) {
                throw new InvalidSelectionException("You already added a platform called " + name);
            }
            if (url.length() > MAX_URL_LENGTH) {
                throw new InvalidSelectionException("Links can be up to " + MAX_URL_LENGTH + " characters");
            }
            if (!isValidUrl(url)) {
                throw new InvalidSelectionException("The link for " + name + " must be a full http:// or https:// address");
            }
            cleaned.add(new CustomPlatform(name, url));
        }
        return new Selection(services, List.copyOf(cleaned));
    }

    /** An absolute http(s) URL with a host; {title} may appear anywhere after the host. */
    static boolean isValidUrl(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }
        url = url.trim();
        String probe = url.replace(TITLE_PLACEHOLDER, "x");
        if (probe.chars().anyMatch(ch -> Character.isWhitespace(ch) || ch == '{' || ch == '}' || ch == '<' || ch == '>' || ch == '"')) {
            return false;
        }
        try {
            URI uri = new URI(probe);
            String scheme = uri.getScheme();
            if (!("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))
                    || uri.getHost() == null || uri.getHost().isBlank() || uri.getRawUserInfo() != null
                    || uri.getPort() > 65535) {
                return false;
            }
            // The placeholder can't be part of the scheme or host (it's for paths and queries)
            return url.startsWith(scheme + "://" + uri.getRawAuthority());
        } catch (URISyntaxException e) {
            return false;
        }
    }
}
