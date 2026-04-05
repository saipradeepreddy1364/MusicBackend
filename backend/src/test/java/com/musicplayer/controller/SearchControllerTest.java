package com.musicplayer.controller;

import com.musicplayer.service.JioSaavnService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SearchController.class)
class SearchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JioSaavnService jiosaavnService;

    @Test
    void searchAll_shouldReturn200_whenQueryPresent() throws Exception {
        when(jiosaavnService.search(eq("Arijit"), anyInt(), anyInt()))
                .thenReturn(Map.of("results", "mocked"));

        mockMvc.perform(get("/search").param("q", "Arijit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    void searchAll_shouldReturn400_whenQueryMissing() throws Exception {
        mockMvc.perform(get("/search"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void searchSongs_shouldReturn200() throws Exception {
        when(jiosaavnService.searchSongs(anyString(), anyInt(), anyInt()))
                .thenReturn(Map.of("songs", "mocked"));

        mockMvc.perform(get("/search/songs").param("q", "Kesariya"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
