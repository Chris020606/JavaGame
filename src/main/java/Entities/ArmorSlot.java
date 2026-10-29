package Entities;

public enum ArmorSlot {
    HEAD("Helmet",1),CHEST("Chestplate",3),LEGS("Greaves",2),BOOTS("Boots",1);
    public final String label;
    public final int protection;
    ArmorSlot(String label,int protection) { this.label=label;this.protection=protection; }
    public String icon() { return name().toLowerCase(); }
}
