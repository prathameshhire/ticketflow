package com.ticketflow.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
class SecurityRoleRestrictionsTest {

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
    void roleRestrictionsRejectForbiddenTicketOperations() throws Exception {
        User customer = createUser(UserRole.CUSTOMER);
        User agent = createUser(UserRole.AGENT);
        User admin = createUser(UserRole.ADMIN);

        String customerToken = tokenFor(customer);
        String agentToken = tokenFor(agent);
        String adminToken = tokenFor(admin);
        Long ticketId = createTicket(customerToken);

        mockMvc.perform(patch("/api/tickets/{id}/assign", ticketId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(customerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "assignedAgentId": %d
                                }
                                """.formatted(agent.getId())))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/tickets/{id}/status", ticketId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(agentToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "IN_PROGRESS"
                                }
                                """))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/tickets/{id}/assign", ticketId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "assignedAgentId": %d
                                }
                                """.formatted(agent.getId())))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/tickets/{id}/status", ticketId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(agentToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "IN_PROGRESS"
                                }
                                """))
                .andExpect(status().isOk());
    }

    private Long createTicket(String token) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/tickets")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Restricted operation test",
                                  "description": "Used to verify role boundaries.",
                                  "priority": "HIGH"
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

