package com.Toine.animeCountdownBackend.services.watch;

import com.Toine.animeCountdownBackend.services.watch.MyServices.CustomPlatform;
import com.Toine.animeCountdownBackend.services.watch.MyServices.InvalidSelectionException;
import com.Toine.animeCountdownBackend.services.watch.MyServices.Selection;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MyServicesTest {

    @Test
    void servicesAreDedupedAndPutInCatalogOrder() {
        Selection clean = MyServices.validate(new Selection(List.of("netflix", "crunchyroll", "netflix"), null));

        assertEquals(List.of("crunchyroll", "netflix"), clean.services());
        assertEquals(List.of(), clean.customPlatforms());
    }

    @Test
    void unknownServiceIsRejected() {
        var e = assertThrows(InvalidSelectionException.class,
                () -> MyServices.validate(new Selection(List.of("crunchyroll", "piratebay"), List.of())));
        assertTrue(e.getMessage().contains("piratebay"));
    }

    @Test
    void customPlatformsAreTrimmed() {
        Selection clean = MyServices.validate(new Selection(List.of(),
                List.of(new CustomPlatform("  My   Library ", " https://library.example.org/search?q={title} "))));

        assertEquals(List.of(new CustomPlatform("My Library", "https://library.example.org/search?q={title}")),
                clean.customPlatforms());
    }

    @Test
    void urlsMustBeAbsoluteHttpOrHttps() {
        assertTrue(MyServices.isValidUrl("https://example.com"));
        assertTrue(MyServices.isValidUrl("http://example.com/watch/{title}"));
        assertTrue(MyServices.isValidUrl("HTTPS://Example.com/path?a=1&q={title}"));

        assertFalse(MyServices.isValidUrl("javascript:alert(1)"));
        assertFalse(MyServices.isValidUrl("ftp://example.com"));
        assertFalse(MyServices.isValidUrl("example.com"));
        assertFalse(MyServices.isValidUrl("https://"));
        assertFalse(MyServices.isValidUrl("https://{title}.com"));
        assertFalse(MyServices.isValidUrl("https://user:pass@example.com"));
        assertFalse(MyServices.isValidUrl("https://example.com/a b"));
        assertFalse(MyServices.isValidUrl("https://example.com/{other}"));
        assertFalse(MyServices.isValidUrl("data:text/html,hi"));
        assertFalse(MyServices.isValidUrl(""));
    }

    @Test
    void badCustomPlatformsAreRejectedWithAReadableMessage() {
        assertThrows(InvalidSelectionException.class, () -> custom("", "https://a.example"));
        assertThrows(InvalidSelectionException.class, () -> custom("x".repeat(MyServices.MAX_NAME_LENGTH + 1), "https://a.example"));
        assertThrows(InvalidSelectionException.class, () -> custom("Site", "javascript:alert(1)"));
        assertThrows(InvalidSelectionException.class,
                () -> custom("Site", "https://a.example/" + "x".repeat(MyServices.MAX_URL_LENGTH)));
    }

    @Test
    void duplicateNamesAreRejected() {
        assertThrows(InvalidSelectionException.class, () -> MyServices.validate(new Selection(List.of(), List.of(
                new CustomPlatform("Plex", "https://a.example"), new CustomPlatform("plex", "https://b.example")))));
    }

    @Test
    void tooManyCustomPlatformsAreRejected() {
        List<CustomPlatform> many = new ArrayList<>();
        for (int i = 0; i <= MyServices.MAX_CUSTOM; i++) {
            many.add(new CustomPlatform("Site " + i, "https://s" + i + ".example"));
        }
        assertThrows(InvalidSelectionException.class, () -> MyServices.validate(new Selection(List.of(), many)));
    }

    @Test
    void nullRequestIsEmpty() {
        assertEquals(Selection.empty(), MyServices.validate(null));
    }

    private static Selection custom(String name, String url) {
        return MyServices.validate(new Selection(List.of(), List.of(new CustomPlatform(name, url))));
    }
}
