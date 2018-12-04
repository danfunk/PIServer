package edu.virginia.psyc.r01.service;

import edu.virginia.psyc.r01.domain.R01Study;
import edu.virginia.psyc.r01.persistence.*;
import org.mindtrails.domain.Conditions.NoNewConditionException;
import org.mindtrails.domain.Participant;
import org.mindtrails.domain.Conditions.RandomCondition;
import org.mindtrails.domain.RestExceptions.MissingEligibilityException;
import org.mindtrails.domain.Study;
import org.mindtrails.persistence.ParticipantRepository;
import org.mindtrails.domain.Conditions.RandomConditionRepository;
import org.mindtrails.service.ParticipantService;
import org.mindtrails.service.ParticipantServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;
import java.util.*;

/**
 * Largely a wrapper around the Participant Repository.  Allows us to
 * save, create, and find customized participant objects.
 */
@Service
public class R01ParticipantService extends ParticipantServiceImpl implements ParticipantService {

    protected static final String ELIGIBLE_SESSION = "ELIGIBLE";

    String UNASSIGNED_SEGMENT = "unassigned";

    @Autowired
    ParticipantRepository participantRepository;

    @Autowired
    DASS21_ASRepository dass21RRepository;

    @Autowired
    DemographicsRepository demographicsRepository;

    @Autowired
    RandomConditionRepository randomBlockRepository;

    @Autowired
    AttritionPredictionRepository attritionPredictionRepository;

    @Override
    public Participant create() {
        Participant p = new Participant();
        R01Study study = new R01Study();
        p.setStudy(study);
        p.setReceiveGiftCards(true);
        study.setConditioning(R01Study.CONDITION.NONE.name());
        return p;
    }

    @Override
    public RandomCondition getCondition(Participant p) throws NoNewConditionException {
        String segmentation = getSegmentation(p);

        // We have to know their segmentation to set a new condition.
        if (segmentation.equals(UNASSIGNED_SEGMENT)) throw new NoNewConditionException();

        // For NONE, and TRAINING, it may be possible to update the condition.
        RandomCondition assignment;
        R01Study.CONDITION condition = R01Study.CONDITION.valueOf(p.getStudy().getConditioning());
        switch (condition) {
            case NONE:
                assignment = nextAssignmentForSegment(segmentation);
                break;
            case TRAINING:
                AttritionPrediction pred = attritionPredictionRepository.findFirstByParticipant(p);
                if (pred != null && pred.isAtRisk()) {
                    assignment = nextAssignmentForCoaching(segmentation);
                } else {
                    // We don't have a prediction for this participant yet.
                    throw new NoNewConditionException();
                }
                break;
            default:  // once assigned to control, coach or no_coach, we no longer assign new conditions.
                throw new NoNewConditionException();
        }
        return assignment;
    }

    @Override
    public void markConditionAsUsed(RandomCondition rc) {
        this.randomBlockRepository.delete(rc);
    }

    protected String getSegmentation(Participant p) {
        Demographics demographics = demographicsRepository.findByParticipantId(p.getId());
        DASS21_AS dass = dass21RRepository.findFirstByParticipantAndSession(p, ELIGIBLE_SESSION);
        if(demographics != null && dass != null) {
            return getSegmentFromDassAndDemographics(dass, demographics);
        } else {
            return UNASSIGNED_SEGMENT;
        }
    }

    protected String getSegmentFromDassAndDemographics(DASS21_AS dass, Demographics demographics) {
        String gender_seg = demographics.calculateSegmentation();
        String dass_seg = dass.calculateSegmentation();
        return gender_seg + "_" + dass_seg;
    }


    private RandomCondition nextAssignmentForSegment(String segment) {
        assureRandomAssignmentsAvailableForSegment(segment);
        RandomCondition assignment = randomBlockRepository.findFirstBySegmentNameOrderById(segment);
        return assignment;
    }

    private RandomCondition nextAssignmentForCoaching(String segment) {
        String coachSegment = "coach_" + segment;
        assureRandomAssignmentsAvailableForCoaching(coachSegment);
        RandomCondition assignment = randomBlockRepository.findFirstBySegmentNameOrderById(coachSegment);
        return assignment;
    }

    /**
     * Random blocks for intial assignment are split 75/25 for training and control respectively.
     * @param segment
     */
    private void assureRandomAssignmentsAvailableForSegment(String segment) {
        if(randomBlockRepository.countAllBySegmentName(segment) < 1) {
            Map<String, Float> valuePercentages = new HashMap<>();
            valuePercentages.put(R01Study.CONDITION.TRAINING.name(), 75.0f);
            valuePercentages.put(R01Study.CONDITION.CONTROL.name(), 25.0f);
            List<RandomCondition> blocks = RandomCondition.createBlocks(valuePercentages, 50, segment);
            this.randomBlockRepository.save(blocks);
            this.randomBlockRepository.flush();
        }
    }

    /**
     * Random blocks for coaching assignment are split 50/50 for coaching and no coaching
     * @param segment
     */
    private void assureRandomAssignmentsAvailableForCoaching(String segment) {
        if(randomBlockRepository.countAllBySegmentName(segment) < 1) {
            Map<String, Float> valuePercentages = new HashMap<>();
            valuePercentages.put(R01Study.CONDITION.HR_COACH.name(), 50.0f);
            valuePercentages.put(R01Study.CONDITION.HR_NOCOACH.name(), 50.0f);
            List<RandomCondition> blocks = RandomCondition.createBlocks(valuePercentages, 50, segment);
            this.randomBlockRepository.save(blocks);
            this.randomBlockRepository.flush();
        }
    }

    @Override
    public List<Study> getStudies() {
        List<Study> studies = new ArrayList<>();
        studies.add(new R01Study());
        return studies;
    }

    @Override
    public boolean isEligible(HttpSession session) {
        List<DASS21_AS> forms = dass21RRepository.findBySessionId(session.getId());
        for (DASS21_AS e : forms) {
            if (e.eligible()) return true;
        }
        return false;
    }

    @Override
    public void saveNew(Participant p, HttpSession session) throws MissingEligibilityException {

        List<DASS21_AS> forms = dass21RRepository.findBySessionId(session.getId());
        if(forms.size() < 1) {
            throw new MissingEligibilityException();
        }

        save(p);
        R01Study study = (R01Study)p.getStudy();

        // Now that p is saved, connect any Expectancy Bias eligibility data back to the
        // session, and log it in the TaskLog
        for (DASS21_AS e : forms) {
            e.setParticipant(p);
            e.setSession(ELIGIBLE_SESSION);
            dass21RRepository.save(e);
            study.completeEligibility(e);
        }
        save(p); // Re-save participant to record study.
    }
}