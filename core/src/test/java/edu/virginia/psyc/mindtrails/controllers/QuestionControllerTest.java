package edu.virginia.psyc.mindtrails.controllers;

import edu.virginia.psyc.mindtrails.Application;
import edu.virginia.psyc.mindtrails.MockClasses.*;
import edu.virginia.psyc.mindtrails.controller.QuestionController;
import edu.virginia.psyc.mindtrails.domain.Participant;
import edu.virginia.psyc.mindtrails.persistence.ParticipantRepository;
import edu.virginia.psyc.mindtrails.service.ParticipantService;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;

import static org.junit.Assert.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Created by dan on 10/23/15.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = Application.class)
@WebAppConfiguration
@ActiveProfiles("test")
public class QuestionControllerTest {

    @Autowired
    private FilterChainProxy springSecurityFilterChain;

    @Autowired
    private TestQuestionnaireRepository repository;

    @Autowired
    private TestUndeleteableRepository undeleteableRepository;

    @Autowired
    private QuestionController questionController;

    @Autowired
    private ParticipantService participantService;


    private MockMvc mockMvc;

    @Autowired
    WebApplicationContext wac;

    private Participant participant;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        this.mockMvc = MockMvcBuilders.standaloneSetup(questionController)
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .addFilters(this.springSecurityFilterChain)
                .build();
        repository.deleteAll();
    }

    @Before
    public void veryifyParticipant() {
        participant = participantService.findByEmail("test@test.com");
        if(participant == null) participant = new Participant("John", "test@test.com", false);
        participant.setStudy(new TestStudy());
        participantService.save(participant);
    }

    @After
    public void cleanup() {
    //formRepository.deleteAll();
    }

    private TestQuestionnaire getLastQuestionnaire() {
        repository.flush();
        List<TestQuestionnaire> dataList = repository.findAll();
        return dataList.get(dataList.size() - 1);
    }

    @Test
    public void testGetForm() throws Exception {
        MvcResult result = mockMvc.perform(get("/questions/TestQuestionnaire")
                .with(SecurityMockMvcRequestPostProcessors.user(participant)))
                .andExpect((status().is2xxSuccessful()))
                .andReturn();
    }

    @Test
    public void testGetFormContainsCustomParameters() throws Exception {
        MvcResult result = mockMvc.perform(get("/questions/TestQuestionnaire")
                .with(SecurityMockMvcRequestPostProcessors.user(participant)))
                .andExpect((status().is2xxSuccessful()))
                .andExpect(model().attribute("test", "pickles"))
                .andReturn();
    }


    @Test
    public void testPostDataFormFailsIfWrongSession() throws Exception {
        // TestUndeleteable is at task index 1, not task index 0.
        participant.getStudy().forceToSession("SessionOne");
        ResultActions result = mockMvc.perform(post("/questions/TestUndeleteable")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .with(SecurityMockMvcRequestPostProcessors.user(participant))
                .param("value", "cheese")
                .param("multiValue", "cheddar")
                .param("multiValue", "havarti"))
                .andExpect((status().is4xxClientError()));
    }

    @Test
    public void testPostDataForm() throws Exception {
        ResultActions result = mockMvc.perform(post("/questions/TestQuestionnaire")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .with(SecurityMockMvcRequestPostProcessors.user(participant))
                .param("value", "cheese")
                .param("multiValue", "cheddar")
                .param("multiValue", "havarti"))
                .andExpect((status().is3xxRedirection()));
    }

    @Test
    public void testPostBadData() throws Exception {
        ResultActions result = mockMvc.perform(post("/questions/NoSuchForm")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .with(SecurityMockMvcRequestPostProcessors.user(participant))
                .param("value", "cheese")
                .param("multiValue", "cheddar")
                .param("multiValue", "havarti"))
                .andExpect((status().is4xxClientError()));
    }

    @Test
    public void testThatPostedDataIsStored() throws Exception {
        testPostDataForm();
        Assert.assertEquals("cheese", getLastQuestionnaire().getValue());
    }

    @Test
    public void testParticipantDateAndSessionArePopulated() throws Exception {
        testPostDataForm();
        assertNotNull(getLastQuestionnaire().getDate());
        assertNotNull(getLastQuestionnaire().getSession());
        assertNotNull(getLastQuestionnaire().getParticipantRSA());
    }

    @Test
    public void testParticipantIdIsPopulatedIfItExists() throws Exception {
        // Force Participant to task 1 (which is TestUndeleteable)
//        participant.getStudy().getCurrentSession().setIndex(1);
        ((TestStudy) participant.getStudy()).setCurrentTaskIndex(1);
        participantService.save(participant);
        ResultActions result = mockMvc.perform(post("/questions/TestUndeleteable")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .with(SecurityMockMvcRequestPostProcessors.user(participant))
                .param("value", "cheese"))
                .andExpect((status().is3xxRedirection()));

        undeleteableRepository.flush();
        List<TestUndeleteable> dataList = undeleteableRepository.findAll();
        TestUndeleteable last = dataList.get(dataList.size() - 1);
        assertNotNull(last.getParticipant());

    }
}