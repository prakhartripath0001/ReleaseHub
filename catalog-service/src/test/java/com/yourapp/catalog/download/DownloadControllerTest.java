package com.yourapp.catalog.download;

import com.yourapp.catalog.download.controller.DownloadController;
import com.yourapp.catalog.download.dto.response.DownloadResponse;
import com.yourapp.catalog.download.service.DownloadService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class DownloadControllerTest {

    private MockMvc mockMvc;

    @Mock
    private DownloadService service;

    @InjectMocks
    private DownloadController controller;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void download_shouldReturnDownloadResponse() throws Exception {
        DownloadResponse response = new DownloadResponse("https://example.com/app.zip", "ver-1", "Download recorded successfully");
        when(service.recordAndResolve("ver-1", "user-123")).thenReturn(response);

        mockMvc.perform(get("/download/ver-1")
                        .header("X-User-Id", "user-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.versionId").value("ver-1"))
                .andExpect(jsonPath("$.downloadUrl").value("https://example.com/app.zip"));
    }
}

