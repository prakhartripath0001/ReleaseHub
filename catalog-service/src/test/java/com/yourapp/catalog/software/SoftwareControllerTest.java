package com.yourapp.catalog.software;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yourapp.catalog.common.dto.PageResponse;
import com.yourapp.catalog.software.controller.SoftwareController;
import com.yourapp.catalog.software.dto.request.CreateSoftwareRequest;
import com.yourapp.catalog.software.dto.request.UpdateSoftwareRequest;
import com.yourapp.catalog.software.dto.response.SoftwareResponse;
import com.yourapp.catalog.software.service.SoftwareService;
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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class SoftwareControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private SoftwareService service;

    @InjectMocks
    private SoftwareController controller;

    private SoftwareResponse response;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        response = new SoftwareResponse();
        response.setId("soft-123");
        response.setName("Postman");
        response.setCategory("developer-tools");
    }

    @Test
    void create_shouldReturnCreated() throws Exception {
        CreateSoftwareRequest request = new CreateSoftwareRequest();
        request.setName("Postman");
        request.setPublisherId("pub-1");

        when(service.create(any(CreateSoftwareRequest.class))).thenReturn(response);

        mockMvc.perform(post("/software")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("soft-123"))
                .andExpect(jsonPath("$.name").value("Postman"));
    }

    @Test
    void findById_shouldReturnOk() throws Exception {
        when(service.findById("soft-123")).thenReturn(response);

        mockMvc.perform(get("/software/soft-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("soft-123"));
    }

    @Test
    void findAll_shouldReturnPage() throws Exception {
        PageResponse<SoftwareResponse> page = new PageResponse<>();
        page.setContent(List.of(response));
        page.setTotalElements(1);
        page.setPage(0);
        page.setSize(20);

        when(service.findAll(any(), any(), any(), anyInt(), anyInt())).thenReturn(page);

        mockMvc.perform(get("/software?category=developer-tools"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Postman"));
    }

    @Test
    void update_shouldReturnUpdated() throws Exception {
        UpdateSoftwareRequest request = new UpdateSoftwareRequest();
        request.setName("Postman Updated");

        when(service.update(eq("soft-123"), any(UpdateSoftwareRequest.class))).thenReturn(response);

        mockMvc.perform(put("/software/soft-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("soft-123"));
    }

    @Test
    void delete_shouldReturnNoContent() throws Exception {
        doNothing().when(service).delete("soft-123");

        mockMvc.perform(delete("/software/soft-123"))
                .andExpect(status().isNoContent());
    }
}

