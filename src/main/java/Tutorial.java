/** Practice advances through demonstrated actions, never elapsed reading time. */
public final class Tutorial {
    public enum Step { MOVE, STRIKE, DODGE, CHARGE, PARRY, GEAR, HAVEN, COMPLETE }
    private Step step=Step.MOVE;
    private int hits;
    private float traveled,healingTime;
    public Step step() { return step; }
    public boolean complete() { return step==Step.COMPLETE; }
    public void movement(float distance) { if(step==Step.MOVE && Float.isFinite(distance)) { traveled+=Math.max(0,distance);if(traveled>=4)next(); } }
    public void hit(boolean charged) {
        if(step==Step.STRIKE && ++hits>=2 || step==Step.CHARGE && charged)next();
    }
    public void dodge() { if(step==Step.DODGE)next(); }
    public void parry() { if(step==Step.PARRY)next(); }
    public void inventory() { if(step==Step.GEAR)next(); }
    public void healing(float dt,boolean inside) { if(step==Step.HAVEN) { healingTime=inside?healingTime+Math.max(0,dt):0;if(healingTime>=3)next(); } }
    private void next() { step=Step.values()[step.ordinal()+1]; }
    public String title() { return switch(step) {
        case MOVE -> "Find your footing";case STRIKE -> "Aim and connect";case DODGE -> "Make space";
        case CHARGE -> "Commit to a heavy attack";case PARRY -> "Turn defense into an opening";
        case GEAR -> "Know your equipment";case HAVEN -> "Return to the light";case COMPLETE -> "Ready for the first watch";
    }; }
    public String instruction(Config.Controls controls,HeroClass hero) { return switch(step) {
        case MOVE -> "Move with "+controls.label(Config.Controls.Action.FORWARD)+" / "+controls.label(Config.Controls.Action.LEFT)+" / "+controls.label(Config.Controls.Action.BACK)+" / "+controls.label(Config.Controls.Action.RIGHT)+". Walk a few steps to continue.";
        case STRIKE -> "Point at the practice partner and click, or face them and press "+controls.label(Config.Controls.Action.SLASH)+". Land two hits on the marked partner.";
        case DODGE -> "Press "+controls.label(Config.Controls.Action.DODGE)+" while moving to dodge. Perform one dodge to continue.";
        case CHARGE -> "Hold "+controls.label(Config.Controls.Action.HEAVY)+" until the charge turns gold, then release toward the partner. Hit the marked partner.";
        case PARRY -> "Face the partner. Press "+controls.label(Config.Controls.Action.PARRY)+" just before their gold attack lands. You cannot be hurt here. Try again until you block the hit.";
        case GEAR -> "Open equipment with "+controls.label(Config.Controls.Action.INVENTORY)+". Inspect your weapon and armor, then close it with the same key.";
        case HAVEN -> "Follow the teal ring to the sanctuary. Stay inside for three seconds to recover. Outside practice, protect its integrity and watch the wave counter.";
        case COMPLETE -> "You have practiced the essentials. Class mastery rewards your combat style. Explore optional objectives and spend crystals at the sanctuary workshop.";
    }; }
}
