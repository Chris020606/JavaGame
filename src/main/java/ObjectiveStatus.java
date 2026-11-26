/** Read-only encounter status uses the same escort distances as FieldEvent movement. */
record ObjectiveStatus(
        String title, String instruction, String detail, float progress, boolean guideToBeacon) {
    static ObjectiveStatus of(FieldEvent event, String citizen, float px, float pz, String key) {
        float distance = (float) Math.hypot(event.x() - px, event.z() - pz);
        int metres = Math.round(distance);
        String timer = Math.max(0, (int) Math.ceil(event.time())) + "s LEFT";
        if (event.kind() == FieldEvent.Kind.SURVIVOR) {
            String instruction =
                    event.status() == FieldEvent.Status.WAITING
                            ? (distance < 3
                                    ? "[" + key + "] TALK TO SURVIVOR"
                                    : "SURVIVOR / " + metres + "m AWAY")
                            : event.underAttack()
                                    ? "UNDER ATTACK / Protect " + citizen.split(" ", 2)[0]
                                    : distance >= FieldEvent.FOLLOW_RANGE
                                                    && distance < FieldEvent.CALL_RANGE
                                            ? event.walking()
                                                    ? "COMING TO YOUR CALL"
                                                    : "WAITING / Hold [" + key + "] to call"
                                            : distance >= FieldEvent.CALL_RANGE
                                                    ? "WAITING FOR YOU / " + metres + "m AWAY"
                                                    : distance <= 2
                                                            ? "LEAD THE WAY TO THE BEACON"
                                                            : "FOLLOWING / BEACON "
                                                                    + Math.round(
                                                                            Math.hypot(
                                                                                    event.x(),
                                                                                    event.z()))
                                                                    + "m";
            return new ObjectiveStatus(
                    citizen,
                    instruction,
                    "HEALTH " + Math.round(event.health()) + " / " + timer,
                    event.health() / 100,
                    event.status() == FieldEvent.Status.ACTIVE
                            && !event.underAttack()
                            && distance < FieldEvent.FOLLOW_RANGE);
        }
        boolean supplies = event.kind() == FieldEvent.Kind.SUPPLIES;
        String instruction =
                supplies
                        ? "[" + key + "] HOLD NEAR CACHE"
                        : event.status() == FieldEvent.Status.WAITING
                                ? "[" + key + "] AWAKEN WAYSTONE"
                                : "DEFEND THE WAYSTONE";
        return new ObjectiveStatus(
                event.title(),
                instruction,
                timer,
                supplies ? event.progress() / 2 : event.progress() / 14,
                false);
    }
}
