package com.example.library.integration;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

class PublicApiIntegrationTests extends PostgresBackedIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void booksEndpointReturnsSeededCatalog() throws Exception {
        mockMvc.perform(get("/api/books"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].title", hasItem("Domain-Driven Design")))
                .andExpect(jsonPath("$[*].title", hasItem("Release It!")))
                .andExpect(jsonPath("$[0].availability").exists());
    }

    @Test
    void filtersEndpointReturnsSeededCategoriesAndTags() throws Exception {
        mockMvc.perform(get("/api/books/filters"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categories", hasItem("Architecture")))
                .andExpect(jsonPath("$.tags", not(org.hamcrest.Matchers.empty())));
    }

    @Test
    void discoveryEndpointReturnsWeeklyDiscoverySections() throws Exception {
        mockMvc.perform(get("/api/discovery"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recommendations[*].title", hasItem("Domain-Driven Design")))
                .andExpect(jsonPath("$.mostBorrowedThisWeek[*].title").isNotEmpty())
                .andExpect(jsonPath("$.mostViewedThisWeek[*].title").isNotEmpty());
    }

    @Test
    void publicBranchesEndpointReturnsActiveBranches() throws Exception {
        mockMvc.perform(get("/api/branches/public"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].code", hasItem("CENTRAL")))
                .andExpect(jsonPath("$[*].code", hasItem("EAST")))
                .andExpect(jsonPath("$[*].code", hasItem("HQ")));
    }

    @Test
    void upcomingBooksEndpointReturnsPlannedArrivals() throws Exception {
        mockMvc.perform(get("/api/upcoming-books"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").isNotEmpty())
                .andExpect(jsonPath("$[0].expectedAt").isNotEmpty());
    }
}
