define([], function () {
    return ({
        display_length: 9,
        add_extra_missing_letter: false,
        sequence: [
            {
                input: [
                    {handle: 'space', on: 'space'}
                ],
                layout: [
                    // This is a stimulus object
                    {
                        media: {template: "/PIPlayerScripts/introRecognition.html"}
                    }
                ],
                interactions: [
                    // This is an interaction (it has a condition and an action)
                    {
                        conditions: [
                            {type: 'inputEquals', value: 'space'}
                        ],
                        actions: [
                            {type: 'endTrial'}
                        ]
                    }
                ]
            },
            {
                "inherit": {
                    "set": "posneg",
                    "type": "random"
                },
                "stimuli": [
                    {"inherit": {"set": "error"}},
                    {
                        "data": {
                            "negativeKey": "l",
                            "negativeWord": "fai[ ]ure",
                            "positiveKey": "u",
                            "positiveWord": "s[ ]ccess",
                            "negation": "you will not let your teammates down, and",
                            "statement": "After being inactive for a few years, you recently joined a recreational soccer league. There is a tournament at the end of the season. You believe that [negation] you will contribute to your team’s "
                        },
                        "handle": "paragraph",
                        "media": {
                            "inlineTemplate": "<div class='sentence'><%= stimulusData.statement %><span class='incomplete' style='white-space:nowrap;'><%= trialData.positive ? stimulusData.positiveWord : stimulusData.negativeWord %></span></div>"
                        }
                    },
                    {
                        "handle": "question",
                        data: {

                            positiveAnswer: "y",
                            negativeAnswer: "n"
                        },
                        "media": {
                            "inlineTemplate": "<div> Will your performance probably contribute to the team’s success? </div>"
                        }
                    },
                    {
                        "handle": "multipleChoice",
                        data: {

                            positiveAnswer: "a",
                            negativeAnswer: "b"
                        },
                        "media": {
                            "inlineTemplate": "<div> <p> Your performance will likely: </p> " +
                            "<p> a) help your team win </p>" +
                            "<p> b) drag your team down </p> </div>"
                        }
                    },
                    {"inherit": {"set": "yesno"}},
                    {"inherit": {"set": "stall"}}, {"inherit": {"set": "greatjob"}},
                    {"inherit": {"set": "press_space"}},
                    {"inherit": {"set": "counter"}}
                ]
            }
        ]
    });
});
/* don't forget to close the define wrapper */
