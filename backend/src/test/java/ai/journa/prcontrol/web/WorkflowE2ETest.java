package ai.journa.prcontrol.web;

import ai.journa.prcontrol.dto.LoginRequest;
import ai.journa.prcontrol.dto.ManualArticleRequest;
import ai.journa.prcontrol.dto.OutreachComposeRequest;
import ai.journa.prcontrol.dto.RegisterRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class WorkflowE2ETest {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("prcontrol")
            .withUsername("prcontrol")
            .withPassword("prcontrol");

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void searchToSendWorkflow() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setEmail("admin@example.com");
        registerRequest.setPassword("password");

        String registerResponse = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String token = objectMapper.readTree(registerResponse).get("token").asText();

        String searchResponse = mockMvc.perform(get("/api/articles")
                .header("Authorization", "Bearer " + token)
                .param("beat", "Taxation")
                .param("timeframe", "24h"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode articles = objectMapper.readTree(searchResponse).get("items");
        long articleId = articles.get(0).get("id").asLong();

        mockMvc.perform(get("/api/articles/{id}", articleId)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        String journalistsResponse = mockMvc.perform(get("/api/journalists/search")
                .header("Authorization", "Bearer " + token)
                .param("beat", "Taxation"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        long journalistId = objectMapper.readTree(journalistsResponse).get(0).get("id").asLong();

        String templatesResponse = mockMvc.perform(get("/api/templates")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        long templateId = objectMapper.readTree(templatesResponse).get(0).get("id").asLong();

        ManualArticleRequest manualArticleRequest = new ManualArticleRequest();
        manualArticleRequest.setBeat("Taxation");
        manualArticleRequest.setHeadline("Manual headline");
        manualArticleRequest.setUrl("https://example.com/manual");

        mockMvc.perform(post("/api/articles/manual")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(manualArticleRequest)))
                .andExpect(status().isOk());

        OutreachComposeRequest outreachRequest = new OutreachComposeRequest();
        outreachRequest.setArticleId(articleId);
        outreachRequest.setJournalistId(journalistId);
        outreachRequest.setTemplateId(templateId);
        outreachRequest.setFinalSubject("Story idea");
        outreachRequest.setFinalBody("Hello there");

        mockMvc.perform(post("/api/outreach/send")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(outreachRequest)))
                .andExpect(status().isOk());

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("admin@example.com");
        loginRequest.setPassword("password");
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk());
    }
}
