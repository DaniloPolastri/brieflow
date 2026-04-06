package com.briefflow.unit.entity;

import com.briefflow.entity.Workspace;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WorkspaceTest {

    @Test
    void should_generateSlug_when_prePersist() {
        Workspace workspace = new Workspace();
        workspace.setName("Agencia Criativa Digital");
        workspace.onCreate();

        assertEquals("agencia-criativa-digital", workspace.getSlug());
        assertNotNull(workspace.getCreatedAt());
        assertNotNull(workspace.getUpdatedAt());
    }

    @Test
    void should_handleSpecialChars_when_generatingSlug() {
        Workspace workspace = new Workspace();
        workspace.setName("Café & Design Ltda.");
        workspace.onCreate();

        assertEquals("caf-design-ltda", workspace.getSlug());
    }
}
