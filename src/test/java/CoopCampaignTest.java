import static org.junit.jupiter.api.Assertions.*;

import Entities.Difficulty;

import org.junit.jupiter.api.Test;

class CoopCampaignTest {
    @Test
    void twoInputDrivenHeroesCanCompleteTheBalancedCampaign() {
        for (Difficulty difficulty : new Difficulty[] {Difficulty.EASY, Difficulty.NORMAL}) {
            CoopWorld world = new CoopWorld(difficulty, 321);
            world.attach(0, "One", HeroClass.WARRIOR);
            world.attach(1, "Two", HeroClass.MAGE);
            CoopState state = world.snapshot();
            int steps = 0, maxChapter = 0, retries = 0;
            CoopState.Phase last = state.phase();
            while (state.phase() != CoopState.Phase.VICTORY && retries <= 3 && steps < 140000) {
                if (state.phase() == CoopState.Phase.DEFEAT && last != CoopState.Phase.DEFEAT)
                    retries++;
                last = state.phase();
                world.input(0, CoopBot.input(state, 0, steps + 1));
                world.input(1, CoopBot.input(state, 1, steps + 1));
                world.tick(1f / 60);
                state = world.snapshot();
                maxChapter = Math.max(maxChapter, state.chapter());
                steps++;
            }
            System.out.println(
                    "COOP BALANCE "
                            + difficulty
                            + " phase="
                            + state.phase()
                            + " chapter="
                            + state.chapter()
                            + " kills="
                            + state.kills()
                            + " seconds="
                            + steps / 60f
                            + " retries="
                            + retries
                            + " base="
                            + state.baseHealth()
                            + " hp="
                            + state.heroes().stream().map(h -> h.health() + "").toList());
            if (state.phase() != CoopState.Phase.VICTORY) {
                System.out.println("HEROES " + state.heroes());
                System.out.println("ENEMIES " + state.enemies());
            }
            assertEquals(
                    CoopState.Phase.VICTORY,
                    state.phase(),
                    "Two-player " + difficulty + " campaign failed at chapter " + maxChapter);
            assertEquals(3, maxChapter);
            assertTrue(state.kills() > 60);
        }
    }
}
