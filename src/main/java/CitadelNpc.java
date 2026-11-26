public enum CitadelNpc {
    BLACKSMITH(
            "Goran the Iron Hand",
            "BLACKSMITH & ARMORER",
            -7.5f,
            0,
            1.5f,
            45f,
            "resources/KayKit_Adventurers_2.0_FREE/Characters/gltf/Knight.glb",
            "The Primordial Beacon's fire burns hot enough to temper Dawnsteel. Bring me crystals"
                + " and raise your battle rank, and I'll forge you weapons and armor that shatter"
                + " the frost legions.",
            "Dawnsteel armor is forged in the core embers of the Beacon. Only a seasoned Warden (Lv"
                    + " 10) can wield its full weight without freezing in the high passes."),
    ARCANIST(
            "Lady Elyndra",
            "BEACON SCHOLAR & ALCHEMIST",
            7.5f,
            0,
            1.5f,
            -45f,
            "resources/KayKit_Adventurers_2.0_FREE/Characters/gltf/Mage.glb",
            "The Beacon is not merely a flame; it is the heartbeat of our world. As mentor of"
                    + " Mages, I attune staves, conduits, and enchanted robes for those who wield"
                    + " arcane power.",
            "The ancient texts say the Beacon holds back the Frost Sovereign's curse. If its core"
                    + " ever cools completely, the entire valley will freeze for eternity."),
    COMMANDER(
            "Commander Sean",
            "EXPEDITION GATEKEEPER",
            2.6f,
            0,
            29.5f,
            -60f,
            "resources/outpost/raider.glb",
            "Warden! Our scouts report heavy frost movements in the valley sectors. The gateway is"
                    + " aligned and charged with Beacon energy. Step into the portal when you are"
                    + " ready to deploy.",
            "Each chapter brings us closer to the Hollow King's fortress. Protect the Beacon in the"
                    + " field at all costs—if you fall, the expedition fails."),
    CARAVAN_TRADER(
            "Torvald the Caravan Master",
            "MERCHANT CARAVAN LEADER",
            -2.5f,
            0,
            15.5f,
            90f,
            "resources/KayKit_Adventurers_2.0_FREE/Characters/gltf/Barbarian.glb",
            "Hail, Warden! Our merchant caravan just arrived through the high mountain passes. We"
                + " bring sturdy pack wagons, fresh trail rations, and restorative healing potions."
                + " Restock your satchel before you march past the south gates!",
            "The trade routes through the frost valleys are perilous, but as long as the Citadel"
                    + " stands and the Beacon burns, our caravans will keep supply lines open."),
    ARCHER(
            "Diana the Huntress",
            "ARCHER MENTOR & FLETCHER",
            -7.5f,
            0,
            -5.5f,
            45f,
            "resources/outpost/hero_archer.glb",
            "Keep your draw steady and your eye sharp, marksman! I craft precision recurve bows"
                    + " and hunting crossbows. Show me your skill on the training range, and I will"
                    + " carve you arms fit for a master.",
            "A good bow is an extension of the archer's will. Against the frost legions, distance"
                    + " and true aim are your greatest allies."),
    SURVIVOR_BRAM(
            "Bram the Woodcutter",
            "RESCUED CITIZEN",
            -8.0f,
            0,
            7.5f,
            25f,
            "resources/outpost/citizens/bram.glb",
            "Warden! You pulled me out of that freezing pass. The hearth of Aethelgard Citadel has"
                    + " never felt so warm. Our people owe you our lives!",
            "I saw the frost raiders coming down from the ridge. If you hadn't arrived and escorted"
                    + " me to the sanctuary, I would be another frozen corpse."),
    SURVIVOR_KAELEN(
            "Kaelen the Scout",
            "RESCUED REFUGEE",
            -10.0f,
            0,
            11.5f,
            -35f,
            "resources/outpost/citizens/kaelen.glb",
            "I thought the shadows had claimed me for sure until you broke their lines. Bless the"
                    + " Primordial Beacon, and bless your blade!",
            "I'm keeping watch over the southern ramparts now. Whenever you venture out, remember"
                    + " that everyone inside these walls is counting on you."),
    SURVIVOR_ROWAN(
            "Rowan the Herbalist",
            "RESCUED SURVIVOR",
            9.0f,
            0,
            8.5f,
            -60f,
            "resources/outpost/citizens/rowan.glb",
            "Thank you for saving me, Warden! I brought whatever healing herbs I could salvage to"
                    + " help Lady Elyndra brew tonics for our defenders.",
            "The frost beasts fear the Beacon's light. As long as you keep its flame burning"
                    + " bright on your expeditions, we have hope."),
    SURVIVOR_THERON(
            "Theron the Mason",
            "RESCUED ELDER",
            6.0f,
            0,
            14.0f,
            15f,
            "resources/outpost/citizens/theron.glb",
            "In all my years in the valley, I have never seen such courage. You brought me home"
                    + " through the blizzard. May the sun shine upon you!",
            "These castle walls were built by our ancestors to guard the Sun-Spires. With you"
                    + " defending the Beacon, the Citadel will stand strong."),
    SURVIVOR_MIRA(
            "Mira the Weaver",
            "RESCUED TAILOR",
            -4f,
            0,
            20f,
            90f,
            "resources/outpost/citizens/mira.glb",
            "I thought the caravan would never reach home. Now I can mend coats for the families"
                    + " arriving through the Beacon.",
            "A warm cloak matters as much as steel in these valleys. Every family you bring back"
                    + " gives this city another reason to keep going."),
    SURVIVOR_OWEN(
            "Owen the Baker",
            "RESCUED BAKER",
            5f,
            0,
            22f,
            -90f,
            "resources/outpost/citizens/owen.glb",
            "The ovens are cold, but I saved my starter and a sack of grain. We will have bread"
                    + " again, thanks to you.",
            "The gangs took our village stores before winter. Here, we share what we have. Bring"
                    + " the others home, Warden.");

    public static final CitadelNpc[] RESCUED_SURVIVORS = {
        SURVIVOR_BRAM,
        SURVIVOR_KAELEN,
        SURVIVOR_ROWAN,
        SURVIVOR_THERON,
        SURVIVOR_MIRA,
        SURVIVOR_OWEN
    };

    public static CitadelNpc[] rescuedNpcs(int count) {
        int n = Math.max(0, Math.min(RESCUED_SURVIVORS.length, count));
        CitadelNpc[] result = new CitadelNpc[n];
        System.arraycopy(RESCUED_SURVIVORS, 0, result, 0, n);
        return result;
    }

    public boolean isSurvivor() {
        return this != BLACKSMITH
                && this != ARCANIST
                && this != ARCHER
                && this != COMMANDER
                && this != CARAVAN_TRADER;
    }

    public HeroClass mentorClass() {
        return switch (this) {
            case BLACKSMITH -> HeroClass.WARRIOR;
            case ARCANIST -> HeroClass.MAGE;
            case ARCHER -> HeroClass.ARCHER;
            default -> null;
        };
    }

    private final String name;
    private final String title;
    private final float x;
    private final float y;
    private final float z;
    private final float yaw;
    private final String modelPath;
    private final String greeting;
    private final String lore;

    CitadelNpc(
            String name,
            String title,
            float x,
            float y,
            float z,
            float yaw,
            String modelPath,
            String greeting,
            String lore) {
        this.name = name;
        this.title = title;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.modelPath = modelPath;
        this.greeting = greeting;
        this.lore = lore;
    }

    public String npcName() {
        return name;
    }

    public String title() {
        return title;
    }

    public float x() {
        return x;
    }

    public float y() {
        return y;
    }

    public float z() {
        return z;
    }

    public float yaw() {
        return yaw;
    }

    public String modelPath() {
        return modelPath;
    }

    public String greeting() {
        return greeting;
    }

    public String dynamicGreeting(Progression progress) {
        if (progress == null) return greeting;
        CitadelNpc mentor = CitadelGuide.mentor(progress.heroClass());
        if (mentorClass() != null && this != mentor)
            return "Your mentor is "
                    + mentor.npcName()
                    + ". "
                    + switch (mentor) {
                        case BLACKSMITH -> "Follow the sound of the hammer west of the main road.";
                        case ARCANIST -> "Look east of the main road for her sanctum.";
                        default -> "Look for the archery targets northwest of the square.";
                    };
        if (!progress.hasWeapon()) {
            return switch (this) {
                case BLACKSMITH ->
                        "Try the grip. I wrapped it in leather so your hand won't stick to the"
                                + " steel out there.";
                case ARCANIST ->
                        "Here, hold this staff near the ember. See the grain catch the light?"
                                + " That's how you'll find your aim in the dark.";
                case ARCHER ->
                        "I strung this one indoors. Give the string a pull before you take it out"
                                + " into the cold.";
                case COMMANDER ->
                        "Pick up your weapon from "
                                + mentor.npcName()
                                + " first. I'll hold the gate.";
                default -> greeting;
            };
        }
        return switch (this) {
            case BLACKSMITH -> "Lay your kit on the bench. Let's see what those crystals will buy.";
            case ARCANIST ->
                    "Set your staff by the brazier. I can work the crystals into the wood or weave"
                            + " them into a robe.";
            case ARCHER ->
                    "Bowstrings fray in this cold. Let me see yours while you look over the bows"
                            + " and leathers.";
            default -> greeting;
        };
    }

    String blessingReply() {
        return switch (this) {
            case SURVIVOR_BRAM ->
                    "I'll split another stack before dark. Nobody under this roof ought to wake up"
                            + " cold.";
            case SURVIVOR_KAELEN ->
                    "I'll take the dawn watch. If anyone makes it down that road, I'll see them.";
            case SURVIVOR_ROWAN ->
                    "Here, smell that. The mint I brought through the gate has put out its first"
                            + " leaves.";
            case SURVIVOR_THERON ->
                    "See that pale stone in the wall? My first patch since I came home. It'll"
                            + " hold.";
            case SURVIVOR_MIRA ->
                    "I saved a length of blue wool for the next little one through the gate.";
            case SURVIVOR_OWEN ->
                    "Come by when the ovens cool. I kept you the heel of the first loaf.";
            default -> throw new IllegalStateException("Only rescued citizens receive a blessing");
        };
    }

    public String lore() {
        return lore;
    }

    public boolean isNear(float px, float pz, float radius) {
        float dx = px - x, dz = pz - z;
        return (dx * dx + dz * dz) <= (radius * radius);
    }
}
