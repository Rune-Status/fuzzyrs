package com.rs.content.dialogues;

/**
 * @author FuzzyAvacado
 */
public enum DialogueExpressions {

    REALLY_SAD(9760), SAD(9765), DEPRESSED(9770), WORRIED(9775), SCARED(9780), MEAN_FACE(
            9785), MEAN_HEAD_BANG(9790), EVIL(9795), WHAT_THE_CRAP(9800), CALM(
            9805), CALM_TALK(9810), TOUGH(9815), SNOBBY(9820), SNOBBY_HEAD_MOVE(
            9825), CONFUSED(9830), DRUNK_HAPPY_TIRED(9835), TALKING_ALOT(9845), HAPPY_TALKING(
            9850), BAD_ASS(9855), THINKING(9860), COOL_YES(9864), LAUGH_EXCITED(
            9851), SECRETLY_TALKING(9838);

    private int id;

    DialogueExpressions(int id) {
        this.id = id;
    }

    /**
     * @return the anim
     */
    public int getId() {
        return id;
    }

    /**
     * @param id the anim to set
     */
    public void setId(int id) {
        this.id = id;
    }
}
