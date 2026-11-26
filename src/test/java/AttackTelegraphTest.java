import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class AttackTelegraphTest {
    @Test
    void warningCompletesAtDamageNotAtRecoveryEnd() {
        for (float duration : new float[] {.42f, .6f, 1.2f, 1.65f, 2}) {
            assertEquals(0, AttackTelegraph.progress(duration, duration), .001f);
            assertEquals(.5f, AttackTelegraph.progress((duration + .25f) / 2, duration), .001f);
            assertEquals(1, AttackTelegraph.progress(.25f, duration));
            assertEquals(1, AttackTelegraph.progress(0, duration));
        }
    }
}
