package store.artistictech.apiaggregator.controller;

import java.util.Map;

import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import store.artistictech.apiaggregator.model.AggregatedResponse;
import store.artistictech.apiaggregator.model.AggregationOptions;
import store.artistictech.apiaggregator.model.ServiceStatus;
import store.artistictech.apiaggregator.service.AggregationService;

@WebMvcTest(AggregationController.class)
class AggregationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AggregationService aggregationService;

    @Test
    void aggregate_returnsAggregatedResponseAsJson() throws Exception {
        AggregatedResponse response = new AggregatedResponse(
                "u1", false,
                Map.of("profile", Map.of("userId", "u1")),
                Map.of("profile", new ServiceStatus(true, null, 5)));

        when(aggregationService.aggregate(eq("u1"), any(AggregationOptions.class))).thenReturn(response);

        mockMvc.perform(get("/api/aggregate/u1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("u1"))
                .andExpect(jsonPath("$.partialFailure").value(false))
                .andExpect(jsonPath("$.data.profile.userId").value("u1"));
    }

    @Test
    void aggregate_reflectsPartialFailureFlagFromService() throws Exception {
        AggregatedResponse response = new AggregatedResponse("u2", true, Map.of(), Map.of());
        when(aggregationService.aggregate(eq("u2"), any(AggregationOptions.class))).thenReturn(response);

        mockMvc.perform(get("/api/aggregate/u2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.partialFailure").value(true));
    }

    @Test
    void aggregate_translatesQueryParamsIntoAggregationOptions() throws Exception {
        AggregatedResponse response = new AggregatedResponse("u3", false, Map.of(), Map.of());
        when(aggregationService.aggregate(eq("u3"), any(AggregationOptions.class))).thenReturn(response);

        mockMvc.perform(get("/api/aggregate/u3")
                        .param("profileDelayMs", "10")
                        .param("failOrders", "true"))
                .andExpect(status().isOk());

        verify(aggregationService).aggregate(eq("u3"), argThat(options ->
                Long.valueOf(10L).equals(options.forService("profile").delayMs())
                        && Boolean.TRUE.equals(options.forService("orders").fail())));
    }
}
