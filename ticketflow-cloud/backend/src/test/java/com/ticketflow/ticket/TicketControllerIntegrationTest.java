package com.ticketflow.ticket;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
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
class TicketControllerIntegrationTest {

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
    void ticketsCanBeCreatedListedAssignedUpdatedAndCommentedOn() throws Exception {
        User customer = createUser(UserRole.CUSTOMER);
        User otherCustomer = createUser(UserRole.CUSTOMER);
        User agent = createUser(UserRole.AGENT);
        User admin = createUser(UserRole.ADMIN);

        String customerToken = tokenFor(customer);
        String otherCustomerToken = tokenFor(otherCustomer);
        String agentToken = tokenFor(agent);
        String adminToken = tokenFor(admin);

        Long ticketId = createTicket(customerToken, "Checkout outage", "Payment checkout returns 500.", "URGENT");
        createTicket(otherCustomerToken, "Invoice request", "Need an invoice copy.", "LOW");

        mockMvc.perform(get("/api/tickets")
                        .param("status", "OPEN")
                        .param("priority", "URGENT")
                        .param("q", "checkout")
                        .param("sort", "createdAt")
                        .header(HttpHeaders.AUTHORIZATION, bearer(customerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(ticketId))
                .andExpect(jsonPath("$.content[0].slaDueAt", notNullValue()))
                .andExpect(jsonPath("$.totalElements").value(1));

        mockMvc.perform(patch("/api/tickets/{id}/assign", ticketId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "assignedAgentId": %d
                                }
                                """.formatted(agent.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignedAgent.id").value(agent.getId()));

        mockMvc.perform(get("/api/tickets")
                        .param("assignedAgentId", agent.getId().toString())
                        .header(HttpHeaders.AUTHORIZATION, bearer(agentToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(ticketId));

        mockMvc.perform(patch("/api/tickets/{id}/status", ticketId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(agentToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "RESOLVED"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"))
                .andExpect(jsonPath("$.resolvedAt", notNullValue()));

        mockMvc.perform(post("/api/tickets/{id}/comments", ticketId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(agentToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "body": "Patched the payment service configuration."
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticketId").value(ticketId))
                .andExpect(jsonPath("$.author.id").value(agent.getId()))
                .andExpect(jsonPath("$.body").value("Patched the payment service configuration."));

        mockMvc.perform(get("/api/tickets/{id}/comments", ticketId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(customerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].ticketId").value(ticketId));

        mockMvc.perform(patch("/api/tickets/{id}/assign", ticketId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(agentToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "assignedAgentId": %d
                                }
                                """.formatted(agent.getId())))
                .andExpect(status().isForbidden());
    }

    private Long createTicket(String token, String title, String description, String priority) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/tickets")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "%s",
                                  "description": "%s",
                                  "priority": "%s"
                                }
                                """.formatted(title, description, priority)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OPEN"))
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

