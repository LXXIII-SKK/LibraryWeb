package com.example.library;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ModulithStructureTests {

    @Test
    void verifiesApplicationModules() {
        ApplicationModules.of(LibraryApplication.class).verify();
    }
}
