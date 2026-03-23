package com.example.library.integration;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

class IdentityApiIntegrationTests extends PostgresBackedIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void profileEndpointResolvesSeededMemberFromJwtSubject() throws Exception {
        mockMvc.perform(get("/api/profile").with(readerJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("reader"))
                .andExpect(jsonPath("$.role").value("MEMBER"))
                .andExpect(jsonPath("$.branch.code").value("CENTRAL"))
                .andExpect(jsonPath("$.permissions", hasItem("LOAN_SELF_READ")));
    }

    @Test
    void adminCanListUsersFromDatabase() throws Exception {
        mockMvc.perform(get("/api/users").with(adminJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].username", hasItem("admin")))
                .andExpect(jsonPath("$[*].username", hasItem("reader")))
                .andExpect(jsonPath("$[*].username", hasItem("branch.librarian")));
    }

    @Test
    void branchManagerListIsScopedToManageableBranchUsers() throws Exception {
        mockMvc.perform(get("/api/users").with(branchManagerJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].role", everyItem(allOf(
                        not(startsWith("ADMIN")),
                        not(startsWith("BRANCH_MANAGER")),
                        not(startsWith("AUDITOR"))))))
                .andExpect(jsonPath("$[*].username", hasItem("branch.librarian")))
                .andExpect(jsonPath("$[*].username", hasItem("reader")))
                .andExpect(jsonPath("$[*].username", not(hasItem("admin"))))
                .andExpect(jsonPath("$[*].username", not(hasItem("east.manager"))));
    }
}
