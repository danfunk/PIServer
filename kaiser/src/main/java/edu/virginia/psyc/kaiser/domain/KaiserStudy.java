package edu.virginia.psyc.kaiser.domain;

import lombok.Data;
import org.mindtrails.domain.BaseStudy;
import org.mindtrails.domain.Session;
import org.mindtrails.domain.Task;
import org.mindtrails.domain.tracking.TaskLog;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This is where you define the sessions and tasks that make up the study.
 */
@Entity

@Data
@DiscriminatorValue("KAISER")
public class KaiserStudy extends BaseStudy {

    // TODO: Write test to ensure we never get a "NONE" as a condition
    public enum CONDITION {NO_INCENTIVE, NO_INCENTIVE_SHORTM, CAN_COACH, CAN_COACH_SHORTM, TESTIMONIALS, TESTIMONIALS_SHORTM, BONUS_5, BONUS_5_SHORTM, BONUS_10, BONUS_10_SHORTM, BONUS_20, BONUS_20_SHORTM}
    public static Map<String, Enum<?>> conditionMappings = new HashMap<String, Enum<?>>();

    public enum SESSION {preTest, firstSession, secondSession, thirdSession, fourthSession, fifthSession, PostFollowUp }

    public enum STUDY_EXTENSIONS {KAISER}

    public static final String PRE_TEST = "preTest";
    public static final String FIRST_SESSION = "firstSession";
    public static final String SECOND_SESSION = "secondSession";
    public static final String THIRD_SESSION = "thirdSession";
    public static final String FOURTH_SESSION = "fourthSession";
    public static final String FIFTH_SESSION = "fifthSession";
    public static final String POST_FOLLOWUP = "PostFollowUp";
    public static final String POST_FOLLOWUP2 = "PostFollowUp2";

    @Override
    public String getName() {return "KAISER";}

    public KaiserStudy() {
        this.currentSession = PRE_TEST;

        conditionMappings.put("2AOM8dDBOJ", CONDITION.NO_INCENTIVE);
        conditionMappings.put("q3R5OsNiAW", CONDITION.NO_INCENTIVE_SHORTM);
        conditionMappings.put("DSDvBmGcYE", CONDITION.CAN_COACH);
        conditionMappings.put("PJy60pTXaM", CONDITION.CAN_COACH_SHORTM);
        conditionMappings.put("WtQgTY7a1w", CONDITION.TESTIMONIALS);
        conditionMappings.put("O4mi6hD0Wd", CONDITION.TESTIMONIALS_SHORTM);
        conditionMappings.put("oLewA3TOBh", CONDITION.BONUS_5);
        conditionMappings.put("8sXj54aeOq", CONDITION.BONUS_5_SHORTM);
        conditionMappings.put("TtBdqsBNiZ", CONDITION.BONUS_10);
        conditionMappings.put("N70rEL8MwX", CONDITION.BONUS_10_SHORTM);
        conditionMappings.put("X1dwxumXD4", CONDITION.BONUS_20);
        conditionMappings.put("w72gBJwnpu", CONDITION.BONUS_20_SHORTM);
    }

    public KaiserStudy(String currentSession, int taskIndex, Date lastSessionDate, List<TaskLog> taskLogs, boolean receiveGiftCards) {
        super(currentSession, taskIndex, lastSessionDate, taskLogs, receiveGiftCards);
    }


    @Override
    /** Check out the Templeton Study for building a more complex setup. */
    public Map<String,Object> getPiPlayerParameters() {
        // No longer used, just return an empty map.
        Map<String,Object> map = super.getPiPlayerParameters();
        return map;
    }

    public List<String>getConditions(){
        return Stream.of(CONDITION.values()) .map(Enum::name) .collect(Collectors.toList());
    }

    public boolean inSession() {
        return (!getCurrentSession().getName().equals(KaiserStudy.SESSION.preTest.toString()) &&
                !getCurrentSession().getName().equals(KaiserStudy.SESSION.firstSession.toString()) &&
                !getCurrentSession().getName().equals(KaiserStudy.SESSION.secondSession.toString()) &&
                !getCurrentSession().getName().equals(KaiserStudy.SESSION.thirdSession.toString())&&
                !getCurrentSession().getName().equals(KaiserStudy.SESSION.fourthSession.toString())&&
                !getCurrentSession().getName().equals(KaiserStudy.SESSION.fifthSession.toString())&&
                !getCurrentSession().getName().equals(KaiserStudy.SESSION.PostFollowUp.toString()));
    }

    /**
     * Returns the list of sessions and tasks that define the study.
     * @return
     */
    @Override
    public List<Session> getStatelessSessions() {

        List<Session> sessions = new ArrayList<>();
        Session pretest, session1, session2, session3, session4, session5, post, post2;

        //Changes from RO1: In Kaiser, baseline OASIS and DASS moved to Pre-Test from Eligibility. BBSIQ is not administered.

        int giftAmount = 0;
        if (this.conditioning.equals(CONDITION.BONUS_5.name())) {
            giftAmount = 5;
        } else if (this.conditioning.equals(CONDITION.BONUS_10.name())) {
            giftAmount = 10;
        } else if (this.conditioning.equals(CONDITION.BONUS_20.name())) {
            giftAmount = 20;
        }


        pretest = new Session (PRE_TEST, "Initial Assessment",0, 0);
        pretest.setIndex(0);
        pretest.addTask(new Task("Identity","Personal Information", Task.TYPE.questions, 0 ));
        pretest.addTask(new Task("Credibility","How Does MindTrails Work?", Task.TYPE.questions, 0 ));
        pretest.addTask(new Task("Demographics","Personal Background", Task.TYPE.questions, 2 ));
        pretest.addTask(new Task("MentalHealthHistory","Mental Health and Treatment History", Task.TYPE.questions, 2 ));
        pretest.addTask(new Task("OA","Anxiety Review", Task.TYPE.questions, 1 ));
        pretest.addTask(new Task("DASS21_AS","Mood Assessment", Task.TYPE.questions, 0 ));
        pretest.addTask(new Task("AnxietyTriggers","Anxiety Triggers", Task.TYPE.questions, 0 ));
        pretest.addTask(new Task("recognitionRatings", "Completing Short Stories", Task.TYPE.angular, 5));
        pretest.addTask(new Task("RR","Completing Short Stories, Pt. 2", Task.TYPE.questions, 0 ));
        pretest.addTask(new Task("Comorbid","Mood, Drinking, and Marijuana Use", Task.TYPE.questions, 0 ));
        pretest.addTask(new Task("Wellness","What I Believe", Task.TYPE.questions, 0 ));
        pretest.addTask(new Task("Mechanisms","How I Respond", Task.TYPE.questions, 0 ));
        pretest.addTask(new Task("Covid19","COVID-19", Task.TYPE.questions, 0 ));
        pretest.addTask(new Task("TechnologyUse", "Technology Use", Task.TYPE.questions, 0));
        sessions.add(pretest);
        
        session1 = new Session(FIRST_SESSION, "Level 1: Beginner", giftAmount, 0);
        session1.setIndex(1);

        session1.addTask(new Task("Affect","Current Feelings, Pre", "pre", Task.TYPE.questions, 0));
        session1.addTask(new Task("1", "Training Session 1", Task.TYPE.angular, 20));
        session1.addTask(new Task("Affect","Current Feelings, Post", "post", Task.TYPE.questions, 0));
        session1.addTask(new Task("CC","Compare and Contrast", Task.TYPE.questions, 0 ));        
        session1.addTask(new Task("SessionReview", "Session Review", Task.TYPE.questions, 0));
        session1.addTask(new Task("OA","Anxiety Review", Task.TYPE.questions, 1 ));
        session1.addTask(new Task("CoachPrompt","Feedback", Task.TYPE.questions, 0 ));
        session1.addTask(new Task("ReturnIntention","Returning to the Program", Task.TYPE.questions, 0));
        sessions.add(session1);

        session2 = new Session(SECOND_SESSION, "Level 2: Intermediate", 0, 5);
        session2.setIndex(2);

        session2.addTask(new Task("2", "Training Session 2", Task.TYPE.angular, 20));
        session2.addTask(new Task("SessionReview", "Session Review", Task.TYPE.questions, 0));
        session2.addTask(new Task("OA","Anxiety Review", Task.TYPE.questions, 1 ));
        session2.addTask(new Task("ReturnIntention","Returning to the Program", Task.TYPE.questions, 0));
        sessions.add(session2);

        session3 = new Session(THIRD_SESSION, "Level 3: Advanced", 0, 5);
        session3.setIndex(3);

        session3.addTask(new Task("Affect","Current Feelings, Pre", "pre", Task.TYPE.questions, 0));
        session3.addTask(new Task("3", "Training Session 3", Task.TYPE.angular, 20));
        session3.addTask(new Task("Affect","Current Feelings, Post", "post", Task.TYPE.questions, 0));
        session3.addTask(new Task("CC","Compare and Contrast", Task.TYPE.questions, 0 ));        
        session3.addTask(new Task("SessionReview", "Session Review", Task.TYPE.questions, 0));
        session3.addTask(new Task("OA","Anxiety Review", Task.TYPE.questions, 1 ));
        session3.addTask(new Task("DASS21_AS","Mood Assessment", Task.TYPE.questions, 0 ));
        session3.addTask(new Task("recognitionRatings", "Completing Short Stories", Task.TYPE.angular, 5));
        session3.addTask(new Task("RR","Completing Short Stories, Pt. 2", Task.TYPE.questions, 0 ));
        session3.addTask(new Task("Comorbid","Mood, Drinking, and Marijuana Use", Task.TYPE.questions, 0 ));
        session3.addTask(new Task("Wellness","What I Believe", Task.TYPE.questions, 0 ));
        session3.addTask(new Task("Mechanisms","How I Respond", Task.TYPE.questions, 0 ));
        session3.addTask(new Task("Covid19","COVID-19", Task.TYPE.questions, 0 ));
        session3.addTask(new Task("ReturnIntention","Returning to the Program", Task.TYPE.questions, 0));
        sessions.add(session3);

        session4 = new Session(FOURTH_SESSION, "Level 4: Expert", 0, 5);
        session4.setIndex(4);

        session4.addTask(new Task("4", "Training Session 4", Task.TYPE.angular, 20));
        session4.addTask(new Task("SessionReview", "Session Review", Task.TYPE.questions, 0));
        session4.addTask(new Task("OA","Anxiety Review", Task.TYPE.questions, 1 ));
        session4.addTask(new Task("ReturnIntention","Returning to the Program", Task.TYPE.questions, 0));
        sessions.add(session4);

        session5 = new Session(FIFTH_SESSION, "Level 5: Master", 0, 5);
        session5.setIndex(5);

        session5.addTask(new Task("Affect","Current Feelings, Pre", "pre", Task.TYPE.questions, 0));
        session5.addTask(new Task("5", "Training Session 5", Task.TYPE.angular, 20));
        session5.addTask(new Task("Affect","Current Feelings, Post", "post", Task.TYPE.questions, 0));
        session5.addTask(new Task("CC","Compare and Contrast", Task.TYPE.questions, 0 ));      
        session5.addTask(new Task("SessionReview", "Session Review", Task.TYPE.questions, 0));
        session5.addTask(new Task("OA","Anxiety Review", Task.TYPE.questions, 1 ));
        session5.addTask(new Task("DASS21_AS","Mood Assessment", Task.TYPE.questions, 0 ));
        session5.addTask(new Task("recognitionRatings", "Completing Short Stories", Task.TYPE.angular, 5));
        session5.addTask(new Task("RR","Completing Short Stories, Pt. 2", Task.TYPE.questions, 0 ));
        session5.addTask(new Task("Comorbid","Mood, Drinking, and Marijuana Use", Task.TYPE.questions, 0 ));
        session5.addTask(new Task("Wellness","What I Believe", Task.TYPE.questions, 0 ));
        session5.addTask(new Task("Mechanisms","How I Respond", Task.TYPE.questions, 0 ));
        session5.addTask(new Task("Covid19","COVID-19", Task.TYPE.questions, 0 ));
        session5.addTask(new Task("HelpSeeking","Change in Help-Seeking Behavior", Task.TYPE.questions, 1));
        session5.addTask(new Task("Evaluation","Evaluating the Program", Task.TYPE.questions, 2));
        session5.addTask(new Task("AssessingProgram","Assessing the Program", Task.TYPE.questions, 2));
        sessions.add(session5);

        post = new Session(POST_FOLLOWUP, "2 Month Post Training", 0, 60);
        post.setIndex(6);

        post.addTask(new Task("OA","Anxiety Review", Task.TYPE.questions, 1 ));
        post.addTask(new Task("DASS21_AS","Mood Assessment", Task.TYPE.questions, 0 ));
        post.addTask(new Task("recognitionRatings", "Completing Short Stories", Task.TYPE.angular, 5));
        post.addTask(new Task("RR","Completing Short Stories, Pt. 2", Task.TYPE.questions, 0 ));
        post.addTask(new Task("Comorbid","Mood, Drinking, and Marijuana Use", Task.TYPE.questions, 0 ));
        post.addTask(new Task("Wellness","What I Believe", Task.TYPE.questions, 0 ));
        post.addTask(new Task("Mechanisms","How I Respond", Task.TYPE.questions, 0 ));
        post.addTask(new Task("Covid19","COVID-19", Task.TYPE.questions, 0 ));
        post.addTask(new Task("HelpSeeking","Change in Help-Seeking Behavior", Task.TYPE.questions, 1));
        sessions.add(post);

        return sessions;
    }
}