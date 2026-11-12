import static org.junit.jupiter.api.Assertions.*;

import assets.AssetPaths;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

class CastleAssetsTest {

    @Test
    void kayKitMedievalHexagonAssetsExistOnDisk() {
        String base = "resources/KayKit_Medieval_Hexagon_Pack_1.0_FREE/Assets/gltf/";
        String[] assets = {
            // Terrain & Roads
            "tiles/base/hex_grass.gltf",
            "tiles/base/hex_water.gltf",
            "tiles/roads/hex_road_A.gltf",
            "tiles/roads/hex_road_B.gltf",
            "tiles/roads/hex_road_D.gltf",
            "tiles/roads/hex_road_L.gltf",

            // Castle & Fortifications
            "buildings/blue/building_castle_blue.gltf",
            "buildings/blue/building_tower_A_blue.gltf",
            "buildings/blue/building_tower_B_blue.gltf",
            "buildings/blue/building_tower_base_blue.gltf",
            "buildings/blue/building_tower_catapult_blue.gltf",
            "buildings/neutral/wall_straight.gltf",
            "buildings/neutral/wall_straight_gate.gltf",
            "buildings/neutral/fence_wood_straight.gltf",
            "buildings/neutral/fence_stone_straight.gltf",

            // District Architecture
            "buildings/blue/building_church_blue.gltf",
            "buildings/blue/building_blacksmith_blue.gltf",
            "buildings/blue/building_barracks_blue.gltf",
            "buildings/blue/building_archeryrange_blue.gltf",
            "buildings/blue/building_tavern_blue.gltf",
            "buildings/red/building_tavern_red.gltf",
            "buildings/red/building_market_red.gltf",
            "buildings/yellow/building_market_yellow.gltf",
            "buildings/blue/building_market_blue.gltf",
            "buildings/blue/building_well_blue.gltf",
            "buildings/green/building_windmill_green.gltf",
            "buildings/green/building_lumbermill_green.gltf",
            "buildings/neutral/building_grain.gltf",

            // Residential
            "buildings/green/building_home_A_green.gltf",
            "buildings/green/building_home_B_green.gltf",
            "buildings/red/building_home_A_red.gltf",
            "buildings/red/building_home_B_red.gltf",
            "buildings/yellow/building_home_A_yellow.gltf",
            "buildings/yellow/building_home_B_yellow.gltf",
            "buildings/blue/building_home_A_blue.gltf",
            "buildings/blue/building_home_B_blue.gltf",

            // Props & Decoration
            "decoration/props/barrel.gltf",
            "decoration/props/crate_A_big.gltf",
            "decoration/props/crate_B_big.gltf",
            "decoration/props/crate_open.gltf",
            "decoration/props/weaponrack.gltf",
            "decoration/props/target.gltf",
            "decoration/props/tent.gltf",
            "decoration/props/flag_blue.gltf",
            "decoration/props/flag_red.gltf",
            "decoration/props/wheelbarrow.gltf",
            "decoration/props/pallet.gltf",
            "decoration/props/sack.gltf",
            "decoration/props/ladder.gltf",
            "decoration/props/bucket_arrows.gltf",

            // Nature
            "decoration/nature/tree_single_A.gltf",
            "decoration/nature/tree_single_B.gltf",
            "decoration/nature/trees_A_large.gltf",
            "decoration/nature/rock_single_A.gltf",
            "decoration/nature/rock_single_B.gltf",
            "decoration/nature/hills_A.gltf"
        };

        for (String asset : assets) {
            Path p = Path.of(AssetPaths.path(base + asset));
            assertTrue(Files.exists(p), "Missing KayKit hexagon asset: " + asset);
            assertTrue(Files.isReadable(p), "Cannot read KayKit hexagon asset: " + asset);
        }
    }

    @Test
    void fortressModelsExistOnDisk() {
        String[] models = {
            "iron-fence-border-gate.obj",
            "iron-fence-border-gate.mtl",
            "stone-wall.obj",
            "stone-wall.mtl",
            "crypt-door.obj",
            "crypt-door.mtl",
            "crypt-large.obj",
            "crypt-large.mtl",
            "crypt-large-roof.obj",
            "crypt-large-roof.mtl",
            "pillar-obelisk.obj",
            "pillar-obelisk.mtl"
        };
        for (String m : models) {
            Path p = Path.of(AssetPaths.path("resources/outpost/graveyard/" + m));
            assertTrue(Files.exists(p), "Missing fortress asset: " + m);
            assertTrue(Files.isReadable(p), "Cannot read fortress asset: " + m);
        }
        Path lampTex = Path.of(AssetPaths.path("resources/outpost/citadel/lamp_iron_dark.png"));
        assertTrue(Files.exists(lampTex), "Missing dark iron lamp texture: lamp_iron_dark.png");
        assertTrue(
                Files.isReadable(lampTex),
                "Cannot read dark iron lamp texture: lamp_iron_dark.png");
    }

    @Test
    void npcCharacterModelsExistOnDisk() {
        String[] characters = {
            "Knight.glb", "Mage.glb", "Ranger.glb", "Barbarian.glb", "Rogue_Hooded.glb", "Rogue.glb"
        };
        for (String c : characters) {
            Path p =
                    Path.of(
                            AssetPaths.path(
                                    "resources/KayKit_Adventurers_2.0_FREE/Characters/gltf/" + c));
            assertTrue(Files.exists(p), "Missing character asset: " + c);
            assertTrue(Files.isReadable(p), "Cannot read character asset: " + c);
        }
    }

    @Test
    void fantasyWeaponsBitsExistOnDisk() {
        String[] weapons = {
            "sword_A.gltf",
            "sword_B.gltf",
            "sword_E.gltf",
            "axe_B.gltf",
            "bow_A.gltf",
            "bow_A_withString.gltf",
            "bow_B.gltf",
            "bow_B_withString.gltf",
            "wand_A.gltf",
            "staff_A.gltf",
            "staff_B.gltf",
            "spear_A.gltf",
            "weapons_bits_texture.png"
        };
        for (String w : weapons) {
            Path p = Path.of(AssetPaths.path("resources/outpost/weapons/" + w));
            assertTrue(Files.exists(p), "Missing weapon asset: " + w);
            assertTrue(Files.isReadable(p), "Cannot read weapon asset: " + w);
        }
    }
}
