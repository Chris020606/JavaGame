package Entities;

public enum PlayerAnimation {
    BOW_IDLE("Ranged_Bow_Idle"),
    BOW_RELEASE("Ranged_Bow_Release"),
    MAGIC_CAST("Ranged_Magic_Spellcasting"),
    // Movement
    WALK("Walking_A.001"),
    WALK_BACKWARD("Walking_Backwards"),
    RUN("Running_A"),
    RUN_STRAFE_LEFT("Running_Strafe_Left"),
    RUN_STRAFE_RIGHT("Running_Strafe_Right"),
    SNEAK("Sneaking"),
    CRAWL("Crawling"),
    CROUCH("Crouching"),

    // Jump
    JUMP("Jump_Full_Short"),
    JUMP_LONG("Jump_Full_Long"),
    JUMP_START("Jump_Start"),
    JUMP_IDLE("Jump_Idle"),
    JUMP_LAND("Jump_Land"),

    // Dodge
    DODGE_FORWARD("Dodge_Forward"),
    DODGE_BACKWARD("Dodge_Backward"),
    DODGE_LEFT("Dodge_Left"),
    DODGE_RIGHT("Dodge_Right"),
    // One-handed combat

    ATTACK_CHOP("Melee_1H_Attack_Chop"),
    ATTACK_JUMP_CHOP("Melee_1H_Attack_Jump_Chop"),
    ATTACK_DIAGONAL("Melee_1H_Attack_Slice_Diagonal"),
    ATTACK_HORIZONTAL("Melee_1H_Attack_Slice_Horizontal"),
    ATTACK_STAB("Melee_1H_Attack_Stab"),

    // Blocking
    BLOCK_IDLE("Melee_Block"),
    BLOCK("Melee_Blocking"),
    BLOCK_ATTACK("Melee_Block_Attack"),
    BLOCK_HIT("Melee_Block_Hit"),

    // Idle
    MELEE_IDLE("Melee_2H_Idle"),
    UNARMED_IDLE("Melee_Unarmed_Idle");
    private final String animationName;

    PlayerAnimation(String animationName) {
        this.animationName = animationName;
    }

    public String getAnimationName() {
        return animationName;
    }
}
