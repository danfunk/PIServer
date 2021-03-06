package org.mindtrails.controller;

import org.mindtrails.domain.CoachPrompt;
import org.mindtrails.domain.Participant;
import org.mindtrails.domain.forms.ParticipantUpdateAdmin;
import org.mindtrails.domain.tracking.CoachLog;
import org.mindtrails.persistence.CoachLogRepository;
import org.mindtrails.persistence.CoachPromptRepository;
import org.mindtrails.persistence.ParticipantRepository;
import org.mindtrails.service.ParticipantService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping("/coach")
public class CoachController extends BaseController {

    private static final Logger LOG = LoggerFactory.getLogger(AdminController.class);

    @Autowired
    private ParticipantRepository participantRepository;

    @Autowired
    private ParticipantService participantService;

    @Autowired
    private CoachPromptRepository coachPromptRepository;

    @Autowired
    private CoachLogRepository coachLogRepository;

    private static final int PER_PAGE=20; // Number of users to display per page.

    @RequestMapping(method = RequestMethod.GET)
    public String listCoaches(ModelMap model, Principal principal,
        final @RequestParam(value = "search", required = false, defaultValue = "") String search,
        final @RequestParam(value = "page", required = false, defaultValue = "0") String pageParam) {

        Page<Participant> daoList;
        PageRequest pageRequest;

        List<Participant> coaches = participantRepository.findCoaches();
        int page = Integer.parseInt(pageParam);
        pageRequest = new PageRequest(page, PER_PAGE);
        if(search.isEmpty()) {
            daoList = participantService.findEligibleForCoaching(pageRequest);
        } else {
            daoList = participantService.searchEligibleForCoaching(pageRequest, search);
        }

        model.addAttribute("coaches", coaches);
        model.addAttribute("search", search);
        model.addAttribute("paging", daoList);
        model.addAttribute("participants", daoList);
        return "coach/coaches";
    }

    @RequestMapping(value="/{id}", method = RequestMethod.GET)
    public String listCoachees(ModelMap model, Principal principal, @PathVariable("id") long id) {
        Participant coach  = participantRepository.findOne(id);
        model.addAttribute("coach", coach);
        return "coach/coachees";
    }

    @RequestMapping(value="/me", method = RequestMethod.GET)
    public String listCoachees(ModelMap model, Principal principal) {

        Participant coach = getParticipant(principal);
        model.addAttribute("coach", coach);
        return "coach/coachees";
    }

    @RequestMapping(value="/participant/{id}", method=RequestMethod.GET)
    public String viewParticipant(ModelMap model, Principal principal,
                                        @PathVariable("id") long id) {
        Participant coachee;
        ParticipantUpdateAdmin form;
        coachee    = participantRepository.findOne(id);
        List<CoachPrompt> cp = coachPromptRepository.findAllByParticipant(coachee);
        model.addAttribute("coachee", coachee);
        model.addAttribute("coaches", participantRepository.findCoaches());
        model.addAttribute("feedback", cp);
        return "coach/participant";
    }

    @RequestMapping(value="/log/{id}", method = RequestMethod.POST)
    public String createLog(ModelMap model, Principal principal,
                            @PathVariable("id") long coacheeId,
                            @ModelAttribute CoachLog coachLog) {
        Participant coach = getParticipant(principal);
        Participant coachee = participantRepository.findOne(coacheeId);
        coachLog.setId(0);
        coachLog.setCoach(coach);
        coachLog.setParticipant(coachee);
        this.coachLogRepository.saveAndFlush(coachLog);
        return "redirect:/coach/participant/" + coacheeId;
    }

}
