package com.rs.content.dialogues.impl;

import com.rs.content.dialogues.Dialogue;
import com.rs.core.cache.loaders.NPCDefinitions;
import com.rs.player.content.PartyRoom;

public class PartyPete extends Dialogue {

    @Override
    public void start() {
        sendEntityDialogue(
                SEND_3_TEXT_CHAT,
                new String[]{
                        NPCDefinitions.getNPCDefinitions(659).name,
                        "The items in the party chest are worth "
                                + PartyRoom.getTotalCoins() + "",
                        "coins! Hang around until they drop and you might get",
                        "something valueable!"}, IS_NPC, 659, 9843);
    }

    @Override
    public void run(final int interfaceId, final int componentId) {
        end();
    }

    @Override
    public void finish() {

    }

}