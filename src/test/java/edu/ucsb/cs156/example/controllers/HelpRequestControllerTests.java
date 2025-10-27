package edu.ucsb.cs156.example.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.ArgumentMatchers.argThat;


import edu.ucsb.cs156.example.ControllerTestCase;
import edu.ucsb.cs156.example.entities.HelpRequest;
import edu.ucsb.cs156.example.repositories.HelpRequestRepository;
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
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import java.util.List;


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

    @WithMockUser(roles = {"USER"})
    @Test
    public void user_get_all_returns_data() throws Exception {
        var h = HelpRequest.builder()
            .requesterEmail("a@b.com")
            .teamId("T1")
            .tableOrBreakoutRoom("Table1")
            .explanation("x")
            .solved(false)
            .requestTime(LocalDateTime.parse("2025-12-23T10:00:00"))
            .build();

        when(helpRequestRepository.findAll()).thenReturn(List.of(h));

        mockMvc.perform(get("/api/helprequests/all"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].requesterEmail", is("a@b.com")))
            .andExpect(jsonPath("$[0].teamId", is("T1")));

        // Kills VoidMethodCall mutants
        verify(helpRequestRepository).findAll();
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

    @WithMockUser(roles = {"ADMIN"})
    @Test
    public void admin_can_post_and_entity_is_saved() throws Exception {
        when(helpRequestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(post("/api/helprequests/post")
                .param("requesterEmail", "a@b.com")
                .param("teamId", "T1")
                .param("tableOrBreakoutRoom", "Table1")
                .param("explanation", "x")
                .param("solved", "false")
                .param("requestTime", "2025-12-23T10:00:00")
                .with(csrf()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.requesterEmail", is("a@b.com")))
            .andExpect(jsonPath("$.teamId", is("T1")));

        // Verify fields to kill more mutants
        verify(helpRequestRepository).save(argThat(h ->
            "a@b.com".equals(h.getRequesterEmail()) &&
            "T1".equals(h.getTeamId()) &&
            "Table1".equals(h.getTableOrBreakoutRoom()) &&
            "x".equals(h.getExplanation()) &&
            !h.isSolved() &&
            LocalDateTime.parse("2025-12-23T10:00:00").equals(h.getRequestTime())
        ));
    }            
}
