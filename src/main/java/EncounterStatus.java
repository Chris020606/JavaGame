/** Read-only encounter presentation. Does not advance waves or change rescue timers. */
record EncounterStatus(String title, String detail, float clearedFraction) {
    static EncounterStatus of(
            Campaign.Chapter chapter,
            int wave,
            int queued,
            int living,
            int total,
            float rest,
            boolean pendingRescue,
            boolean complete) {
        int local = Math.max(1, wave - chapter.first() + 1);
        float fraction =
                total <= 0
                        ? 0
                        : Math.max(0, Math.min(1, (total - queued - living) / (float) total));
        if (complete) return new EncounterStatus("CHAPTER SECURED", "THE BEACON ENDURES", 1);
        if (pendingRescue && queued == 0 && living == 0)
            return new EncounterStatus("FIELD SECURED", "SURVIVOR AWAITING EXTRACTION", 1);
        if (wave < chapter.first() || total == 0)
            return new EncounterStatus(
                    chapter.bossOnly() ? "BOSS APPROACHING" : "PREPARE FOR BATTLE",
                    "ARRIVAL IN " + Math.max(0, (int) Math.ceil(rest)) + "s",
                    0);
        if (queued == 0 && living == 0)
            return new EncounterStatus(
                    "WAVE " + local + " CLEARED",
                    "NEXT WAVE IN " + Math.max(0, (int) Math.ceil(rest)) + "s",
                    1);
        return new EncounterStatus(
                chapter.bossOnly() ? "BOSS DUEL" : "WAVE " + local + " OF " + chapter.waves(),
                living
                        + " IN THE FIELD"
                        + (queued > 0 ? " / " + queued + " INCOMING" : " / FINAL ENEMIES"),
                fraction);
    }
}
