package io.arex.inst.httpclient.webclient.v5;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class WebClientModuleInstrumentationTest {

    @Test
    void instrumentationTypes() {
        assertNotNull(new WebClientModuleInstrumentation().instrumentationTypes());
    }
}