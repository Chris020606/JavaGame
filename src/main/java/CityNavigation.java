/** Optional service tracking stays separate from the character's required starter objective. */
public final class CityNavigation {
    private CitadelNpc tracked;

    public boolean track(CitadelNpc npc) {
        if (npc == null || npc.isSurvivor()) return false;
        tracked = npc;
        return true;
    }

    public boolean visit(CitadelNpc citizen, Progression progress) {
        if (!progress.hasWeapon()) return false;
        for (CitadelNpc home : CitadelNpc.rescuedNpcs(progress.rescuedSurvivors())) {
            if (home == citizen) {
                tracked = citizen;
                return true;
            }
        }
        return false;
    }

    public void clear() {
        tracked = null;
    }

    public CitadelNpc tracked() {
        return tracked;
    }

    public CitadelGuide.Destination destination(Progression progress) {
        // A new unarmed warden always sees their essential mentor objective.
        if (tracked == null || !progress.hasWeapon()) return CitadelGuide.destination(progress);
        boolean gate = tracked == CitadelNpc.COMMANDER;
        return new CitadelGuide.Destination(
                gate ? "Expedition Gate" : tracked.npcName(),
                gate
                        ? "Enter the portal when ready"
                        : tracked == CitadelNpc.CARAVAN_TRADER
                                ? "Potions and caravan upgrades"
                                : tracked.isSurvivor()
                                        ? "Visit them in the city"
                                        : "Class weapons and armor",
                gate ? CitadelHub.PORTAL_X : tracked.x(),
                gate ? CitadelHub.PORTAL_Z : tracked.z());
    }
}
