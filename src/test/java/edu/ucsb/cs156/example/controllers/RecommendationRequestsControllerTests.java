package edu.ucsb.cs156.example.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edu.ucsb.cs156.example.ControllerTestCase;
import edu.ucsb.cs156.example.entities.RecommendationRequest;
import edu.ucsb.cs156.example.repositories.RecommendationRequestRepository;
import edu.ucsb.cs156.example.repositories.UserRepository;
import edu.ucsb.cs156.example.testconfig.TestConfig;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MvcResult;

@WebMvcTest(controllers = RecommendationRequestsController.class)
@Import(TestConfig.class)
public class RecommendationRequestsControllerTests extends ControllerTestCase {

  @MockBean RecommendationRequestRepository recommendationRequestRepository;

  @MockBean UserRepository userRepository;

  // Authorization tests for /api/recommendationrequests/admin/all

  @Test
  public void logged_out_users_cannot_get_all() throws Exception {
    mockMvc
        .perform(get("/api/recommendationrequests/all"))
        .andExpect(status().is(403)); // logged out users can't get all
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void logged_in_users_can_get_all() throws Exception {
    mockMvc.perform(get("/api/recommendationrequests/all")).andExpect(status().is(200)); // logged
  }

  // Authorization tests for /api/recommendationrequests/post
  // (Perhaps should also have these for put and delete)

  @Test
  public void logged_out_users_cannot_post() throws Exception {
    mockMvc.perform(post("/api/recommendationrequests/post")).andExpect(status().is(403));
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void logged_in_regular_users_cannot_post() throws Exception {
    mockMvc
        .perform(post("/api/recommendationrequests/post"))
        .andExpect(status().is(403)); // only admins can post
  }

  // // Tests with mocks for database actions

  @WithMockUser(roles = {"USER"})
  @Test
  public void logged_in_user_can_get_all_recommendation_requests() throws Exception {

    // arrange
    LocalDateTime ldt1 = LocalDateTime.parse("2022-01-03T00:00:00");
    LocalDateTime ldt2 = LocalDateTime.parse("2022-02-04T00:00:00");

    RecommendationRequest recommendationRequest1 =
        RecommendationRequest.builder()
            .requesterEmail("ja@ucsb.edu")
            .professorEmail("pconrad@ucsb.edu")
            .explanation("I need a recommendation")
            .dateRequested(ldt1)
            .dateNeeded(ldt2)
            .done(false)
            .build();

    LocalDateTime ldt3 = LocalDateTime.parse("2025-03-11T00:00:00");
    LocalDateTime ldt4 = LocalDateTime.parse("2052-08-11T00:00:00");

    RecommendationRequest recommendationRequest2 =
        RecommendationRequest.builder()
            .requesterEmail("alex@ucsb.edu")
            .professorEmail("richert@ucsb.edu")
            .explanation("Grad school recommendation")
            .dateRequested(ldt3)
            .dateNeeded(ldt4)
            .done(false)
            .build();

    ArrayList<RecommendationRequest> expectedRecommendationRequests = new ArrayList<>();
    expectedRecommendationRequests.addAll(
        Arrays.asList(recommendationRequest1, recommendationRequest2));

    when(recommendationRequestRepository.findAll()).thenReturn(expectedRecommendationRequests);

    // act
    MvcResult response =
        mockMvc
            .perform(get("/api/recommendationrequests/all"))
            .andExpect(status().isOk())
            .andReturn();

    // assert

    verify(recommendationRequestRepository, times(1)).findAll();
    String expectedJson = mapper.writeValueAsString(expectedRecommendationRequests);
    String responseString = response.getResponse().getContentAsString();
    assertEquals(expectedJson, responseString);
  }

  @WithMockUser(roles = {"ADMIN", "USER"})
  @Test
  public void an_admin_user_can_post_a_new_recommendation_request() throws Exception {
    // arrange

    LocalDateTime ldt1 = LocalDateTime.parse("2022-01-03T00:00:00");
    LocalDateTime ldt2 = LocalDateTime.parse("2022-02-04T00:00:00");

    RecommendationRequest recommendationRequest1 =
        RecommendationRequest.builder()
            .requesterEmail("ja@ucsb.edu")
            .professorEmail("pconrad@ucsb.edu")
            .explanation("recommendation")
            .dateRequested(ldt1)
            .dateNeeded(ldt2)
            .done(false)
            .build();

    // when(recommendationRequestRepository.save(eq(recommendationRequest1)))
    //     .thenReturn(recommendationRequest1);

    when(recommendationRequestRepository.save(any(RecommendationRequest.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    // act
    MvcResult response =
        mockMvc
            .perform(
                post("/api/recommendationrequests/post?requesterEmail=ja@ucsb.edu&professorEmail=pconrad@ucsb.edu&explanation=recommendation&dateRequested=2022-01-03T00:00:00&dateNeeded=2022-02-04T00:00:00&done=false")
                    .with(csrf()))
            .andExpect(status().isOk())
            .andReturn();

    // assert
    verify(recommendationRequestRepository, times(1))
        .save(
            argThat(
                (RecommendationRequest r) ->
                    r.getRequesterEmail().equals("ja@ucsb.edu")
                        && r.getProfessorEmail().equals("pconrad@ucsb.edu")
                        && r.getExplanation().equals("recommendation")
                        && r.getDateRequested().equals(ldt1)
                        && r.getDateNeeded().equals(ldt2)
                        && r.getDone() == false));

    RecommendationRequest expected =
        RecommendationRequest.builder()
            .requesterEmail("ja@ucsb.edu")
            .professorEmail("pconrad@ucsb.edu")
            .explanation("recommendation")
            .dateRequested(ldt1)
            .dateNeeded(ldt2)
            .done(false)
            .build();

    String expectedJson = mapper.writeValueAsString(expected);
    String responseString = response.getResponse().getContentAsString();
    assertEquals(expectedJson, responseString);
  }

  @WithMockUser(roles = {"ADMIN", "USER"})
  @Test
  public void an_admin_user_can_post_a_recommendation_request_with_done_true() throws Exception {
    // arrange
    LocalDateTime ldt1 = LocalDateTime.parse("2023-03-10T00:00:00");
    LocalDateTime ldt2 = LocalDateTime.parse("2023-04-10T00:00:00");

    when(recommendationRequestRepository.save(any(RecommendationRequest.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    // act
    MvcResult response =
        mockMvc
            .perform(
                post("/api/recommendationrequests/post"
                        + "?requesterEmail=test@ucsb.edu"
                        + "&professorEmail=prof@ucsb.edu"
                        + "&explanation=testDoneTrue"
                        + "&dateRequested=2023-03-10T00:00:00"
                        + "&dateNeeded=2023-04-10T00:00:00"
                        + "&done=true")
                    .with(csrf()))
            .andExpect(status().isOk())
            .andReturn();

    // assert
    verify(recommendationRequestRepository, times(1))
        .save(
            argThat(
                (RecommendationRequest r) ->
                    r.getRequesterEmail().equals("test@ucsb.edu")
                        && r.getProfessorEmail().equals("prof@ucsb.edu")
                        && r.getExplanation().equals("testDoneTrue")
                        && r.getDateRequested().equals(ldt1)
                        && r.getDateNeeded().equals(ldt2)
                        && r.getDone() == true));

    RecommendationRequest expected =
        RecommendationRequest.builder()
            .requesterEmail("test@ucsb.edu")
            .professorEmail("prof@ucsb.edu")
            .explanation("testDoneTrue")
            .dateRequested(ldt1)
            .dateNeeded(ldt2)
            .done(true)
            .build();

    String expectedJson = mapper.writeValueAsString(expected);
    String responseString = response.getResponse().getContentAsString();
    assertEquals(expectedJson, responseString);
  }

  @Test
  public void logged_out_users_cannot_get_by_id() throws Exception {
    mockMvc
        .perform(get("/api/recommendationrequests?id=7"))
        .andExpect(status().is(403)); // logged out users can't get by id
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void test_that_logged_in_user_can_get_by_id_when_the_id_exists() throws Exception {

    // arrange
    LocalDateTime ldt = LocalDateTime.parse("2022-01-03T00:00:00");
    LocalDateTime ldt2 = LocalDateTime.parse("2022-02-03T00:00:00");

    RecommendationRequest recommendationRequest =
        RecommendationRequest.builder()
            .requesterEmail("john@ucsb.edu")
            .professorEmail("bob@ucsb.edu")
            .explanation("Help")
            .dateRequested(ldt)
            .dateNeeded(ldt2)
            .done(false)
            .build();

    when(recommendationRequestRepository.findById(eq(7L)))
        .thenReturn(Optional.of(recommendationRequest));

    // act
    MvcResult response =
        mockMvc
            .perform(get("/api/recommendationrequests?id=7"))
            .andExpect(status().isOk())
            .andReturn();

    // assert

    verify(recommendationRequestRepository, times(1)).findById(eq(7L));
    String expectedJson = mapper.writeValueAsString(recommendationRequest);
    String responseString = response.getResponse().getContentAsString();
    assertEquals(expectedJson, responseString);
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void test_that_logged_in_user_can_get_by_id_when_the_id_does_not_exist() throws Exception {

    // arrange

    when(recommendationRequestRepository.findById(eq(7L))).thenReturn(Optional.empty());

    // act
    MvcResult response =
        mockMvc
            .perform(get("/api/recommendationrequests?id=7"))
            .andExpect(status().isNotFound())
            .andReturn();

    // assert

    verify(recommendationRequestRepository, times(1)).findById(eq(7L));
    Map<String, Object> json = responseToJson(response);
    assertEquals("EntityNotFoundException", json.get("type"));
    assertEquals("RecommendationRequest with id 7 not found", json.get("message"));
  }

  @WithMockUser(roles = {"ADMIN", "USER"})
  @Test
  public void admin_can_edit_an_existing_recommendation_request() throws Exception {
    // arrange

    LocalDateTime ldt1 = LocalDateTime.parse("2022-01-03T00:00:00");
    LocalDateTime ldt2 = LocalDateTime.parse("2023-01-06T00:00:00");
    LocalDateTime ldt3 = LocalDateTime.parse("2025-01-03T00:00:00");
    LocalDateTime ldt4 = LocalDateTime.parse("2025-01-06T00:00:00");

    RecommendationRequest recommendationRequest =
        RecommendationRequest.builder()
            .requesterEmail("john@ucsb.edu")
            .professorEmail("bob@ucsb.edu")
            .explanation("Help")
            .dateRequested(ldt1)
            .dateNeeded(ldt2)
            .done(false)
            .build();

    RecommendationRequest editedRecommendationRequest =
        RecommendationRequest.builder()
            .requesterEmail("alex@ucsb.edu")
            .professorEmail("clara@ucsb.edu")
            .explanation("NOHELP")
            .dateRequested(ldt3)
            .dateNeeded(ldt4)
            .done(true)
            .build();

    String requestBody = mapper.writeValueAsString(editedRecommendationRequest);

    // when(recommendationRequestRepository.findById(eq(67L))).thenReturn(Optional.of(recommendationRequest));
    when(recommendationRequestRepository.findById(eq(67L)))
        .thenReturn(Optional.of(recommendationRequest));
    when(recommendationRequestRepository.save(any(RecommendationRequest.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    // act
    MvcResult response =
        mockMvc
            .perform(
                put("/api/recommendationrequests?id=67")
                    .contentType(MediaType.APPLICATION_JSON)
                    .characterEncoding("utf-8")
                    .content(requestBody)
                    .with(csrf()))
            .andExpect(status().isOk())
            .andReturn();

    // assert
    // verify(recommendationRequestRepository, times(1)).findById(67L);
    // verify(recommendationRequestRepository, times(1)).save(editedRecommendationRequest);
    verify(recommendationRequestRepository, times(1))
        .save(
            argThat(
                (RecommendationRequest r) ->
                    r.getRequesterEmail().equals("alex@ucsb.edu")
                        && r.getProfessorEmail().equals("clara@ucsb.edu")
                        && r.getExplanation().equals("NOHELP")
                        && r.getDateRequested().equals(ldt3)
                        && r.getDateNeeded().equals(ldt4)
                        && r.getDone() == true));

    String responseString = response.getResponse().getContentAsString();
    assertEquals(requestBody, responseString);
  }

  @WithMockUser(roles = {"ADMIN", "USER"})
  @Test
  public void admin_cannot_edit_recommendation_request_that_does_not_exist() throws Exception {
    // arrange

    LocalDateTime ldt1 = LocalDateTime.parse("2022-01-03T00:00:00");
    LocalDateTime ldt2 = LocalDateTime.parse("2022-01-06T00:00:00");

    RecommendationRequest recommendationRequest =
        RecommendationRequest.builder()
            .requesterEmail("john@ucsb.edu")
            .professorEmail("bob@ucsb.edu")
            .explanation("Help")
            .dateRequested(ldt1)
            .dateNeeded(ldt2)
            .done(true)
            .build();

    String requestBody = mapper.writeValueAsString(recommendationRequest);

    when(recommendationRequestRepository.findById(eq(67L))).thenReturn(Optional.empty());

    // act
    MvcResult response =
        mockMvc
            .perform(
                put("/api/recommendationrequests?id=67")
                    .contentType(MediaType.APPLICATION_JSON)
                    .characterEncoding("utf-8")
                    .content(requestBody)
                    .with(csrf()))
            .andExpect(status().isNotFound())
            .andReturn();

    // assert
    verify(recommendationRequestRepository, times(1)).findById(67L);
    Map<String, Object> json = responseToJson(response);
    assertEquals("RecommendationRequest with id 67 not found", json.get("message"));
  }
}
