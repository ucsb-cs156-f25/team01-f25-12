package edu.ucsb.cs156.example.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edu.ucsb.cs156.example.ControllerTestCase;
import edu.ucsb.cs156.example.entities.UCSBOrganization;
import edu.ucsb.cs156.example.repositories.UCSBOrganizationRepository;
import edu.ucsb.cs156.example.repositories.UserRepository;
import edu.ucsb.cs156.example.testconfig.TestConfig;
import java.util.ArrayList;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MvcResult;

@WebMvcTest(controllers = UCSBOrganizationController.class)
@Import(TestConfig.class)
public class UCSBOrganizationTests extends ControllerTestCase {

  @MockBean UCSBOrganizationRepository ucsbOrganizationRepository;

  @MockBean UserRepository userRepository;

  @Test
  public void logged_out_users_cannot_get_all() throws Exception {
    mockMvc
        .perform(get("/api/ucsborganization/all"))
        .andExpect(status().is(403)); // logged out users can't get all
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void logged_in_users_can_get_all() throws Exception {
    UCSBOrganization org1 =
        UCSBOrganization.builder()
            .orgCode("ORG1")
            .orgTranslationShort("Organization One Short")
            .orgTranslation("Organization One Full Name")
            .inactive(false)
            .build();
    UCSBOrganization org2 =
        UCSBOrganization.builder()
            .orgCode("ORG2")
            .orgTranslationShort("Organization Two Short")
            .orgTranslation("Organization Two Full Name")
            .inactive(true)
            .build();
    ArrayList<UCSBOrganization> expectedOrgs = new ArrayList<>(Arrays.asList(org1, org2));
    when(ucsbOrganizationRepository.findAll()).thenReturn(expectedOrgs);

    MvcResult mvcResult =
        mockMvc.perform(get("/api/ucsborganization/all")).andExpect(status().isOk()).andReturn();
    String responseString = mvcResult.getResponse().getContentAsString();
    UCSBOrganization[] actualOrgs = mapper.readValue(responseString, UCSBOrganization[].class);
    assertEquals(2, actualOrgs.length);
    assertEquals("ORG1", actualOrgs[0].getOrgCode());
    assertEquals("Organization One Short", actualOrgs[0].getOrgTranslationShort());
    assertEquals("Organization One Full Name", actualOrgs[0].getOrgTranslation());
    assertEquals(false, actualOrgs[0].getInactive());
    assertEquals("ORG2", actualOrgs[1].getOrgCode());
    assertEquals("Organization Two Short", actualOrgs[1].getOrgTranslationShort());
    assertEquals("Organization Two Full Name", actualOrgs[1].getOrgTranslation());
    assertEquals(true, actualOrgs[1].getInactive());
  }

  @Test
  public void logged_out_users_cannot_post() throws Exception {
    mockMvc.perform(post("/api/ucsborganization/post")).andExpect(status().is(403));
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void logged_in_regular_users_cannot_post() throws Exception {
    mockMvc
        .perform(post("/api/ucsborganization/post"))
        .andExpect(status().is(403)); // only admins can post
  }

  @WithMockUser(roles = {"ADMIN"})
  @Test
  public void admin_can_post_new_organization() throws Exception {
    UCSBOrganization og =
        UCSBOrganization.builder()
            .orgCode("INACTIVE")
            .orgTranslationShort("Inactive Org Short")
            .orgTranslation("Inactive Org Full Name")
            .inactive(true)
            .build();

    when(ucsbOrganizationRepository.save(any(UCSBOrganization.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    MvcResult mvcResult =
        mockMvc
            .perform(
                post("/api/ucsborganization/post")
                    .with(csrf())
                    .param("orgCode", "INACTIVE")
                    .param("orgTranslationShort", "Inactive Org Short")
                    .param("orgTranslation", "Inactive Org Full Name")
                    .param("inactive", "true"))
            .andExpect(status().isOk())
            .andReturn();

    verify(ucsbOrganizationRepository, times(1))
        .save(
            argThat(
                org ->
                    org.getOrgCode().equals("INACTIVE")
                        && org.getOrgTranslationShort().equals("Inactive Org Short")
                        && org.getOrgTranslation().equals("Inactive Org Full Name")
                        && org.getInactive()));

    String responseString = mvcResult.getResponse().getContentAsString();
    UCSBOrganization returned = mapper.readValue(responseString, UCSBOrganization.class);

    assertEquals("INACTIVE", returned.getOrgCode());
    assertEquals("Inactive Org Short", returned.getOrgTranslationShort());
    assertEquals("Inactive Org Full Name", returned.getOrgTranslation());
    assertEquals(true, returned.getInactive());
  }
}
