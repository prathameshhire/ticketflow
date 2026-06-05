package com.ticketflow.alert;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketflow.auth.JwtService;
import com.ticketflow.auth.UserPrincipal;
import com.ticketflow.user.User;
import com.ticketflow.user.UserRepository;
import com.ticketflow.user.UserRole;

@SpringBootTest
@AutoConfigureMockMvc
class AlertDashboardIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Test
    void alertWorkflowAndDashboardSummaryWorkThroughRestApis() throws Exception {
        User customer = createUser(UserRole.CUSTOMER);
        User agent = createUser(UserRole.AGENT);
        User admin = createUser(UserRole.ADMIN);

        String customerToken = tokenFor(customer);
        String agentToken = tokenFor(agent);
        String adminToken = tokenFor(admin);

        mockMvc.perform(get("/api/dashboard/summary")
                        .header(HttpHeaders.AUTHORIZATION, bearer(customerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalTickets").value(0));

        Long ticketId = createTicket(customerToken);

        mockMvc.perform(get("/api/dashboard/summary")
                        .header(HttpHeaders.AUTHORIZATION, bearer(customerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalTickets").value(1))
                .andExpect(jsonPath("$.openCount").value(1))
                .andExpect(jsonPath("$.ticketsByPriority.URGENT").value(1))
                .andExpect(jsonPath("$.ticketsByStatus.OPEN").value(1));

        mockMvc.perform(patch("/api/tickets/{id}/assign", ticketId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "assignedAgentId": %d
                                }
                                """.formatted(agent.getId())))
                .andExpect(status().isOk());

        JsonNode assignmentAlert = waitForAlert(agentToken, "ASSIGNMENT");
        assertNotNull(assignmentAlert.get("id"));

        mockMvc.perform(patch("/api/tickets/{id}/status", ticketId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(agentToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "RESOLVED"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resolvedAt", notNullValue()));

        JsonNode statusAlert = waitForAlert(customerToken, "STATUS_CHANGE");
        assertEquals("STATUS_CHANGE", statusAlert.get("type").asText());

        mockMvc.perform(post("/api/tickets/{id}/comments", ticketId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(agentToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "body": "Resolution details posted."
                                }
                                """))
                .andExpect(status().isOk());

        JsonNode customerCommentAlert = waitForAlert(customerToken, "COMMENT");
        Long commentAlertId = customerCommentAlert.get("id").asLong();

        mockMvc.perform(patch("/api/alerts/{id}/read", commentAlertId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(customerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.readFlag").value(true));

        mockMvc.perform(post("/api/tickets/{id}/comments", ticketId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(customerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "body": "Thanks for the fix."
                                }
                                """))
                .andExpect(status().isOk());

        waitForAlert(agentToken, "COMMENT");

        mockMvc.perform(get("/api/alerts")
                        .header(HttpHeaders.AUTHORIZATION, bearer(agentToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(2))));

        mockMvc.perform(patch("/api/alerts/read-all")
                        .header(HttpHeaders.AUTHORIZATION, bearer(agentToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.updatedCount", greaterThanOrEqualTo(2)));

        mockMvc.perform(get("/api/dashboard/summary")
                        .header(HttpHeaders.AUTHORIZATION, bearer(agentToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalTickets").value(1))
                .andExpect(jsonPath("$.resolvedCount").value(1))
                .andExpect(jsonPath("$.agentWorkload[0].agentId").value(agent.getId()))
                .andExpect(jsonPath("$.agentWorkload[0].totalAssigned").value(1))
                .andExpect(jsonPath("$.averageResolutionTimeHours", notNullValue()));
    }

    private JsonNode waitForAlert(String token, String type) throws Exception {
        long deadline = System.currentTimeMillis() + 5_000;
        JsonNode latestResponse = null;
        while (System.currentTimeMillis() < deadline) {
            MvcResult result = mockMvc.perform(get("/api/alerts")
                            .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                    .andExpect(status().isOk())
                    .andReturn();

            latestResponse = objectMapper.readTree(result.getResponse().getContentAsString());
            for (JsonNode alert : latestResponse) {
                if (type.equals(alert.get("type").asText())) {
                    return alert;
                }
            }

            Thread.sleep(100);
        }

        fail("Timed out waiting for alert type " + type + ". Last response: " + latestResponse);
        return null;
    }

    private Long createTicket(String token) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/tickets")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Priority production incident",
                                  "description": "Customers cannot submit orders.",
                                  "priority": "URGENT"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        return response.get("id").asLong();
    }

    private User createUser(UserRole role) {
        String email = role.name().toLowerCase() + "-" + UUID.randomUUID() + "@example.com";
        User user = User.create(role.name() + " User", email, passwordEncoder.encode("password123"), role);
        return userRepository.save(user);
    }

    private String tokenFor(User user) {
        return jwtService.generateToken(UserPrincipal.from(user));
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}

