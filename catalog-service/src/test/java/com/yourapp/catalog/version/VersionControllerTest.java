package com.yourapp.catalog.version;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yourapp.catalog.version.controller.VersionController;
import com.yourapp.catalog.version.dto.request.CreateVersionRequest;
import com.yourapp.catalog.version.dto.response.VersionResponse;
import com.yourapp.catalog.version.service.VersionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class VersionControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private VersionService service;

    @InjectMocks
    private VersionController controller;

    private VersionResponse response;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        response = new VersionResponse();
        response.setId("ver-1");
        response.setSoftwareId("soft-1");
        response.setVersionNumber("1.0.0");
    }

    @Test
    void create_shouldReturnCreated() throws Exception {
        CreateVersionRequest request = new CreateVersionRequest();
        request.setVersionNumber("1.0.0");

        when(service.create(eq("soft-1"), any(CreateVersionRequest.class))).thenReturn(response);

        mockMvc.perform(post("/software/soft-1/versions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("ver-1"))
                .andExpect(jsonPath("$.versionNumber").value("1.0.0"));
    }

    @Test
    void findBySoftware_shouldReturnList() throws Exception {
        when(service.findBySoftwareId("soft-1")).thenReturn(List.of(response));

        mockMvc.perform(get("/software/soft-1/versions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].versionNumber").value("1.0.0"));
    }

    @Test
    void findById_shouldReturnOk() throws Exception {
        when(service.findById("ver-1")).thenReturn(response);

        mockMvc.perform(get("/versions/ver-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("ver-1"));
    }

    @Test
    void delete_shouldReturnNoContent() throws Exception {
        doNothing().when(service).delete("ver-1");

        mockMvc.perform(delete("/versions/ver-1"))
                .andExpect(status().isNoContent());
    }
}

