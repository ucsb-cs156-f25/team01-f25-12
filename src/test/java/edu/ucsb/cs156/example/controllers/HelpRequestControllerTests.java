package edu.ucsb.cs156.example.controllers;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edu.ucsb.cs156.example.ControllerTestCase;
import edu.ucsb.cs156.example.entities.HelpRequest;
import edu.ucsb.cs156.example.repositories.HelpRequestRepository;
import edu.ucsb.cs156.example.repositories.UserRepository;
import edu.ucsb.cs156.example.testconfig.TestConfig;
import java.time.LocalDateTime;
import java.util.List;
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
  public void logged_out_users_cannot_get_all() throws Exception {
    mockMvc
        .perform(get("/api/helprequests/all"))
        .andExpect(status().is(403)); // logged out users can't get all
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void logged_in_users_can_get_all() throws Exception {
    mockMvc.perform(get("/api/helprequests/all")).andExpect(status().is(200)); // logged
  }



  // Authorization tests for /api/ucsbdates/post
  // (Perhaps should also have these for put and delete)

  @Test
  public void logged_out_users_cannot_post() throws Exception {
    mockMvc.perform(post("/api/helprequests/post")).andExpect(status().is(403));
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void logged_in_regular_users_cannot_post() throws Exception {
    mockMvc
        .perform(post("/api/helprequests/post"))
        .andExpect(status().is(403)); // only admins can post
  }



    @WithMockUser(roles = {"USER"})
    @Test
    public void logged_in_user_can_get_all_helpRequests() throws Exception {

        // arrange
        LocalDateTime ldt1 = LocalDateTime.parse("2022-01-03T00:00:00");


        HelpRequest helpRequest1 =
            HelpRequest.builder()
                .requesterEmail("email")
                .teamId("team")
                .tableOrBreakoutRoom("room")
                .requestTime(LocalDateTime.now())
                .explanation("help")
                .solved(false)
                .build();

        LocalDateTime ldt2 = LocalDateTime.parse("2022-03-11T00:00:00");

        HelpRequest helpRequest2 =
            HelpRequest.builder()
                .requesterEmail("email2")
                .teamId("team2")
                .tableOrBreakoutRoom("room1")
                .requestTime(LocalDateTime.now())
                .explanation("help me too")
                .solved(false)
                .build();

        ArrayList<HelpRequest> expectedRequests = new ArrayList<>();
        expectedRequests.addAll(Arrays.asList(helpRequest1, helpRequest2));

        when(helpRequestRepository.findAll()).thenReturn(expectedRequests);

        // act
        MvcResult response =
            mockMvc.perform(get("/api/helprequests/all")).andExpect(status().isOk()).andReturn();

        // assert

        verify(helpRequestRepository, times(1)).findAll();
        String expectedJson = mapper.writeValueAsString(expectedRequests);
        String responseString = response.getResponse().getContentAsString();
        assertEquals(expectedJson, responseString);
    }

  @WithMockUser(roles = {"ADMIN", "USER"})
  @Test
  public void an_admin_user_can_post_a_new_helprequest() throws Exception {
    // arrange

    LocalDateTime ldt1 = LocalDateTime.parse("2022-01-03T00:00:00");

    HelpRequest helpRequest1 =
        HelpRequest.builder()
            .requesterEmail("email")
            .teamId("team")
            .tableOrBreakoutRoom("room")
            .requestTime(LocalDateTime.now())
            .explanation("help")
            .solved(false)
            .build();

    when(helpRequestRepository.save(eq(helpRequest1))).thenReturn(helpRequest1);

    // act
  MvcResult response =
      mockMvc
          .perform(
              post("/api/helprequests/post")
                  .param("requesterEmail", "email")
                  .param("teamId", "team")
                  .param("tableOrBreakoutRoom", "room")
                  .param("explanation", "help")
                  .param("solved", "false")
                  .param("requestTime", "2025-12-23T00:00:00")
                  .with(csrf()))
          .andExpect(status().isOk())
          .andReturn();


    // assert
    verify(helpRequestRepository, times(1)).save(helpRequest1);
    String expectedJson = mapper.writeValueAsString(helpRequest1);
    String responseString = response.getResponse().getContentAsString();
    assertEquals(expectedJson, responseString);
  } 
}
