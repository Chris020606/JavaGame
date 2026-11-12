/** The finite story route. Global wave numbers keep older saves and enemy scaling compatible. */
public final class Campaign {
    public record Chapter(
            String title, int first, int last, boolean bossOnly, String story, String aftermath) {
        public int waves() {
            return last - first + 1;
        }

        public int cleared(int wave, int queued, int living, int total) {
            int completed = wave - first + (queued == 0 && living == 0 && total > 0 ? 1 : 0);
            return Math.max(0, Math.min(waves(), completed));
        }
    }

    public static final Chapter[] CHAPTERS = {
        new Chapter(
                "THE LAST EMBER",
                1,
                4,
                false,
                "A century ago, the Celestial Sun-Spires fell to the Hollow Frost, plunging"
                    + " Aethelgard into eternal twilight. Only this Primordial Beacon endures—a"
                    + " sacred relic holding the realm's last living ember. If the beacon is"
                    + " extinguished, the frost claims the sanctuary and all life perishes. Hold"
                    + " the perimeter, awaken its ancient solar ley-lines, and protect the light at"
                    + " all costs.",
                "The Primordial Beacon blazes to life, casting golden warmth across the pine. But"
                        + " across the barrows, an ancient corruption stirs in response."),
        new Chapter(
                "THE ASH WARDEN",
                5,
                5,
                true,
                "The Ash Cemetery guards the mountain passage to the ancient sun-forges. Its"
                    + " once-noble keeper swore an eternal oath to protect the valley, but the"
                    + " Hollow Frost corrupted his soul into the dreaded Ash Warden. Defeat your"
                    + " fallen predecessor beneath the crypts and reclaim the Sunstone Seal.",
                "The Ash Warden falls. His broken seal clears the high northern pass to the Crown"
                        + " of Dawn."),
        new Chapter(
                "ROAD TO DAWN",
                6,
                9,
                false,
                "Blizzards howl through the high glacier pass. Sensing the Beacon's rising light,"
                        + " the Hollow King has sent boneguards, dead archers and frost hexers to"
                        + " ambush the last refugee caravan. Guard the ancient Waystones until the"
                        + " survivors reach sanctuary.",
                "The caravan reaches the warmth of your sanctuary. Only the Hollow King stands"
                        + " between the valley and the eternal morning."),
        new Chapter(
                "THE HOLLOW KING",
                10,
                10,
                true,
                "At the zenith of the Necropolis sits the Hollow King—the ancient tyrant who"
                    + " swallowed the suns and brought the winter of souls. Face him in his throne"
                    + " of frost, shatter his frozen crown, and let this be the last long night.",
                "At dawn, the Primordial Beacon ignites the sky. Golden warmth washes over the"
                    + " valley, melting a century of frost. The road home is open. Your watch is"
                    + " complete, Warden of the Dawn.")
    };

    public static Chapter chapter(int wave) {
        for (Chapter chapter : CHAPTERS) if (wave <= chapter.last()) return chapter;
        return CHAPTERS[CHAPTERS.length - 1];
    }

    public static int index(int wave) {
        for (int i = 0; i < CHAPTERS.length; i++) if (wave <= CHAPTERS[i].last()) return i;
        return CHAPTERS.length - 1;
    }

    public static boolean boundary(int wave) {
        return wave == 4 || wave == 5 || wave == 9 || wave >= 10;
    }

    public static int enemies(int wave) {
        return chapter(wave).bossOnly() ? 1 : Math.min(36, 4 + wave * 3);
    }

    private Campaign() {}
}
