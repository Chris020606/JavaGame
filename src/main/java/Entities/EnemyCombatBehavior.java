package Entities;

/**
 * Combat movement and spatial intelligence rules for enemies.
 *
 * <p>Governs tactical positioning, readable kiting for ranged attackers, encirclement and crowding
 * prevention for melee ranks, and obstacle/boundary deflection.
 *
 * <p>Pure simulation logic: no native rendering or desktop input dependencies.
 */
public final class EnemyCombatBehavior {
    public static final float ARENA_LIMIT = 22.0f;
    public static final float ARENA_MARGIN = 19.5f;

    public record Motion(float vx, float vz, String action) {}

    private EnemyCombatBehavior() {}

    /**
     * Calculates the tactical motion vector and high-level behavioral action for an enemy.
     *
     * @param enemy the enemy unit
     * @param tx target X coordinate (e.g. hero or objective)
     * @param tz target Z coordinate
     * @param separationX external squad separation push from nearby allies
     * @param separationZ external squad separation push from nearby allies
     * @return tactical motion vector and action label
     */
    public static Motion planMotion(
            Enemy enemy, float tx, float tz, float separationX, float separationZ) {
        float dx = tx - enemy.x;
        float dz = tz - enemy.z;
        float d = (float) Math.hypot(dx, dz);

        // Zero-distance fallback: determine escape normal if perfectly overlapping target
        float nx;
        float nz;
        if (d < 0.001f) {
            float rad = (float) Math.toRadians(enemy.facing);
            nx = (float) Math.sin(rad);
            nz = (float) Math.cos(rad);
            if (nx == 0 && nz == 0) {
                nx = enemy.personality > 0.5f ? 1.0f : -1.0f;
                nz = 0.0f;
            }
            d = 0.001f;
        } else {
            nx = dx / d;
            nz = dz / d;
        }

        float vx = 0;
        float vz = 0;
        float side = enemy.personality > 0.5f ? 1.0f : -1.0f;
        String action = "APPROACH";

        if (enemy.ranged()) {
            // RANGED BEHAVIOR (Archer & Shaman)
            // On Easy, retreat threshold is slightly closer and kiting is gentler to prevent
            // frustrating chases.
            float retreatThreshold =
                    enemy.difficulty == Difficulty.EASY
                            ? 3.8f
                            : enemy.difficulty == Difficulty.NORMAL ? 4.5f : 4.8f;

            if (d < retreatThreshold) {
                // Backpedal with lateral evasion
                float kiteSpeed = enemy.difficulty == Difficulty.EASY ? 0.65f : 0.85f;
                vx = (-nx + nz * side * 0.45f) * kiteSpeed;
                vz = (-nz - nx * side * 0.45f) * kiteSpeed;
                action = "RETREAT";

                // Deflect retreat away from arena walls so archers don't back themselves into
                // corners
                if (Math.abs(enemy.x) > ARENA_MARGIN || Math.abs(enemy.z) > ARENA_MARGIN) {
                    float toCenterX = -enemy.x / ARENA_LIMIT;
                    float toCenterZ = -enemy.z / ARENA_LIMIT;
                    vx = vx * 0.5f + toCenterX * 0.5f;
                    vz = vz * 0.5f + toCenterZ * 0.5f;
                }
            } else if (d < 7.4f) {
                // Tactical sweet spot: strafe when on cooldown or when line of sight is obstructed
                if (enemy.cooldown > 0.15f || enemy.sideStepTime > 0) {
                    float strafeSpeed =
                            enemy.difficulty == Difficulty.EASY
                                    ? 0.50f
                                    : enemy.sideStepTime > 0 ? 0.65f : 0.55f;

                    // Test if strafing in preferred direction moves into an arena wall; flip if
                    // obstructed
                    float testX = enemy.x + nz * side * strafeSpeed * 1.5f;
                    float testZ = enemy.z - nx * side * strafeSpeed * 1.5f;
                    if (Math.abs(testX) > ARENA_LIMIT - 0.5f
                            || Math.abs(testZ) > ARENA_LIMIT - 0.5f) {
                        side = -side;
                    }

                    vx = nz * side * strafeSpeed;
                    vz = -nx * side * strafeSpeed;
                    action = "STRAFE";
                } else {
                    // Ready to fire: micro-adjust facing and hold effective range
                    vx = nx * 0.10f;
                    vz = nz * 0.10f;
                    action = "APPROACH";
                }
            } else {
                // Advance into effective firing range
                vx = nx;
                vz = nz;
                action = "APPROACH";
            }
        } else if (!enemy.boss() && enemy.cooldown > 0.35f && d < 3.0f && enemy.variant != 2) {
            // MELEE RECOVERY (Post-attack breathing room)
            vx = -nx * 0.65f + nz * side * 0.25f;
            vz = -nz * 0.65f - nx * side * 0.25f;
            action = "RECOVER";
        } else {
            // MELEE APPROACH & FLANKING
            float stopDistance = enemy.boss() ? 2.2f : enemy.variant == 2 ? 1.8f : 1.45f;

            if (d > stopDistance) {
                float flank = 0.0f;
                if (!enemy.boss() && d > 3.0f && d < 10.0f) {
                    // Scouts take wider flank paths; raiders distribute based on personality;
                    // brutes take center
                    if (enemy.variant == 1) {
                        flank = 0.75f * side;
                    } else if (enemy.variant == 0) {
                        flank =
                                (enemy.personality > 0.6f
                                        ? 0.35f
                                        : enemy.personality < 0.4f ? -0.35f : 0.0f);
                    } else if (enemy.variant == 2) {
                        flank = 0.0f; // Heavy brute holds center line
                    } else {
                        flank = 0.35f * side;
                    }
                } else if (!enemy.boss() && enemy.variant == 1 && d <= 3.0f && d > stopDistance) {
                    // Scouts continue active flanking in close quarters rather than collapsing into
                    // a straight line
                    flank = 0.45f * side;
                }

                vx = nx + nz * flank;
                vz = nz - nx * flank;
                action = flank != 0.0f ? "FLANK" : "APPROACH";
            } else if (!enemy.boss() && d < 0.5f) {
                // Point-blank / zero-distance overlap resolution: step out to natural melee
                // distance
                vx = -nx * 0.6f;
                vz = -nz * 0.6f;
                action = "RECOVER";
            } else {
                action = "APPROACH";
            }
        }

        // Apply external ally separation with anti-crowding damping
        float sepWeight =
                enemy.variant == 2 ? 0.55f : 0.90f; // Brutes hold ground against lighter allies
        float sepMag = (float) Math.hypot(separationX, separationZ);
        if (sepMag > 1.5f) {
            separationX = (separationX / sepMag) * 1.5f;
            separationZ = (separationZ / sepMag) * 1.5f;
        }

        // In close melee range, deflect separation tangentially around the target circle
        if (!enemy.ranged() && d < 3.5f && d > 0.1f && sepMag > 0.01f) {
            float tangentX = -nz;
            float tangentZ = nx;
            float dotTangent = separationX * tangentX + separationZ * tangentZ;
            vx += tangentX * dotTangent * sepWeight;
            vz += tangentZ * dotTangent * sepWeight;
            vx += separationX * sepWeight * 0.3f;
            vz += separationZ * sepWeight * 0.3f;
        } else {
            vx += separationX * sepWeight;
            vz += separationZ * sepWeight;
        }

        // Soft boundary repulsion near arena perimeters
        if (Math.abs(enemy.x) > ARENA_MARGIN || Math.abs(enemy.z) > ARENA_MARGIN) {
            if (enemy.x > ARENA_MARGIN && vx > 0) {
                vx -= (enemy.x - ARENA_MARGIN) / (ARENA_LIMIT - ARENA_MARGIN) * Math.abs(vx);
            } else if (enemy.x < -ARENA_MARGIN && vx < 0) {
                vx += (-ARENA_MARGIN - enemy.x) / (ARENA_LIMIT - ARENA_MARGIN) * Math.abs(vx);
            }
            if (enemy.z > ARENA_MARGIN && vz > 0) {
                vz -= (enemy.z - ARENA_MARGIN) / (ARENA_LIMIT - ARENA_MARGIN) * Math.abs(vz);
            } else if (enemy.z < -ARENA_MARGIN && vz < 0) {
                vz += (-ARENA_MARGIN - enemy.z) / (ARENA_LIMIT - ARENA_MARGIN) * Math.abs(vz);
            }
        }

        // Normalize velocity vector if exceeding unit magnitude
        float magnitude = (float) Math.hypot(vx, vz);
        if (magnitude > 1.0f) {
            vx /= magnitude;
            vz /= magnitude;
        }

        return new Motion(vx, vz, action);
    }
}
