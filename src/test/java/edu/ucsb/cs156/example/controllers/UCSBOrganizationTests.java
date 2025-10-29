package edu.ucsb.cs156.example.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
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

import edu.ucsb.cs156.example.ControllerTestCase;
import edu.ucsb.cs156.example.entities.UCSBOrganization;
import edu.ucsb.cs156.example.repositories.UCSBOrganizationRepository;
import edu.ucsb.cs156.example.repositories.UserRepository;
import edu.ucsb.cs156.example.testconfig.TestConfig;
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

  @WithMockUser(roles = {"USER"})
  @Test
  public void test_that_logged_in_user_can_get_by_id_when_the_id_exists() throws Exception {

    // arrange

    UCSBOrganization org =
        UCSBOrganization.builder()
            .orgCode("TT")
            .orgTranslationShort("Theta Tau")
            .orgTranslation("Engineering Frat")
            .inactive(false)
            .build();

    when(ucsbOrganizationRepository.findById(eq("TT"))).thenReturn(Optional.of(org));

    // act
    MvcResult response =
        mockMvc
            .perform(get("/api/ucsborganization?orgCode=TT"))
            .andExpect(status().isOk())
            .andReturn();

    // assert

    verify(ucsbOrganizationRepository, times(1)).findById(eq("TT"));
    String expectedJson = mapper.writeValueAsString(org);
    String responseString = response.getResponse().getContentAsString();
    assertEquals(expectedJson, responseString);
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void test_that_logged_in_user_can_get_by_id_when_the_id_does_not_exist() throws Exception {

    // arrange

    when(ucsbOrganizationRepository.findById(eq("akpsi"))).thenReturn(Optional.empty());

    // act
    MvcResult response =
        mockMvc
            .perform(get("/api/ucsborganization?orgCode=akpsi"))
            .andExpect(status().isNotFound())
            .andReturn();

    // assert

    verify(ucsbOrganizationRepository, times(1)).findById(eq("akpsi"));
    Map<String, Object> json = responseToJson(response);
    assertEquals("EntityNotFoundException", json.get("type"));
    assertEquals("UCSBOrganization with id akpsi not found", json.get("message"));
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

  @WithMockUser(roles = {"ADMIN", "USER"})
  @Test
  public void admin_can_delete_a_org() throws Exception {
    // arrange

    UCSBOrganization organization1 =
        UCSBOrganization.builder()
            .orgCode("TT")
            .orgTranslationShort("Theta Tau")
            .orgTranslation("Engineering Frat")
            .inactive(false)
            .build();

    when(ucsbOrganizationRepository.findById(eq("TT"))).thenReturn(Optional.of(organization1));

    // act
    MvcResult response =
        mockMvc
            .perform(delete("/api/ucsborganization?orgCode=TT").with(csrf()))
            .andExpect(status().isOk())
            .andReturn();

    // assert
    verify(ucsbOrganizationRepository, times(1)).findById("TT");
    verify(ucsbOrganizationRepository, times(1)).delete(any());

    Map<String, Object> json = responseToJson(response);
    assertEquals("UCSBOrganization with id TT deleted", json.get("message"));
  }

  @WithMockUser(roles = {"ADMIN", "USER"})
  @Test
  public void admin_tries_to_delete_non_existant_organization_and_gets_right_error_message()
      throws Exception {
    // arrange

    when(ucsbOrganizationRepository.findById(eq("akpsi"))).thenReturn(Optional.empty());

    // act
    MvcResult response =
        mockMvc
            .perform(delete("/api/ucsborganization?orgCode=akpsi").with(csrf()))
            .andExpect(status().isNotFound())
            .andReturn();

    // assert
    verify(ucsbOrganizationRepository, times(1)).findById("akpsi");
    Map<String, Object> json = responseToJson(response);
    assertEquals("UCSBOrganization with id akpsi not found", json.get("message"));
  }

  @WithMockUser(roles = {"ADMIN", "USER"})
  @Test
  public void admin_can_edit_an_existing_organization() throws Exception {
    // arrange

    UCSBOrganization organizationOg =
        UCSBOrganization.builder()
            .orgCode("TT")
            .orgTranslationShort("Theta Tau")
            .orgTranslation("Engineering Frat")
            .inactive(false)
            .build();

    UCSBOrganization organizationEdited =
        UCSBOrganization.builder()
            .orgCode("T_T")
            .orgTranslationShort("Theta_Tau")
            .orgTranslation("Eng Frat")
            .inactive(true)
            .build();

    String requestBody = mapper.writeValueAsString(organizationEdited);

    when(ucsbOrganizationRepository.findById(eq("TT"))).thenReturn(Optional.of(organizationOg));

    // act
    MvcResult response =
        mockMvc
            .perform(
                put("/api/ucsborganization?orgCode=TT")
                    .contentType(MediaType.APPLICATION_JSON)
                    .characterEncoding("utf-8")
                    .content(requestBody)
                    .with(csrf()))
            .andExpect(status().isOk())
            .andReturn();

    // assert
    verify(ucsbOrganizationRepository, times(1)).findById("TT");
    verify(ucsbOrganizationRepository, times(1))
        .save(organizationEdited); // should be saved with updated info
    String responseString = response.getResponse().getContentAsString();
    assertEquals(requestBody, responseString);
  }

  @WithMockUser(roles = {"ADMIN", "USER"})
  @Test
  public void admin_cannot_edit_commons_that_does_not_exist() throws Exception {
    // arrange

    UCSBOrganization organizationEdited =
        UCSBOrganization.builder()
            .orgCode("akpsi")
            .orgTranslationShort("AK Psi")
            .orgTranslation("Business Frat")
            .inactive(false)
            .build();

    String requestBody = mapper.writeValueAsString(organizationEdited);

    when(ucsbOrganizationRepository.findById(eq("akpsi"))).thenReturn(Optional.empty());

    // act
    MvcResult response =
        mockMvc
            .perform(
                put("/api/ucsborganization?orgCode=akpsi")
                    .contentType(MediaType.APPLICATION_JSON)
                    .characterEncoding("utf-8")
                    .content(requestBody)
                    .with(csrf()))
            .andExpect(status().isNotFound())
            .andReturn();

    // assert
    verify(ucsbOrganizationRepository, times(1)).findById("akpsi");
    Map<String, Object> json = responseToJson(response);
    assertEquals("UCSBOrganization with id akpsi not found", json.get("message"));
  }
}
