package edu.ucsb.cs156.example.controllers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edu.ucsb.cs156.example.ControllerTestCase;
import edu.ucsb.cs156.example.repositories.HelpRequestRepository;
import edu.ucsb.cs156.example.repositories.UserRepository;
import edu.ucsb.cs156.example.testconfig.TestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;

@WebMvcTest(controllers = HelpRequestController.class)
@Import(TestConfig.class)
public class HelpRequestControllerTests extends ControllerTestCase {
  @MockBean HelpRequestRepository helpRequestRepository;

  @MockBean UserRepository userRepository;

  // Authorization tests for /api/helpRequests/admin/all

  @Test
  void logged_out_users_cannot_get_all() throws Exception {
    mockMvc.perform(get("/api/helprequests/all")).andExpect(status().isForbidden()); // 403
  }

  @WithMockUser(roles = {"USER"})
  @Test
  void logged_in_users_can_get_all_and_see_data() throws Exception {
    var t = LocalDateTime.parse("2025-12-23T10:00:00");
    var h =
        HelpRequest.builder()
            .requesterEmail("a@b.com")
            .teamId("T1")
            .tableOrBreakoutRoom("Table1")
            .explanation("x")
            .solved(false)
            .requestTime(t)
            .build();

    when(helpRequestRepository.findAll()).thenReturn(List.of(h));

    mockMvc
        .perform(get("/api/helprequests/all"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].requesterEmail", is("a@b.com")))
        .andExpect(jsonPath("$[0].teamId", is("T1")))
        .andExpect(jsonPath("$[0].tableOrBreakoutRoom", is("Table1")))
        .andExpect(jsonPath("$[0].explanation", is("x")))
        .andExpect(jsonPath("$[0].solved", is(false)));

    verify(helpRequestRepository).findAll();
  }

  // ===== POST /api/helprequests/post =====

  @Test
  void logged_out_users_cannot_post() throws Exception {
    mockMvc.perform(post("/api/helprequests/post")).andExpect(status().isForbidden()); // 403
  }

  @WithMockUser(roles = {"USER"})
  @Test
  void logged_in_regular_users_cannot_post() throws Exception {
    mockMvc
        .perform(post("/api/helprequests/post"))
        .andExpect(status().isForbidden()); // 403 (admin-only)
  }

  @WithMockUser(roles = {"ADMIN", "USER"})
  @Test
  void admin_can_post_and_entity_is_saved() throws Exception {
    // Return whatever we save (common Mockito pattern)
    when(helpRequestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    mockMvc
        .perform(
            post("/api/helprequests/post")
                .param("requesterEmail", "a@b.com")
                .param("teamId", "T1")
                .param("tableOrBreakoutRoom", "Table1")
                .param("explanation", "x")
                .param("solved", "false")
                .param("requestTime", "2025-12-23T10:00:00")
                .with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.requesterEmail", is("a@b.com")))
        .andExpect(jsonPath("$.teamId", is("T1")))
        .andExpect(jsonPath("$.tableOrBreakoutRoom", is("Table1")))
        .andExpect(jsonPath("$.explanation", is("x")))
        .andExpect(jsonPath("$.solved", is(false)));

    verify(helpRequestRepository)
        .save(
            argThat(
                h ->
                    "a@b.com".equals(h.getRequesterEmail())
                        && "T1".equals(h.getTeamId())
                        && "Table1".equals(h.getTableOrBreakoutRoom())
                        && "x".equals(h.getExplanation())
                        && !h.isSolved()
                        && LocalDateTime.parse("2025-12-23T10:00:00").equals(h.getRequestTime())));
  }
}
