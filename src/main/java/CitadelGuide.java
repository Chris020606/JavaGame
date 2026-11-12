/** City objectives use world compass directions, independent of the orbiting camera. */
public final class CitadelGuide {
    public record Destination(String title, String instruction, float x, float z) {
        public int distance(float playerX, float playerZ) {
            return Math.round((float) Math.hypot(x - playerX, z - playerZ));
        }

        public String direction(float playerX, float playerZ) {
            if (distance(playerX, playerZ) <= 3) return "HERE";
            String[] directions = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};
            double angle = Math.atan2(x - playerX, playerZ - z);
            int index = Math.floorMod((int) Math.round(angle / (Math.PI / 4)), 8);
            return directions[index];
        }
    }

    public static Destination destination(Progression progress) {
        if (!progress.hasWeapon()) {
            for (CitadelNpc npc : CitadelNpc.values())
                if (npc.mentorClass() == progress.heroClass())
                    return new Destination(
                            npc.npcName(), "Claim your starter weapon", npc.x(), npc.z());
        }
        return new Destination(
                "Expedition Gate",
                "Enter the portal when ready",
                CitadelHub.PORTAL_X,
                CitadelHub.PORTAL_Z);
    }

    private CitadelGuide() {}
}
