package com.Toine.animeCountdownBackend.services.watch;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Name matching and link validation against my-services-vectors.json, which the frontend's
 * scripts/check-my-services.ts also checks, so both sides agree.
 */
class MyServicesVectorsTest {

    private static JsonNode vectors() throws Exception {
        try (InputStream in = MyServicesVectorsTest.class.getResourceAsStream("/my-services-vectors.json")) {
            return new ObjectMapper().readTree(in);
        }
    }

    @Test
    void namesSimplifyLikeTheFrontend() throws Exception {
        for (JsonNode v : vectors().get("names")) {
            assertEquals(v.get(1).asText(), StreamingServices.simplify(v.get(0).asText()), v.get(0).asText());
        }
    }

    @Test
    void linksValidateLikeTheFrontend() throws Exception {
        for (JsonNode v : vectors().get("urls")) {
            assertEquals(v.get(1).asBoolean(), MyServices.isValidUrl(v.get(0).asText()), v.get(0).asText());
        }
    }
}
