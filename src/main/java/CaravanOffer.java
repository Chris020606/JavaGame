/** Merchant presentation derives prices, previews and availability from actual progression. */
public record CaravanOffer(String name, String preview, String action, String explanation) {
    public static CaravanOffer forItem(Progression p, int item) {
        String[] names = {"HEARTWOOD CHARM", "TRAVELER'S FLASK", "RESTORATION INCENSE"};
        int value =
                switch (item) {
                    case 0 -> p.maxHealth();
                    case 1 -> p.maxStamina();
                    case 2 -> p.staminaRecovery();
                    default -> throw new IllegalArgumentException("Invalid caravan item");
                };
        boolean complete = p.caravanRank(item) >= 5;
        int increase = complete ? 0 : item == 2 ? 1 : 5;
        String stat = item == 0 ? "MAX HP" : item == 1 ? p.resourceLabel() : "RECOVERY / SEC";
        int missing = Math.max(0, p.caravanPrice(item) - p.crystals());
        String action =
                complete
                        ? "ALL RANKS OWNED"
                        : missing > 0
                                ? "NEED " + missing + " MORE CR"
                                : "BUY / " + p.caravanPrice(item) + " CR";
        String explanation =
                complete
                        ? "Fully upgraded. This permanent bonus is already active."
                        : "Permanent upgrade / "
                                + value
                                + " to "
                                + (value + increase)
                                + " "
                                + stat.toLowerCase(java.util.Locale.ROOT)
                                + ". No equipment slot required.";
        return new CaravanOffer(
                names[item], stat + "  " + value + " > " + (value + increase), action, explanation);
    }
}
