package com.rs.content.dialogues.impl;

import com.rs.content.actions.skills.dungeoneering.Dungeoneering;
import com.rs.content.dialogues.Dialogue;

public class DungLad extends Dialogue {

    @Override
    public void start() {
        sendPlayerDialogue(SEND_4_OPTIONS, "Which dung floor would you like?",
                "Easy", "Medium", "Hard", "Extreme"); // Change options maybe?
    }

    public void run(int interfaceId, int componentId) {
        if (interfaceId == SEND_4_OPTIONS) {
            if (componentId == 1) {
                Dungeoneering.startDungeon(2, 2, 4, player);
                sendPlayerDialogue(SEND_2_TEXT_CHAT, "",
                        "You have entered the Easy Dung, i will be adding", // You
                        // can
                        // change
                        // txt
                        "more to them later including working party. Report bugs.");
            }
            if (componentId == 2) {
                Dungeoneering.startDungeon(9, 3, 6, player);
                sendPlayerDialogue(SEND_2_TEXT_CHAT, "",
                        "You have entered the Medium Dung, i will be adding", // You
                        // can
                        // change
                        // txt
                        "more to them later including working party. Report bugs.");
            }
            if (componentId == 3) {
                Dungeoneering.startDungeon(15, 5, 7, player);
                sendPlayerDialogue(SEND_2_TEXT_CHAT, "",
                        "You have entered the Hard Dung, i will be adding", // You
                        // can
                        // change
                        // txt
                        "more to them later including working party. Report bugs.");
            }
            if (componentId == 4) {
                Dungeoneering.startDungeon(25, 6, 9, player);
                sendPlayerDialogue(SEND_2_TEXT_CHAT, "",
                        "You have entered the Extreme Dung, i will be adding", // You
                        // can
                        // change
                        // txt
                        "more to them later including working party. Report bugs.");
            }
        }
    }

    @Override
    public void finish() {

    }

}