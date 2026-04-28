package com.example.ratelimiting;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("demo")
public class PingAuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void testPingIsAccessible() throws Exception {
        mockMvc.perform(get("/ping"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.status").value("ok"));
    }

    @Test
    public void testLoginAndDashboard() throws Exception {
        // 1. Login
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
               .contentType(MediaType.APPLICATION_JSON)
               .content("{\"username\":\"demo\",\"password\":\"password\"}"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.access_token").exists())
               .andReturn();

        String response = loginResult.getResponse().getContentAsString();
        Map<String, Object> map = objectMapper.readValue(response, Map.class);
        String token = "Bearer " + map.get("access_token");

        // 2. Verify Dashboard Policies
        mockMvc.perform(get("/api/dashboard/policies")
               .header("Authorization", token))
               .andExpect(status().isOk());

        // 3. Verify Dashboard Analytics
        mockMvc.perform(get("/api/dashboard/analytics")
               .header("Authorization", token))
               .andExpect(status().isOk());
    }
}
