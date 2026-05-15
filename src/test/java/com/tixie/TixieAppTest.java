package com.tixie;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class TixieAppTest {

    @Test
    void appClass_canBeInstantiated() {
        assertNotNull(new TixieApp());
    }
}
