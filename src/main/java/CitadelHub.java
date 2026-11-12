import static com.raylib.Colors.*;
import static com.raylib.Raylib.*;

import assets.AssetPaths;

import java.util.*;

/**
 * The Sanctuary Citadel Hub: A complete, living fantasy medieval city built from the KayKit
 * Medieval Hexagon Pack. Features a grand central boulevard, dominant rear castle, fortress
 * perimeter walls, market square, craftsman forge, arcanist sanctum, guard barracks, cathedral
 * district, rural windmill outskirts, and expedition teleport gateway.
 */
public final class CitadelHub implements AutoCloseable {
    public static final float PORTAL_X = 0f;
    public static final float PORTAL_Z = 29.5f;
    public static final float PORTAL_RADIUS = 2.2f;

    // Hexagon grid parameters (flat-topped orientation, scale 4.08f for seamless overlap)
    public static final float GRID_SCALE = 4.08f;
    public static final float COL_SPACING = (float) (Math.sqrt(3.0) * 4.0f); // ~6.9282f
    public static final float ROW_SPACING = 8.0f;
    public static final float ROW_STAGGER = 4.0f; // 4.0f for odd columns

    // Layout records
    public record HexPlacement(Model model, float x, float z, float yaw, float scale) {}

    public record BuildingPlacement(Model model, float x, float z, float yaw, float scale) {}

    public record PropPlacement(Model model, float x, float z, float yaw, float scale) {}

    public record StreetLight(float x, float z, float flickerOffset) {}

    public record BrazierLight(float x, float z, float flickerOffset) {}

    public static final class Obstacle {
        public final float minX, minZ, maxX, maxZ;
        public final float cx, cz, radius;
        public final boolean isBox;

        public static Obstacle circle(float cx, float cz, float radius) {
            return new Obstacle(0, 0, 0, 0, cx, cz, radius, false);
        }

        public static Obstacle box(float minX, float minZ, float maxX, float maxZ) {
            return new Obstacle(
                    minX, minZ, maxX, maxZ, (minX + maxX) / 2, (minZ + maxZ) / 2, 0, true);
        }

        private Obstacle(
                float minX,
                float minZ,
                float maxX,
                float maxZ,
                float cx,
                float cz,
                float radius,
                boolean isBox) {
            this.minX = minX;
            this.minZ = minZ;
            this.maxX = maxX;
            this.maxZ = maxZ;
            this.cx = cx;
            this.cz = cz;
            this.radius = radius;
            this.isBox = isBox;
        }
    }

    // Track all loaded models for lighting preparation and safe unloading
    private final Map<Model, Float> propGroundOffsets = new IdentityHashMap<>();
    private final List<Model> allLoadedModels = new ArrayList<>();
    private Texture lampPostTexture = null;

    // Terrain and road models
    private Model hexGrassModel;
    private Model hexWaterModel;
    private Model hexRoadStraightModel;
    private Model hexRoadTurnBModel;
    private Model hexRoadTurnCModel;
    private Model hexRoadCrossHModel;
    private Model hexRoadEndMModel;

    // Architecture models
    private Model castleModel;
    private Model churchModel;
    private Model blacksmithModel;
    private Model barracksModel;
    private Model archeryRangeModel;
    private Model tavernRedModel;
    private Model publicWellModel;
    private Model flowerYellowModel, flowerPurpleModel, gardenBushModel;
    private Model marketRedModel;
    private Model marketYellowModel;
    private Model marketBlueModel;
    private Model windmillModel;

    // Completely unique residential & workshop building models (zero duplicates)
    private Model homeAGreenModel;
    private Model homeBYellowModel;
    private Model watermillGreenModel;
    private Model mineGreenModel;
    private Model lumbermillGreenModel;

    // Fortress walls, gates, and towers
    private Model wallStraightModel;
    private Model wallGateModel;
    private Model towerAModel;
    private Model towerBModel;
    private Model towerBaseModel;
    private Model towerCatapultModel;
    private Model fenceWoodModel;
    private Model fenceStoneModel;

    // Props and decorations
    private Model barrelModel;
    private Model crateABigModel;
    private Model crateBBigModel;
    private Model crateOpenModel;
    private Model weaponRackModel;
    private Model targetModel;
    private Model tentModel;
    private Model flagBlueModel;
    private Model flagRedModel;
    private Model wheelbarrowModel;
    private Model palletModel;
    private Model sackModel;
    private Model ladderModel;
    private Model bucketArrowsModel;
    private Model lampPostModel;

    // Interactive sanctuary features
    private Model portalRingModel;
    private Model altarModel;
    private Model campfireModel;

    // Nature and environment
    private Model treeSingleAModel;
    private Model treeSingleBModel;
    private Model treesLargeModel;
    private Model treesMediumModel;
    private Model rockSingleAModel;
    private Model rockSingleBModel;
    private Model hillsModel;
    private Model mountainAModel;
    private Model mountainBModel;
    private Model mountainCModel;
    private Model mountainGrassModel;
    private Model pineTallModel;
    private Model pineRoundModel;
    private Model distantMountainsModel;

    // NPC character models
    private Model commanderModel;
    private final Model[] citizenModels = new Model[6];
    private final ModelAnimation[] citizenAnimations = new ModelAnimation[6];
    private final int[] citizenAnimationCounts = new int[6];

    private Model knightModel;
    private Model mageModel;
    private Model rangerModel;
    private Model barbarianModel;
    private Model rogueHoodedModel;
    private Model rogueModel;

    // Native NPC animations (100% matched to respective model skeletons)
    private ModelAnimation commanderAnims;
    private int commanderAnimCount = 0;
    private ModelAnimation knightAnims;
    private int knightAnimCount = 0;
    private ModelAnimation mageAnims;
    private int mageAnimCount = 0;
    private ModelAnimation rangerAnims;
    private int rangerAnimCount = 0;
    private ModelAnimation barbarianAnims;
    private int barbarianAnimCount = 0;
    private ModelAnimation archerAnims;
    private int archerAnimCount = 0;
    private ModelAnimation shamanAnims;
    private int shamanAnimCount = 0;

    // Precomputed city geometry layers
    private final List<HexPlacement> terrainTiles = new ArrayList<>();
    private final List<BuildingPlacement> buildings = new ArrayList<>();
    private final List<PropPlacement> props = new ArrayList<>();
    private final List<StreetLight> streetLights = new ArrayList<>();
    private final List<BrazierLight> braziers = new ArrayList<>();
    private final List<Obstacle> collisionObstacles = new ArrayList<>();

    private CitadelNpc activeNpc = null;
    private String npcDialogue = "";
    private float teleportTimer = 0f;
    private boolean teleporting = false;
    private boolean returnPortalOpen = false;

    public CitadelHub() {
        loadModels();
        buildCityLayout();
    }

    private Model tryLoad(String relativePath) {
        if (!IsWindowReady()) return new Model();
        try {
            String path = AssetPaths.path(relativePath);
            if (path != null && java.nio.file.Files.exists(java.nio.file.Path.of(path))) {
                Model model = LoadModel(path);
                if (model != null && model.meshCount() > 0) {
                    allLoadedModels.add(model);
                    propGroundOffsets.put(model, -GetModelBoundingBox(model).min().y());
                    return model;
                }
            }
        } catch (Exception ignored) {
        }
        return new Model();
    }

    private void loadModels() {
        String kayGltf = "resources/KayKit_Medieval_Hexagon_Pack_1.0_FREE/Assets/gltf/";

        // 1. Terrain & Connected Roads
        hexGrassModel = tryLoad(kayGltf + "tiles/base/hex_grass.gltf");
        hexWaterModel = tryLoad(kayGltf + "tiles/base/hex_water.gltf");
        hexRoadStraightModel = tryLoad(kayGltf + "tiles/roads/hex_road_A.gltf");
        hexRoadTurnBModel = tryLoad(kayGltf + "tiles/roads/hex_road_B.gltf");
        hexRoadTurnCModel = tryLoad(kayGltf + "tiles/roads/hex_road_C.gltf");
        hexRoadCrossHModel = tryLoad(kayGltf + "tiles/roads/hex_road_H.gltf");
        hexRoadEndMModel = tryLoad(kayGltf + "tiles/roads/hex_road_M.gltf");

        // 2. Castle & Fortifications
        castleModel = tryLoad(kayGltf + "buildings/blue/building_castle_blue.gltf");
        towerAModel = tryLoad(kayGltf + "buildings/blue/building_tower_A_blue.gltf");
        towerBModel = tryLoad(kayGltf + "buildings/blue/building_tower_B_blue.gltf");
        towerBaseModel = tryLoad(kayGltf + "buildings/blue/building_tower_base_blue.gltf");
        towerCatapultModel = tryLoad(kayGltf + "buildings/blue/building_tower_catapult_blue.gltf");
        wallStraightModel = tryLoad(kayGltf + "buildings/neutral/wall_straight.gltf");
        wallGateModel = tryLoad(kayGltf + "buildings/neutral/wall_straight_gate.gltf");
        fenceWoodModel = tryLoad(kayGltf + "buildings/neutral/fence_wood_straight.gltf");
        fenceStoneModel = tryLoad(kayGltf + "buildings/neutral/fence_stone_straight.gltf");

        // 3. Civic & District Architecture
        churchModel = tryLoad(kayGltf + "buildings/blue/building_church_blue.gltf");
        blacksmithModel = tryLoad(kayGltf + "buildings/blue/building_blacksmith_blue.gltf");
        barracksModel = tryLoad(kayGltf + "buildings/blue/building_barracks_blue.gltf");
        archeryRangeModel = tryLoad(kayGltf + "buildings/blue/building_archeryrange_blue.gltf");
        tavernRedModel = tryLoad(kayGltf + "buildings/red/building_tavern_red.gltf");
        publicWellModel = tryLoad(kayGltf + "buildings/yellow/building_well_yellow.gltf");
        flowerYellowModel = tryLoad("resources/outpost/nature/flower_yellowC.obj");
        flowerPurpleModel = tryLoad("resources/outpost/nature/flower_purpleC.obj");
        gardenBushModel = tryLoad("resources/outpost/nature/plant_bushDetailed.obj");
        marketRedModel = tryLoad(kayGltf + "buildings/red/building_market_red.gltf");
        marketYellowModel = tryLoad(kayGltf + "buildings/yellow/building_market_yellow.gltf");
        marketBlueModel = tryLoad(kayGltf + "buildings/blue/building_market_blue.gltf");
        windmillModel = tryLoad(kayGltf + "buildings/green/building_windmill_green.gltf");

        // 4. Completely Unique Residential & Workshop Architecture
        homeAGreenModel = tryLoad(kayGltf + "buildings/green/building_home_A_green.gltf");
        homeBYellowModel = tryLoad(kayGltf + "buildings/yellow/building_home_B_yellow.gltf");
        watermillGreenModel = tryLoad(kayGltf + "buildings/green/building_watermill_green.gltf");
        mineGreenModel = tryLoad(kayGltf + "buildings/green/building_mine_green.gltf");
        lumbermillGreenModel = tryLoad(kayGltf + "buildings/green/building_lumbermill_green.gltf");

        // 5. Props & Crafting Assets
        barrelModel = tryLoad(kayGltf + "decoration/props/barrel.gltf");
        crateABigModel = tryLoad(kayGltf + "decoration/props/crate_A_big.gltf");
        crateBBigModel = tryLoad(kayGltf + "decoration/props/crate_B_big.gltf");
        crateOpenModel = tryLoad(kayGltf + "decoration/props/crate_open.gltf");
        weaponRackModel = tryLoad(kayGltf + "decoration/props/weaponrack.gltf");
        targetModel = tryLoad(kayGltf + "decoration/props/target.gltf");
        tentModel = tryLoad(kayGltf + "decoration/props/tent.gltf");
        flagBlueModel = tryLoad(kayGltf + "decoration/props/flag_blue.gltf");
        flagRedModel = tryLoad(kayGltf + "decoration/props/flag_red.gltf");
        wheelbarrowModel = tryLoad(kayGltf + "decoration/props/wheelbarrow.gltf");
        palletModel = tryLoad(kayGltf + "decoration/props/pallet.gltf");
        sackModel = tryLoad(kayGltf + "decoration/props/sack.gltf");
        ladderModel = tryLoad(kayGltf + "decoration/props/ladder.gltf");
        bucketArrowsModel = tryLoad(kayGltf + "decoration/props/bucket_arrows.gltf");
        lampPostModel = tryLoad("resources/outpost/graveyard/lightpost-single.obj");
        if (IsWindowReady()) {
            try {
                String texPath = AssetPaths.path("resources/outpost/citadel/lamp_iron_dark.png");
                if (texPath != null && java.nio.file.Files.exists(java.nio.file.Path.of(texPath))) {
                    lampPostTexture = LoadTexture(texPath);
                    if (lampPostModel != null && lampPostModel.materials() != null) {
                        for (int i = 0; i < lampPostModel.materialCount(); i++) {
                            Material mat = new Material(lampPostModel.materials()).position(i);
                            MaterialMap map =
                                    new MaterialMap(mat.maps()).position(MATERIAL_MAP_ALBEDO);
                            map.texture(lampPostTexture);
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }

        // 6. Interactive Sanctuary Centerpieces
        portalRingModel = tryLoad("resources/outpost/nature/statue_ring.obj");
        altarModel = tryLoad("resources/outpost/graveyard/altar-stone.obj");
        campfireModel = tryLoad("resources/outpost/nature/campfire_stones.obj");

        // 7. Nature & Scenery
        treeSingleAModel = tryLoad(kayGltf + "decoration/nature/tree_single_A.gltf");
        treeSingleBModel = tryLoad(kayGltf + "decoration/nature/tree_single_B.gltf");
        treesLargeModel = tryLoad(kayGltf + "decoration/nature/trees_A_large.gltf");
        treesMediumModel = tryLoad(kayGltf + "decoration/nature/trees_B_large.gltf");
        rockSingleAModel = tryLoad(kayGltf + "decoration/nature/rock_single_A.gltf");
        rockSingleBModel = tryLoad(kayGltf + "decoration/nature/rock_single_B.gltf");
        hillsModel = tryLoad(kayGltf + "decoration/nature/hills_A.gltf");
        mountainAModel = tryLoad(kayGltf + "decoration/nature/mountain_A_grass_trees.gltf");
        mountainBModel = tryLoad(kayGltf + "decoration/nature/mountain_B_grass_trees.gltf");
        mountainCModel = tryLoad(kayGltf + "decoration/nature/mountain_C_grass_trees.gltf");
        mountainGrassModel = tryLoad(kayGltf + "decoration/nature/mountain_B_grass.gltf");
        pineTallModel = tryLoad("resources/outpost/nature/tree_pineTallA_detailed.obj");
        pineRoundModel = tryLoad("resources/outpost/nature/tree_pineRoundC.obj");
        distantMountainsModel = tryLoad("resources/outpost/landscape/mountains.obj");

        // 8. Characters (Native adventurer models with matching skeletons)
        commanderModel = tryLoad("resources/outpost/raider.glb");
        knightModel = tryLoad("resources/outpost/hero_warrior.glb");
        mageModel = tryLoad("resources/outpost/hero_mage.glb");
        rangerModel = tryLoad("resources/outpost/hero_archer.glb");
        barbarianModel = tryLoad("resources/outpost/raider.glb");
        rogueHoodedModel = tryLoad("resources/outpost/archer.glb");
        rogueModel = tryLoad("resources/outpost/shaman.glb");

        if (IsWindowReady()) {
            for (int i = 0; i < CitadelNpc.RESCUED_SURVIVORS.length; i++) {
                String name =
                        CitadelNpc.RESCUED_SURVIVORS[i]
                                .name()
                                .replace("SURVIVOR_", "")
                                .toLowerCase(java.util.Locale.ROOT);
                String path = "resources/outpost/citizens/" + name + ".glb";
                citizenModels[i] = tryLoad(path);
                int[] count = {0};
                citizenAnimations[i] = loadAnims(path, count);
                citizenAnimationCounts[i] = count[0];
                for (int clip = 0; clip < count[0]; clip++) {
                    if (!IsModelAnimationValid(
                            citizenModels[i], getAnimView(citizenAnimations[i], clip, count[0])))
                        throw new IllegalStateException("Citizen animation mismatch: " + name);
                }
            }
        }

        // 9. Native Character Animations (100% matched to respective character meshes)
        if (IsWindowReady()) {
            try {
                int[] ptr = {0};
                commanderAnims = loadAnims("resources/outpost/raider.glb", ptr);
                commanderAnimCount = ptr[0];

                ptr[0] = 0;
                knightAnims = loadAnims("resources/outpost/hero_warrior.glb", ptr);
                knightAnimCount = ptr[0];

                ptr[0] = 0;
                mageAnims = loadAnims("resources/outpost/hero_mage.glb", ptr);
                mageAnimCount = ptr[0];

                ptr[0] = 0;
                rangerAnims = loadAnims("resources/outpost/hero_archer.glb", ptr);
                rangerAnimCount = ptr[0];

                ptr[0] = 0;
                barbarianAnims = loadAnims("resources/outpost/raider.glb", ptr);
                barbarianAnimCount = ptr[0];

                ptr[0] = 0;
                archerAnims = loadAnims("resources/outpost/archer.glb", ptr);
                archerAnimCount = ptr[0];

                ptr[0] = 0;
                shamanAnims = loadAnims("resources/outpost/shaman.glb", ptr);
                shamanAnimCount = ptr[0];
            } catch (Exception ignored) {
            }
        }
    }

    private ModelAnimation loadAnims(String relativePath, int[] ptr) {
        try {
            String path = AssetPaths.path(relativePath);
            if (path != null && java.nio.file.Files.exists(java.nio.file.Path.of(path))) {
                return LoadModelAnimations(path, ptr);
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    /**
     * Build the entire medieval city layout using modular layers. Enforces exact hex grid symmetry
     * angles (30, 90, 150, 210, 270, 330) for zero gaps.
     */
    private void buildCityLayout() {
        terrainTiles.clear();
        buildings.clear();
        props.clear();
        streetLights.clear();
        braziers.clear();

        buildTerrainAndRoads();
        buildCastleDistrict();
        buildPerimeterWalls();
        buildDistrictArchitecture();
        buildPropsAndAtmosphere();
        // Large canopies must never sit on a road tile's walking corridor.
        if (IsWindowReady())
            props.removeIf(prop ->
                    (prop.model == treeSingleAModel || prop.model == treeSingleBModel)
                            && terrainTiles.stream()
                                    .anyMatch(tile ->
                                            tile.model != hexGrassModel
                                                    && tile.model != hexWaterModel
                                                    && Math.hypot(prop.x - tile.x, prop.z - tile.z)
                                                            < 4.8f));
        buildLighting();
        buildCollisionObstacles();
    }

    private record RoadTile(Model model, float yaw) {}

    private void buildTerrainAndRoads() {
        // Mathematically verified road network with 100% matched exits
        Map<String, RoadTile> roads = new HashMap<>();

        // 1. Central Grand Boulevard (Column 0: South Gate to Castle Forecourt)
        // Road terminates cleanly at castle forecourt (0, -3), leaving space under keep clear
        roads.put("0,-3", new RoadTile(hexRoadEndMModel, 90f));
        roads.put("0,-2", new RoadTile(hexRoadCrossHModel, 90f));
        roads.put("0,-1", new RoadTile(hexRoadStraightModel, 90f));
        roads.put("0,0", new RoadTile(hexRoadCrossHModel, 90f));
        roads.put("0,1", new RoadTile(hexRoadStraightModel, 90f));
        roads.put("0,2", new RoadTile(hexRoadStraightModel, 90f));
        roads.put("0,3", new RoadTile(hexRoadCrossHModel, 90f));
        roads.put("0,4", new RoadTile(hexRoadStraightModel, 90f));
        roads.put("0,5", new RoadTile(hexRoadStraightModel, 90f));
        roads.put("0,6", new RoadTile(hexRoadStraightModel, 90f));
        roads.put("0,7", new RoadTile(hexRoadEndMModel, 270f));

        // 2. East Residential District Loop & Street Block
        roads.put("1,0", new RoadTile(hexRoadStraightModel, 150f));
        roads.put("2,1", new RoadTile(hexRoadTurnBModel, 90f));
        roads.put("2,2", new RoadTile(hexRoadStraightModel, 90f));
        roads.put("2,3", new RoadTile(hexRoadTurnBModel, 30f));
        roads.put("1,3", new RoadTile(hexRoadTurnBModel, 330f));

        // 3. West Craft & Barracks District Loop
        roads.put("-1,0", new RoadTile(hexRoadTurnCModel, 270f));
        roads.put("-1,-1", new RoadTile(hexRoadTurnBModel, 90f));
        roads.put("-2,-1", new RoadTile(hexRoadTurnCModel, 210f));
        roads.put("-1,-2", new RoadTile(hexRoadStraightModel, 30f));

        // 4. Cathedral District East
        roads.put("1,-2", new RoadTile(hexRoadTurnBModel, 330f));
        roads.put("2,-2", new RoadTile(hexRoadEndMModel, 30f));

        // 5. Farmlands & Windmill West
        roads.put("-1,3", new RoadTile(hexRoadStraightModel, 30f));
        roads.put("-2,4", new RoadTile(hexRoadEndMModel, 210f));

        // Flat-topped hex grid covering X in [-56, 56] and Z in [-60, 50]
        for (int col = -8; col <= 8; col++) {
            float x = col * COL_SPACING;
            float stagger = (Math.abs(col) % 2 == 1) ? ROW_STAGGER : 0f;

            for (int row = -10; row <= 8; row++) {
                float z = row * ROW_SPACING + stagger;
                String key = col + "," + row;

                if (roads.containsKey(key)) {
                    RoadTile rt = roads.get(key);
                    terrainTiles.add(new HexPlacement(rt.model, x, z, rt.yaw, GRID_SCALE));
                } else if (Math.abs(col) >= 4 && Math.abs(col) <= 6 && row <= -6 && row >= -9) {
                    // Castle moat / outer scenic water tiles behind castle keep
                    terrainTiles.add(new HexPlacement(hexWaterModel, x, z, 90f, GRID_SCALE));
                } else {
                    // Connected lush grass terrain with consistent grid orientation
                    terrainTiles.add(new HexPlacement(hexGrassModel, x, z, 90f, GRID_SCALE));
                }
            }
        }
    }

    private void buildCastleDistrict() {
        // Monumental Castle Keep dominant at rear (scale 12.0f, height ~48m, facing South down
        // boulevard, zero road overlap)
        buildings.add(new BuildingPlacement(castleModel, 0f, -40.0f, 0f, 12.0f));

        // High Flanking Bastion Towers (scale 9.0f, height ~20m)
        buildings.add(new BuildingPlacement(towerAModel, -16.0f, -38f, 0f, 9.0f));
        buildings.add(new BuildingPlacement(towerAModel, 16.0f, -38f, 0f, 9.0f));

        // Castle Curtain Walls connecting towers to keep
        buildings.add(new BuildingPlacement(wallStraightModel, -13.8f, -38f, 0f, 5.5f));
        buildings.add(new BuildingPlacement(wallStraightModel, 13.8f, -38f, 0f, 5.5f));

        // Catapult Defense Bastions (scale 7.5f)
        buildings.add(new BuildingPlacement(towerCatapultModel, -26f, -38f, 0f, 7.5f));
        buildings.add(new BuildingPlacement(towerCatapultModel, 26f, -38f, 0f, 7.5f));

        // Outer fortress walls connecting to catapult bastions
        buildings.add(new BuildingPlacement(wallStraightModel, -21.0f, -38f, 0f, 5.2f));
        buildings.add(new BuildingPlacement(wallStraightModel, 21.0f, -38f, 0f, 5.2f));

        // Castle Court Gateway Towers framing the boulevard entrance
        buildings.add(new BuildingPlacement(towerBModel, -7.5f, -22f, 0f, 6.0f));
        buildings.add(new BuildingPlacement(towerBModel, 7.5f, -22f, 0f, 6.0f));

        // Heraldic Banners flanking castle terrace
        props.add(new PropPlacement(flagBlueModel, -3.2f, -25f, 0f, 4.8f));
        props.add(new PropPlacement(flagBlueModel, 3.2f, -25f, 0f, 4.8f));
        props.add(new PropPlacement(flagBlueModel, -11f, -38f, 0f, 4.5f));
        props.add(new PropPlacement(flagBlueModel, 11f, -38f, 0f, 4.5f));
    }

    private void buildPerimeterWalls() {
        float wallScale = 5.0f;

        // 1. South Ramparts with SINGLE INTENTIONAL MAIN GATE at (0, 0, 34)
        buildings.add(new BuildingPlacement(wallGateModel, 0f, 34f, 0f, 5.2f));
        buildings.add(new BuildingPlacement(towerAModel, -5.5f, 34f, 0f, 5.8f));
        buildings.add(new BuildingPlacement(towerAModel, 5.5f, 34f, 0f, 5.8f));

        // South curtain walls seamlessly spanning to the corners and anchoring into mountain ridges
        float[] southX = {12.0f, 20.0f, 28.0f, 34.0f, 42.0f, 48.0f};
        for (float x : southX) {
            buildings.add(new BuildingPlacement(wallStraightModel, x, 34f, 0f, wallScale));
            buildings.add(new BuildingPlacement(wallStraightModel, -x, 34f, 0f, wallScale));
        }

        // 2. Corner Bastion Towers and Mountain Anchor Towers (Zero Gaps)
        buildings.add(new BuildingPlacement(towerBaseModel, -36f, 34f, 0f, 6.0f));
        buildings.add(new BuildingPlacement(towerBaseModel, 36f, 34f, 0f, 6.0f));
        buildings.add(new BuildingPlacement(towerAModel, -52f, 34f, 0f, 6.2f));
        buildings.add(new BuildingPlacement(towerAModel, 52f, 34f, 0f, 6.2f));

        buildings.add(new BuildingPlacement(towerBaseModel, -36f, -46f, 0f, 6.0f));
        buildings.add(new BuildingPlacement(towerBaseModel, 36f, -46f, 0f, 6.0f));
        buildings.add(new BuildingPlacement(towerAModel, -52f, -46f, 0f, 6.2f));
        buildings.add(new BuildingPlacement(towerAModel, 52f, -46f, 0f, 6.2f));

        // 3. East Perimeter Wall (X = +36, yaw = 90 deg, spanning from +34 to -46)
        float[] sideZ = {30f, 22f, 14f, 6f, -2f, -10f, -18f, -26f, -34f, -42f};
        for (float z : sideZ) {
            buildings.add(new BuildingPlacement(wallStraightModel, 36f, z, 90f, wallScale));
        }
        // East Watchtowers
        buildings.add(new BuildingPlacement(towerBModel, 36f, -2f, 90f, 5.8f));
        buildings.add(new BuildingPlacement(towerBModel, 36f, -26f, 90f, 5.8f));

        // 4. West Perimeter Wall (X = -36, yaw = 90 deg, spanning from +34 to -46)
        for (float z : sideZ) {
            buildings.add(new BuildingPlacement(wallStraightModel, -36f, z, 90f, wallScale));
        }
        // West Watchtowers
        buildings.add(new BuildingPlacement(towerBModel, -36f, -2f, -90f, 5.8f));
        buildings.add(new BuildingPlacement(towerBModel, -36f, -26f, -90f, 5.8f));

        // 5. North Rear Fortress Wall (Z = -46, yaw = 0 deg, spanning from -52 to +52 into the
        // mountains)
        float[] northX = {-48f, -42f, -32f, -24f, -16f, -8f, 0f, 8f, 16f, 24f, 32f, 42f, 48f};
        for (float x : northX) {
            buildings.add(new BuildingPlacement(wallStraightModel, x, -46f, 0f, wallScale));
        }
    }

    private void buildDistrictArchitecture() {
        // 1. Blacksmith & Craft District (West: framing Goran at -7.5, 1.5)
        buildings.add(new BuildingPlacement(blacksmithModel, -15.5f, 1.5f, 90f, 7.8f));

        // 2. Arcanist & Alchemical District (East: framing Lady Elyndra at 7.5, 1.5)
        // Authentic stone scholar sanctum (churchBlueModel) with stained glass and NO barrel!
        buildings.add(new BuildingPlacement(churchModel, 15.5f, 1.5f, -90f, 7.8f));
        props.add(new PropPlacement(tentModel, 18.5f, 4.5f, 45f, 4.5f));

        // 3. Guard Barracks & Training Grounds (Northwest: z in [-20, -12])
        buildings.add(new BuildingPlacement(barracksModel, -16.0f, -15.0f, 180f, 7.8f));
        buildings.add(new BuildingPlacement(archeryRangeModel, -25.0f, -15.0f, 90f, 7.5f));
        props.add(new PropPlacement(tentModel, -16.0f, -23.0f, 30f, 4.5f));

        // 4. Cathedral & Monastery District (Northeast: z in [-20, -12])
        // Rotated with yaw = -90f so its facade and grand entrance face West towards the courtyard
        buildings.add(new BuildingPlacement(churchModel, 21.5f, -15.0f, -90f, 8.2f));
        props.add(new PropPlacement(treeSingleBModel, 22.0f, -22.0f, 0f, 4.5f));

        // 5. Grand Market Square (Center-South: z in [10, 22])
        buildings.add(new BuildingPlacement(marketRedModel, -8.5f, 12.5f, 90f, 6.2f));
        buildings.add(new BuildingPlacement(marketYellowModel, 8.5f, 12.5f, -90f, 6.2f));
        buildings.add(new BuildingPlacement(marketBlueModel, -8.5f, 19.5f, 90f, 6.2f));
        // Market Tavern: THE ONLY BUILDING IN TOWN WITH A BARREL ON TOP (West side of Market)
        buildings.add(new BuildingPlacement(tavernRedModel, -16.5f, 16.5f, 90f, 7.8f));

        // 6. Residential City Block (East: unique distinct homes facing the residential street)
        // Street runs along X ~ 13.86 (Z: 8 to 24). Homes face West directly onto the street!
        // Yellow home shifted eastward to X = 25.5 so no wooden beams overlap the road
        buildings.add(new BuildingPlacement(homeAGreenModel, 22.5f, 7.5f, -90f, 7.5f));
        buildings.add(new BuildingPlacement(homeBYellowModel, 25.5f, 13.0f, -90f, 7.2f));
        buildings.add(new BuildingPlacement(watermillGreenModel, 23.5f, 19.0f, -90f, 7.2f));
        buildings.add(new BuildingPlacement(mineGreenModel, 23.0f, 25.0f, -90f, 6.8f));
        buildings.add(new BuildingPlacement(lumbermillGreenModel, 22.5f, 30.5f, -90f, 7.0f));

        // 7. Rural Outskirts & Windmill (Far West)
        buildings.add(new BuildingPlacement(windmillModel, -27.0f, 10.0f, 0f, 9.5f));
        props.add(new PropPlacement(wheelbarrowModel, -23.0f, 6.0f, 25f, 1.8f));
    }

    private void buildPropsAndAtmosphere() {
        // Public market landmark sits beside the avenue, clear of the merchant and home routes.
        props.add(new PropPlacement(publicWellModel, 8f, 21f, -30f, 3.5f));
        props.add(new PropPlacement(barrelModel, 10f, 22f, 0f, 1.1f));
        props.add(new PropPlacement(treeSingleBModel, 11.5f, 28f, 15f, 3.4f));
        // Authored garden beds give each residential frontage an edge without filling streets.
        float[][] beds = {{18f, 6f}, {19f, 12f}, {18f, 20f}, {-12f, 24f}, {11f, -7f}};
        for (float[] bed : beds) {
            for (int i = 0; i < 5; i++) {
                float bx = bed[0] + (i % 2) * .75f, bz = bed[1] + i * .6f;
                props.add(
                        new PropPlacement(
                                i % 2 == 0 ? flowerYellowModel : flowerPurpleModel,
                                bx,
                                bz,
                                i * 43f,
                                1.4f));
            }
            props.add(new PropPlacement(gardenBushModel, bed[0] + 1.2f, bed[1] + 1.5f, 25f, 1.7f));
        }

        // Residential tree pockets and small work yards, kept out of the central avenue.
        props.add(new PropPlacement(treeSingleAModel, 30f, 5f, 30f, 3f));
        props.add(new PropPlacement(treeSingleBModel, 30f, 17f, 115f, 2.7f));
        props.add(new PropPlacement(treeSingleAModel, 29f, 28f, 200f, 3.2f));
        props.add(new PropPlacement(wheelbarrowModel, -24f, 7f, -25f, 1.5f));
        props.add(new PropPlacement(sackModel, -24.8f, 6.3f, 15f, 1.3f));
        props.add(new PropPlacement(crateOpenModel, 19.5f, 26.5f, 45f, 1.2f));

        // Small outskirts work scenes: a mill delivery and a lumber yard, clear of city roads.
        props.add(new PropPlacement(wheelbarrowModel, -25f, 20f, 32f, 1.8f));
        props.add(new PropPlacement(sackModel, -26.4f, 19.8f, 0f, 1.5f));
        props.add(new PropPlacement(sackModel, -26f, 21.1f, 75f, 1.3f));
        props.add(new PropPlacement(palletModel, 27.5f, 29f, 15f, 1.7f));
        props.add(new PropPlacement(crateABigModel, 28.5f, 29.5f, 20f, 1.4f));
        props.add(new PropPlacement(barrelModel, 27f, 27.5f, 0f, 1.2f));
        // Blacksmith forge props (realistic scale)
        props.add(new PropPlacement(weaponRackModel, -9.5f, 4.0f, 90f, 2.2f));
        props.add(new PropPlacement(crateABigModel, -10.2f, 3.5f, 15f, 1.8f));
        props.add(new PropPlacement(crateBBigModel, -9.5f, 4.5f, 35f, 1.6f));
        props.add(new PropPlacement(palletModel, -11.0f, 4.5f, 0f, 1.6f));
        props.add(new PropPlacement(barrelModel, -9.5f, -1.0f, 0f, 1.3f));

        // Arcanist alchemy props (clean scholarly staging with potion crates and herbal sacks, no
        // barrels)
        props.add(new PropPlacement(crateOpenModel, 9.5f, -1.0f, 0f, 1.6f));
        props.add(new PropPlacement(sackModel, 9.5f, 3.8f, 0f, 1.5f));
        props.add(new PropPlacement(crateABigModel, 10.5f, 2.5f, 20f, 1.6f));

        // Expedition portal framing props
        props.add(new PropPlacement(flagBlueModel, -4.2f, -10f, 0f, 3.5f));
        props.add(new PropPlacement(flagBlueModel, 4.2f, -10f, 0f, 3.5f));
        props.add(new PropPlacement(weaponRackModel, 4.2f, -6.5f, -90f, 2.2f));
        props.add(new PropPlacement(crateABigModel, -4.2f, -6.5f, 20f, 1.8f));
        props.add(new PropPlacement(palletModel, -4.2f, -8.0f, 0f, 1.6f));

        // Diana the Huntress archery range props (framing Diana at -7.5, -5.5)
        props.add(new PropPlacement(targetModel, -10.5f, -5.5f, 90f, 2.5f));
        props.add(new PropPlacement(targetModel, -10.5f, -3.8f, 90f, 2.5f));
        props.add(new PropPlacement(bucketArrowsModel, -8.8f, -6.8f, 0f, 1.6f));
        props.add(new PropPlacement(weaponRackModel, -6.0f, -6.8f, 45f, 2.2f));

        // Merchant Caravan Encampment in Grand Market Square (Trade Pavilion, Crates, Barrels)
        props.add(new PropPlacement(tentModel, -7.5f, 16.5f, 35f, 3.4f));
        props.add(new PropPlacement(crateABigModel, -4.5f, 17.5f, 15f, 2.4f));
        props.add(new PropPlacement(barrelModel, -8.5f, 15.0f, 0f, 2.0f));
        props.add(new PropPlacement(barrelModel, -6.0f, 18.5f, 90f, 1.8f));
        props.add(new PropPlacement(sackModel, -5.0f, 15.5f, 25f, 1.6f));
        props.add(new PropPlacement(palletModel, -7.0f, 14.5f, 0f, 1.6f));
        props.add(new PropPlacement(wheelbarrowModel, -6.0f, 13.5f, 30f, 1.6f));
        props.add(new PropPlacement(palletModel, 4.0f, 15.0f, -20f, 1.6f));
        props.add(new PropPlacement(sackModel, 4.5f, 14.5f, 0f, 1.4f));
        props.add(new PropPlacement(barrelModel, 8.5f, 16.0f, 0f, 1.4f));
        props.add(new PropPlacement(crateABigModel, -8.0f, 10.0f, 0f, 1.8f));
        props.add(new PropPlacement(crateBBigModel, 8.0f, 10.0f, 15f, 1.8f));

        // Rural farm & lumber props
        props.add(new PropPlacement(wheelbarrowModel, -23.0f, 18.0f, -45f, 1.6f));
        props.add(new PropPlacement(ladderModel, -26.0f, 7.0f, 0f, 2.2f));
        props.add(new PropPlacement(barrelModel, -23.0f, 8.0f, 0f, 1.3f));

        // Outer forest and natural scenery outside fortress walls
        float[][] outerTrees = {
            // South Wall Wilds (Dense double-tiered woodland framing South Gate & ramparts)
            {-52f, 38f},
            {-46f, 39f},
            {-40f, 38f},
            {-34f, 39f},
            {-28f, 38f},
            {-22f, 39f},
            {-16f, 38f},
            {-10f, 39f},
            {-5f, 38f},
            {5f, 38f},
            {10f, 39f},
            {16f, 38f},
            {22f, 39f},
            {28f, 38f},
            {34f, 39f},
            {40f, 38f},
            {46f, 39f},
            {52f, 38f},
            {-54f, 44f},
            {-48f, 43f},
            {-42f, 45f},
            {-36f, 44f},
            {-30f, 43f},
            {-24f, 45f},
            {-18f, 44f},
            {-12f, 43f},
            {-6f, 45f},
            {6f, 45f},
            {12f, 43f},
            {18f, 44f},
            {24f, 45f},
            {30f, 43f},
            {36f, 44f},
            {42f, 45f},
            {48f, 43f},
            {54f, 44f},
            {-50f, 50f},
            {-44f, 51f},
            {-38f, 49f},
            {-32f, 50f},
            {-26f, 52f},
            {-20f, 50f},
            {-14f, 51f},
            {-8f, 49f},
            {8f, 49f},
            {14f, 51f},
            {20f, 50f},
            {26f, 52f},
            {32f, 50f},
            {38f, 49f},
            {44f, 51f},
            {50f, 50f},
            // West Perimeter Forest (Dense pine belt outside West Wall from Z = -46 to +36)
            {-40f, 32f},
            {-46f, 34f},
            {-42f, 26f},
            {-48f, 28f},
            {-42f, 20f},
            {-48f, 22f},
            {-43f, 14f},
            {-49f, 16f},
            {-42f, 8f},
            {-48f, 10f},
            {-43f, 2f},
            {-49f, 4f},
            {-42f, -4f},
            {-48f, -2f},
            {-43f, -10f},
            {-49f, -8f},
            {-42f, -16f},
            {-48f, -14f},
            {-43f, -22f},
            {-49f, -20f},
            {-42f, -28f},
            {-48f, -26f},
            {-43f, -34f},
            {-49f, -32f},
            {-42f, -40f},
            {-48f, -38f},
            {-43f, -46f},
            {-50f, -44f},
            {-54f, -20f},
            {-54f, 0f},
            {-55f, 20f},
            {-54f, 34f},
            // East Perimeter Forest (Dense pine belt outside East Wall from Z = -46 to +36)
            {40f, 32f},
            {46f, 34f},
            {42f, 26f},
            {48f, 28f},
            {42f, 20f},
            {48f, 22f},
            {43f, 14f},
            {49f, 16f},
            {42f, 8f},
            {48f, 10f},
            {43f, 2f},
            {49f, 4f},
            {42f, -4f},
            {48f, -2f},
            {43f, -10f},
            {49f, -8f},
            {42f, -16f},
            {48f, -14f},
            {43f, -22f},
            {49f, -20f},
            {42f, -28f},
            {48f, -26f},
            {43f, -34f},
            {49f, -32f},
            {42f, -40f},
            {48f, -38f},
            {43f, -46f},
            {50f, -44f},
            {54f, -20f},
            {54f, 0f},
            {55f, 20f},
            {54f, 34f},
            // North Rear Fortress Forest (Behind castle keep and rear ramparts)
            {-48f, -50f},
            {-42f, -52f},
            {-36f, -50f},
            {-30f, -52f},
            {-24f, -50f},
            {-18f, -52f},
            {-12f, -50f},
            {-6f, -53f},
            {0f, -54f},
            {6f, -53f},
            {12f, -50f},
            {18f, -52f},
            {24f, -50f},
            {30f, -52f},
            {36f, -50f},
            {42f, -52f},
            {48f, -50f},
            {-44f, -56f},
            {-32f, -58f},
            {-20f, -56f},
            {-8f, -58f},
            {8f, -58f},
            {20f, -56f},
            {32f, -58f},
            {44f, -56f}
        };
        for (int i = 0; i < outerTrees.length; i++) {
            float[] t = outerTrees[i];
            Model treeM =
                    (i % 4 == 0)
                            ? pineTallModel
                            : (i % 4 == 1)
                                    ? treesMediumModel
                                    : (i % 4 == 2) ? treesLargeModel : pineRoundModel;
            float scale =
                    (treeM == pineTallModel)
                            ? 3.2f
                            : (treeM == pineRoundModel)
                                    ? 2.8f
                                    : (treeM == treesLargeModel) ? 5.2f : 4.4f;
            props.add(
                    new PropPlacement(treeM, t[0], t[1], (t[0] * 23f + t[1] * 17f) % 360f, scale));
        }

        // Inner town decorative trees and rocks
        props.add(new PropPlacement(treeSingleBModel, 24.0f, -8.0f, 0f, 4.5f));
        props.add(new PropPlacement(treeSingleBModel, 24.0f, -15.0f, 0f, 4.5f));
        props.add(new PropPlacement(pineRoundModel, 14.0f, -26.0f, 0f, 2.5f));
        props.add(new PropPlacement(pineRoundModel, -14.0f, -26.0f, 0f, 2.5f));
        props.add(new PropPlacement(rockSingleAModel, -30.0f, -4.0f, 45f, 3.8f));
        props.add(new PropPlacement(rockSingleBModel, 30.0f, -4.0f, -30f, 3.8f));

        // Colossal Distant Mountain Horizon (scale 22.0f - 28.0f)
        float[][] colossalMountains = {
            // North Massive Wall behind Castle Keep
            {0f, -76f, 0f, 28f},
            {-38f, -72f, 25f, 25f},
            {38f, -72f, -25f, 25f},
            {-72f, -64f, 40f, 24f},
            {72f, -64f, -40f, 24f},
            // West Mountain Ranges
            {-82f, -32f, 80f, 25f},
            {-84f, 5f, 95f, 26f},
            {-80f, 40f, 110f, 24f},
            // East Mountain Ranges
            {82f, -32f, -80f, 25f},
            {84f, 5f, -95f, 26f},
            {80f, 40f, -110f, 24f},
            // South Valley Horizon
            {-54f, 62f, 150f, 22f},
            {54f, 62f, -150f, 22f}
        };
        for (float[] m : colossalMountains) {
            props.add(new PropPlacement(distantMountainsModel, m[0], m[1], m[2], m[3]));
        }

        // Intermediate Mountain Ridges with Pine Trees & Snowy Crags (scale 7.5f - 9.5f)
        float[][] intermediateMountains = {
            // South wall flanking ridges
            {-46f, 44f, 135f, 9.0f, 0},
            {-28f, 48f, 160f, 9.5f, 1},
            {28f, 48f, 200f, 9.5f, 1},
            {46f, 44f, 225f, 9.0f, 0},
            // North Rear Fortress Massif
            {-48f, -54f, 35f, 9.5f, 0},
            {-24f, -58f, 15f, 9.5f, 1},
            {0f, -60f, 0f, 10.0f, 2},
            {24f, -58f, -15f, 9.5f, 1},
            {48f, -54f, -35f, 9.5f, 0},
            // West Mountain Ridges
            {-58f, -42f, 55f, 9.0f, 1},
            {-60f, -22f, 75f, 9.5f, 2},
            {-60f, 0f, 90f, 9.5f, 0},
            {-60f, 22f, 105f, 9.0f, 1},
            {-58f, 40f, 120f, 9.5f, 2},
            // East Mountain Ridges
            {58f, -42f, -55f, 9.0f, 1},
            {60f, -22f, -75f, 9.5f, 2},
            {60f, 0f, -90f, 9.5f, 0},
            {60f, 22f, -105f, 9.0f, 1},
            {58f, 40f, -120f, 9.5f, 2}
        };
        for (float[] im : intermediateMountains) {
            Model mod =
                    (im[4] == 0) ? mountainAModel : (im[4] == 1) ? mountainBModel : mountainCModel;
            props.add(new PropPlacement(mod, im[0], im[1], im[2], im[3]));
        }

        // Expansive outer rolling hills framing the valley base
        float[][] outerHills = {
            // South wall base knolls
            {-48f, 39f, 0f, 6.5f},
            {-36f, 43f, 0f, 7.0f},
            {-20f, 45f, 0f, 6.5f},
            {20f, 45f, 0f, 6.5f},
            {36f, 43f, 0f, 7.0f},
            {48f, 39f, 0f, 6.5f},
            // West wall base knolls
            {-50f, -35f, 90f, 6.5f},
            {-52f, -18f, 90f, 6.5f},
            {-52f, 0f, 90f, 7.0f},
            {-52f, 18f, 90f, 6.5f},
            {-50f, 34f, 90f, 6.5f},
            // East wall base knolls
            {50f, -35f, -90f, 6.5f},
            {52f, -18f, -90f, 6.5f},
            {52f, 0f, -90f, 7.0f},
            {52f, 18f, -90f, 6.5f},
            {50f, 34f, -90f, 6.5f},
            // North rear base knolls
            {-38f, -50f, 0f, 7.0f},
            {0f, -54f, 0f, 7.5f},
            {38f, -50f, 0f, 7.0f}
        };
        for (float[] h : outerHills) {
            props.add(new PropPlacement(hillsModel, h[0], h[1], h[2], h[3]));
        }
        props.add(new PropPlacement(rockSingleAModel, -46f, -10f, 30f, 5.0f));
        props.add(new PropPlacement(rockSingleBModel, 46f, -10f, -30f, 5.0f));
        props.add(new PropPlacement(rockSingleAModel, -46f, 15f, 60f, 5.0f));
        props.add(new PropPlacement(rockSingleBModel, 46f, 15f, -60f, 5.0f));
        props.add(new PropPlacement(rockSingleAModel, -20f, 46f, 45f, 4.5f));
        props.add(new PropPlacement(rockSingleBModel, 20f, 46f, -45f, 4.5f));
    }

    private void buildLighting() {
        // Carefully placed street lamps framing key avenues and intersections without cluttering
        // All lamps placed in grass verges with 0 collision against road paths or buildings
        float[][] lampPositions = {
            {-3.4f, 29.0f}, {3.4f, 29.0f}, // South gate approach
            {-2.8f, 20.5f}, {2.8f, 20.5f}, // South market exit
            {-2.8f, 11.5f}, {2.8f, 11.5f}, // North market entrance
            {-2.8f, 6.0f}, {2.8f, 6.0f}, // Spawn avenue
            {-2.8f, -8.5f}, {2.8f, -8.5f}, // Central crossroads avenue
            {-3.2f, -21.0f}, {3.2f, -21.0f}, // Castle court gateway
            {17.5f, 10.5f}, {17.5f, 22.0f} // Residential front gardens
        };
        for (int i = 0; i < lampPositions.length; i++) {
            streetLights.add(new StreetLight(lampPositions[i][0], lampPositions[i][1], i * 0.9f));
        }

        // Warm fiery braziers at major gateways
        braziers.clear();
    }

    private void buildCollisionObstacles() {
        collisionObstacles.clear();

        // 1. Monumental Castle Keep at (0, -40) and bastion towers (scale 12.0f)
        collisionObstacles.add(Obstacle.box(-12.0f, -48.0f, 12.0f, -35.5f));
        collisionObstacles.add(Obstacle.circle(-16.0f, -38.0f, 4.0f));
        collisionObstacles.add(Obstacle.circle(16.0f, -38.0f, 4.0f));
        collisionObstacles.add(Obstacle.box(-26.0f, -40.0f, -12.0f, -36.0f));
        collisionObstacles.add(Obstacle.box(12.0f, -40.0f, 26.0f, -36.0f));
        collisionObstacles.add(Obstacle.circle(-26.0f, -38.0f, 3.8f));
        collisionObstacles.add(Obstacle.circle(26.0f, -38.0f, 3.8f));

        // Castle court gateway towers
        collisionObstacles.add(Obstacle.circle(-7.5f, -22.0f, 2.8f));
        collisionObstacles.add(Obstacle.circle(7.5f, -22.0f, 2.8f));

        // 2. Perimeter Fortress Walls
        // South Ramparts & Massive Closed Fortress Gate across the entire southern front
        collisionObstacles.add(Obstacle.box(-45.0f, 31.8f, 45.0f, 37.0f));
        collisionObstacles.add(Obstacle.circle(-5.5f, 34.0f, 3.2f));
        collisionObstacles.add(Obstacle.circle(5.5f, 34.0f, 3.2f));

        // North Rear Wall
        collisionObstacles.add(Obstacle.box(-45.0f, -47.5f, 45.0f, -44.0f));

        // West Wall
        collisionObstacles.add(Obstacle.box(-38.0f, -47.0f, -34.0f, 35.0f));

        // East Wall
        collisionObstacles.add(Obstacle.box(34.0f, -47.0f, 38.0f, 35.0f));

        // 3. Civic & District Architecture
        // Blacksmith Forge at (-15.5, 1.5)
        collisionObstacles.add(Obstacle.box(-19.5f, -2.5f, -11.5f, 5.5f));

        // Arcanist Sanctum (churchModel) at (15.5, 1.5)
        collisionObstacles.add(Obstacle.box(11.5f, -2.5f, 19.5f, 5.5f));

        // Barracks & Archery Range
        collisionObstacles.add(Obstacle.box(-20.0f, -19.0f, -12.0f, -11.0f));
        collisionObstacles.add(Obstacle.box(-29.0f, -19.0f, -21.0f, -11.0f));

        // Cathedral & Monastery at (21.5, -15.0)
        collisionObstacles.add(Obstacle.box(17.0f, -20.0f, 26.0f, -10.0f));

        // Windmill
        collisionObstacles.add(Obstacle.circle(-27.0f, 10.0f, 4.5f));

        // Market well: match the visible stone rim, keeping the central avenue open.
        collisionObstacles.add(Obstacle.circle(8f, 21f, 1.15f));
        for (float z : new float[] {9f, 24f})
            for (int side : new int[] {-1, 1})
                collisionObstacles.add(Obstacle.circle(side * 4.8f, z, .12f));
        // 4. Market Square
        collisionObstacles.add(Obstacle.box(-11.5f, 10.5f, -5.5f, 14.5f)); // Red Stall
        collisionObstacles.add(Obstacle.box(5.5f, 10.5f, 11.5f, 14.5f)); // Yellow Stall
        collisionObstacles.add(Obstacle.box(-11.5f, 17.5f, -5.5f, 21.5f)); // Blue Stall
        collisionObstacles.add(
                Obstacle.box(-20.5f, 12.5f, -12.5f, 20.5f)); // Market Tavern (West side)
        collisionObstacles.add(Obstacle.box(-9.5f, 14.8f, -4.0f, 18.5f)); // Merchant Caravan Wagon

        // 5. Residential City Block (Homes along East side of Column 2 street)
        collisionObstacles.add(Obstacle.box(19.0f, 4.5f, 26.0f, 10.5f));
        collisionObstacles.add(Obstacle.box(21.5f, 10.0f, 28.5f, 16.0f)); // Shifted yellow house
        collisionObstacles.add(Obstacle.box(20.0f, 16.0f, 27.0f, 22.0f));
        collisionObstacles.add(Obstacle.box(19.5f, 22.0f, 26.5f, 28.0f));
        collisionObstacles.add(Obstacle.box(19.0f, 27.5f, 26.0f, 33.5f));
    }

    public void resolvePlayerCollision(Entities.Player player) {
        if (player == null || player.getPosition() == null) return;
        float px = player.getPosition().x();
        float pz = player.getPosition().z();
        float r = 0.45f; // Player collision radius

        for (Obstacle obs : collisionObstacles) {
            if (obs.isBox) {
                float minX = obs.minX - r;
                float maxX = obs.maxX + r;
                float minZ = obs.minZ - r;
                float maxZ = obs.maxZ + r;

                if (px > minX && px < maxX && pz > minZ && pz < maxZ) {
                    float left = px - minX;
                    float right = maxX - px;
                    float top = pz - minZ;
                    float bottom = maxZ - pz;
                    float minPush = Math.min(Math.min(left, right), Math.min(top, bottom));

                    if (minPush == left) px = minX;
                    else if (minPush == right) px = maxX;
                    else if (minPush == top) pz = minZ;
                    else pz = maxZ;
                }
            } else {
                float dx = px - obs.cx;
                float dz = pz - obs.cz;
                float distSq = dx * dx + dz * dz;
                float minDist = obs.radius + r;

                if (distSq < minDist * minDist && distSq > 0.0001f) {
                    float dist = (float) Math.sqrt(distSq);
                    float push = minDist - dist;
                    px += (dx / dist) * push;
                    pz += (dz / dist) * push;
                }
            }
        }

        // Firm perimeter safety bounds: Prevent player from ever clipping through fortress walls
        px = Math.max(-33.5f, Math.min(33.5f, px));
        pz = Math.max(-43.5f, Math.min(31.35f, pz));
        player.getPosition().x(px).z(pz);
    }

    private static Vector3 v(float x, float y, float z) {
        return new Vector3().x(x).y(y).z(z);
    }

    public boolean isTeleporting() {
        return teleporting;
    }

    public void startTeleport() {
        teleporting = true;
        teleportTimer = 1.2f;
    }

    public boolean updateTeleport(float dt) {
        if (!teleporting) return false;
        teleportTimer -= dt;
        if (teleportTimer <= 0) {
            teleporting = false;
            return true;
        }
        return false;
    }

    public float teleportTimer() {
        return teleportTimer;
    }

    public boolean isNearPortal(float px, float pz) {
        float dx = px - PORTAL_X, dz = pz - PORTAL_Z;
        return (dx * dx + dz * dz) <= (PORTAL_RADIUS * PORTAL_RADIUS);
    }

    private final Map<CitadelNpc, CitizenRoutine> citizens = new EnumMap<>(CitadelNpc.class);

    /** Update only in the live city. Citizens pause for conversation and nearby players. */
    public void updateCitizens(float dt, float px, float pz, int rescuedCount) {
        for (CitadelNpc npc : CitadelNpc.rescuedNpcs(rescuedCount)) {
            CitizenRoutine citizen = citizens.computeIfAbsent(npc, CitizenRoutine::new);
            citizen.update(
                    dt,
                    npc == activeNpc || Math.hypot(px - citizen.x, pz - citizen.z) < 2.5f,
                    this::citizenWalkable);
        }
    }

    boolean citizenWalkable(float x, float z) {
        for (Obstacle o : collisionObstacles) {
            if (o.isBox) {
                if (x > o.minX - 1.1f
                        && x < o.maxX + 1.1f
                        && z > o.minZ - 1.1f
                        && z < o.maxZ + 1.1f) return false;
            } else if (Math.hypot(x - o.cx, z - o.cz) < o.radius + .4f) return false;
        }
        return Math.abs(x) < 33 && z > -43 && z < 31;
    }

    public float npcX(CitadelNpc npc) {
        CitizenRoutine citizen = citizens.get(npc);
        return citizen == null ? npc.x() : citizen.x;
    }

    public float npcZ(CitadelNpc npc) {
        CitizenRoutine citizen = citizens.get(npc);
        return citizen == null ? npc.z() : citizen.z;
    }

    public CitadelNpc checkNearbyNpc(float px, float pz, int rescuedCount) {
        for (CitadelNpc npc :
                new CitadelNpc[] {
                    CitadelNpc.COMMANDER,
                    CitadelNpc.BLACKSMITH,
                    CitadelNpc.ARCANIST,
                    CitadelNpc.ARCHER,
                    CitadelNpc.CARAVAN_TRADER
                }) {
            if (npc.isNear(px, pz, 3.2f)) return npc;
        }
        for (CitadelNpc npc : CitadelNpc.rescuedNpcs(rescuedCount)) {
            CitizenRoutine citizen = citizens.get(npc);
            float x = citizen == null ? npc.x() : citizen.x;
            float z = citizen == null ? npc.z() : citizen.z;
            if (Math.hypot(px - x, pz - z) < 3.2f) return npc;
        }
        return null;
    }

    public CitadelNpc checkNearbyNpc(float px, float pz) {
        return checkNearbyNpc(px, pz, 0);
    }

    public CitadelNpc activeNpc() {
        return activeNpc;
    }

    public void openNpc(CitadelNpc npc, Progression progress) {
        this.activeNpc = npc;
        this.npcDialogue = npc != null ? npc.dynamicGreeting(progress) : "";
    }

    public void openNpc(CitadelNpc npc) {
        openNpc(npc, null);
    }

    public void closeNpc() {
        this.activeNpc = null;
        this.npcDialogue = "";
    }

    public void setLoreDialogue(String lore) {
        this.npcDialogue = lore;
    }

    public String npcDialogue() {
        return npcDialogue;
    }

    public boolean returnPortalOpen() {
        return returnPortalOpen;
    }

    public void setReturnPortalOpen(boolean open) {
        this.returnPortalOpen = open;
    }

    /** Binds every loaded KayKit model to the active lit / shadow shader pipeline. */
    public void prepareLighting(WorldAssets assets) {
        assets.externalSurface(.78f, .04f);
        for (Model model : allLoadedModels) {
            assets.lightExternalModel(model);
        }
    }

    /** Shadow pass: Renders opaque architectural geometry into the shadow map. */
    public void drawShadowArchitecture() {
        drawCastleTown(0f);
    }

    /** Main 3D pass: Renders the entire medieval city, districts, lighting, portal, and NPCs. */
    public void drawWorld(float time, float playerX, float playerZ, int rescuedCount) {
        // Deep foundation plane beneath the connected hexes extending to distant horizons
        DrawPlane(v(0, -4.05f, -6f), new Vector2().x(360).y(360), Menu.color(38, 48, 56));

        // Full KayKit Medieval City Architecture and Terrain
        drawCastleTown(time);

        // Expedition Teleportation Portal at the South Wall Exit
        drawPortal(PORTAL_X, PORTAL_Z, time, isNearPortal(playerX, playerZ));

        // Interactive NPC Stations (Forge campfire, Arcane altar)
        drawInteractiveStations(time);

        // Street Lanterns and Warm Braziers
        drawLighting(time);

        // 3D Animated / Stanced NPCs
        drawNpcs(time, playerX, playerZ, rescuedCount);

        drawMarketPennants(time);
        drawBirds(time);
        // Volumetric low-poly drifting clouds pass
        drawClouds(time);

        // Active Teleport Beam FX
        if (teleporting) {
            drawTeleportBeam(PORTAL_X, PORTAL_Z, teleportTimer, time);
        }
    }

    public void drawWorld(float time, float playerX, float playerZ) {
        drawWorld(time, playerX, playerZ, 0);
    }

    public void drawMissionReturnPortal(float x, float z, float time, boolean near) {
        drawPortal(x, z, time, near);
        if (near) {
            DrawCircle3D(v(x, .05f, z), 2.2f, v(1, 0, 0), 90f, Menu.GOLD);
        }
    }

    private void drawModel(Model model, float x, float y, float z, float yaw, float scale) {
        if (model != null && model.meshCount() > 0) {
            DrawModelEx(model, v(x, y, z), v(0, 1, 0), yaw, v(scale, scale, scale), WHITE);
        }
    }

    private void drawClouds(float time) {
        if (!IsWindowReady()) return;
        float[][] cloudPuffs = {
            {-32f, 26f, -30f, 4.5f},
            {-26f, 27f, -28f, 5.2f},
            {-20f, 26f, -32f, 4.0f},
            {24f, 28f, -20f, 5.0f},
            {29f, 29f, -18f, 6.0f},
            {35f, 28f, -22f, 4.8f},
            {-16f, 25f, 15f, 4.2f},
            {-10f, 26f, 18f, 5.5f},
            {-4f, 25f, 14f, 4.0f},
            {16f, 27f, 20f, 5.0f},
            {22f, 28f, 23f, 5.8f},
            {28f, 27f, 19f, 4.5f},
            {-6f, 30f, -45f, 6.5f},
            {2f, 31f, -48f, 7.5f},
            {10f, 30f, -44f, 6.0f},
            // Outer mountain horizon cloud banks
            {-65f, 33f, -55f, 7.0f},
            {-58f, 34f, -50f, 8.2f},
            {-50f, 32f, -56f, 6.5f},
            {52f, 34f, -52f, 7.5f},
            {60f, 35f, -48f, 8.5f},
            {68f, 33f, -54f, 6.8f},
            {-60f, 32f, 25f, 6.5f},
            {-54f, 33f, 30f, 7.8f},
            {55f, 32f, 28f, 7.0f},
            {62f, 33f, 34f, 8.0f}
        };

        float drift = (time * 1.5f) % 280f - 140f;
        Color cloudColor = Menu.color(245, 248, 255);
        for (float[] p : cloudPuffs) {
            float cx = p[0] + drift;
            if (cx > 140f) cx -= 280f;
            else if (cx < -140f) cx += 280f;
            DrawSphere(v(cx, p[1], p[2]), p[3], cloudColor);
        }
    }

    private void drawCastleTown(float time) {
        // 1. Terrain & Road Hexagons
        for (HexPlacement tile : terrainTiles) {
            drawModel(tile.model, tile.x, 0f, tile.z, tile.yaw, tile.scale);
        }

        // 2. City Buildings & Fortifications
        for (BuildingPlacement b : buildings) {
            drawModel(b.model, b.x, 0f, b.z, b.yaw, b.scale);
        }

        // 3. Props, Banners, and Scenery
        for (PropPlacement p : props) {
            drawModel(
                    p.model,
                    p.x,
                    propGroundOffsets.getOrDefault(p.model, 0f) * p.scale,
                    p.z,
                    p.yaw,
                    p.scale);
        }
    }

    private void drawInteractiveStations(float time) {
        // Blacksmith's hearth stone and glowing forge fire at (-6.0, 0, 1.5)
        if (campfireModel != null && campfireModel.meshCount() > 0) {
            DrawModelEx(
                    campfireModel, v(-6.0f, 0, 1.5f), v(0, 1, 0), 0f, v(1.4f, 1.4f, 1.4f), WHITE);
        }

        // Arcanist's rune altar at (6.5, 0, 1.5)
        if (altarModel != null && altarModel.meshCount() > 0) {
            DrawModelEx(altarModel, v(6.5f, 0, 1.5f), v(0, 1, 0), 0f, v(1.2f, 1.2f, 1.2f), WHITE);
        }
    }

    private void drawLighting(float time) {
        // Avenue Street Lanterns (rendered in authentic texture colors, firmly grounded at Y =
        // 0.0f)
        for (StreetLight light : streetLights) {
            float yaw = light.x < 0 ? 90f : -90f;
            if (lampPostModel != null && lampPostModel.meshCount() > 0) {
                DrawModelEx(
                        lampPostModel,
                        v(light.x, 0.0f, light.z),
                        v(0, 1, 0),
                        yaw,
                        v(2.0f, 2.0f, 2.0f),
                        WHITE);
            } else {
                DrawCylinder(
                        v(light.x, 1.4f, light.z), .08f, .10f, 2.8f, 8, Menu.color(48, 42, 38));
            }
        }
    }

    private void drawPortal(float x, float z, float time, boolean active) {
        // Portal base stones
        DrawCircle3D(v(x, .05f, z), PORTAL_RADIUS, v(1, 0, 0), 90f, Menu.color(45, 55, 70));
        DrawCircle3D(
                v(x, .08f, z),
                PORTAL_RADIUS * .85f,
                v(1, 0, 0),
                90f,
                active ? Menu.GOLD : Menu.TEAL);

        // Ancient carved stone portal ring / monolith
        if (portalRingModel != null && portalRingModel.meshCount() > 0) {
            DrawModelEx(
                    portalRingModel,
                    v(x, 0, z),
                    v(0, 1, 0),
                    0f,
                    v(2.0f, 2.0f, 2.0f),
                    active ? Menu.GOLD : Menu.TEAL);
        } else {
            DrawCylinderWires(v(x, 1.6f, z), 2.2f, 2.2f, 3.2f, 32, active ? Menu.GOLD : Menu.TEAL);
        }

        // Swirling arcane rings
        for (int i = 0; i < 3; i++) {
            float ringY = .2f + i * .9f + (float) Math.sin(time * 2.5f + i) * .15f;
            float pulse = .95f + (float) Math.sin(time * 3f + i * 1.5f) * .1f;
            Color ringColor =
                    active
                            ? Fade(Menu.GOLD, .7f - i * .15f)
                            : Fade(Menu.color(95, 205, 240), .65f - i * .15f);
            DrawCircle3D(v(x, ringY, z), 1.8f * pulse, v(0, 1, 0), 0f, ringColor);
        }

        // Floating energy sparks
        for (int i = 0; i < 16; i++) {
            double angle = (i * Math.PI / 8) + time * .4f;
            float r = 1.4f;
            float px = x + (float) Math.cos(angle) * r;
            float pz = z + (float) Math.sin(angle) * r;
            float py = .2f + ((time * .8f + i * .18f) % 2.8f);
            DrawSphere(v(px, py, pz), .06f, Fade(active ? Menu.GOLD : Menu.TEAL, 1f - py / 3f));
        }
    }

    private void drawTeleportBeam(float x, float z, float timer, float time) {
        float alpha = Math.max(0f, Math.min(1f, (1.2f - timer) * 2f));
        DrawCylinder(v(x, 6f, z), 2.2f, 2.4f, 12f, 32, Fade(Menu.GOLD, alpha * .6f));
        DrawCylinder(v(x, 6f, z), 1.2f, 1.3f, 12f, 32, Fade(Menu.GOLD, alpha * .55f));
        DrawCircle3D(v(x, .15f, z), 3.5f * (1.2f - timer), v(1, 0, 0), 90f, Menu.GOLD);
    }

    /** Market entrance pennants sway gently above the avenue. */
    private void drawMarketPennants(float time) {
        // High bunting frames the market entrances; supports stand outside the walkable avenue.
        for (float z : new float[] {9f, 24f}) {
            for (int side : new int[] {-1, 1}) {
                DrawCylinder(v(side * 4.8f, 2.4f, z), .07f, .1f, 4.8f, 8, Menu.color(94, 67, 43));
            }
            for (int i = 0; i < 12; i++) {
                float x = -4.8f + i * .8f;
                float h1 = 4.8f - .5f * (1 - Math.abs(x) / 4.8f);
                float h2 = 4.8f - .5f * (1 - Math.abs(x + .8f) / 4.8f);
                DrawLine3D(v(x, h1, z), v(x + .8f, h2, z), Menu.color(83, 69, 49));
                float breeze = (float) Math.sin(time * 1.8f + i * .8f) * .12f;
                Color cloth =
                        i % 3 == 0
                                ? Menu.color(189, 76, 60)
                                : i % 3 == 1 ? Menu.color(217, 175, 79) : Menu.color(75, 130, 159);
                Vector3 a = v(x + .12f, h1 - .03f, z),
                        b = v(x + .68f, h2 - .03f, z),
                        c = v(x + .4f, (h1 + h2) * .5f - .7f, z + breeze);
                DrawTriangle3D(a, b, c, cloth);
                DrawTriangle3D(c, b, a, cloth);
            }
        }
    }

    /** Distant birds circle over the wall and fields; tiny wing silhouettes keep the sky moving. */
    private void drawBirds(float time) {
        for (int i = 0; i < 9; i++) {
            float angle = time * (.07f + i * .002f) + i * .74f;
            float x = (float) Math.cos(angle) * (38 + i), z = (float) Math.sin(angle) * (42 + i);
            float height = 14 + i % 3 * 2, wing = (float) Math.sin(time * 5 + i) * .25f;
            Color tint = Menu.color(58, 69, 76);
            DrawLine3D(v(x - .45f, height + wing, z), v(x, height, z + .12f), tint);
            DrawLine3D(v(x, height, z + .12f), v(x + .45f, height + wing, z), tint);
        }
    }

    /** The same mesh and skeleton appear during the rescue and after the citizen returns home. */
    public void drawFieldSurvivor(
            WorldAssets assets, FieldEvent event, int rescuedCount, float time) {
        if (!event.visible()) return;
        int index = Math.min(citizenModels.length - 1, Math.max(0, rescuedCount));
        Model model = citizenModels[index];
        ModelAnimation clip =
                getAnimView(
                        citizenAnimations[index],
                        event.status() == FieldEvent.Status.ACTIVE ? 1 : 0,
                        citizenAnimationCounts[index]);
        if (model == null || model.meshCount() == 0) return;
        if (clip != null && IsModelAnimationValid(model, clip))
            UpdateModelAnimation(
                    model, clip, Entities.AnimationFrames.looping(time * 30, clip.keyframeCount()));
        assets.lightExternalModel(model);
        DrawModelEx(
                model, v(event.x(), 0, event.z()), v(0, 1, 0), event.facing(), v(1, 1, 1), WHITE);
    }

    private void drawNpcs(float time, float px, float pz, int rescuedCount) {
        // Goran (Blacksmith) at (-7.5, 0, 1.5)
        boolean nearGoran = CitadelNpc.BLACKSMITH.isNear(px, pz, 4f);
        int goranAnim = nearGoran ? 48 : 32; // Cheering vs Melee_Unarmed_Idle
        drawNpcCharacter(
                CitadelNpc.BLACKSMITH,
                knightModel,
                knightAnims,
                knightAnimCount,
                goranAnim,
                time,
                px,
                pz,
                Menu.color(190, 195, 205));

        // Lady Elyndra (Arcanist) at (7.5, 0, 1.5)
        boolean nearElyndra = CitadelNpc.ARCANIST.isNear(px, pz, 4f);
        int elyndraAnim = nearElyndra ? 51 : 49; // Cheering vs Ranged_Magic_Spellcasting
        drawNpcCharacter(
                CitadelNpc.ARCANIST,
                mageModel,
                mageAnims,
                mageAnimCount,
                elyndraAnim,
                time,
                px,
                pz,
                Menu.color(120, 150, 220));

        // Commander Sean (Expedition Gatekeeper) at the South Wall Exit
        int seanAnim = 1; // Melee_2H_Idle
        drawNpcCharacter(
                CitadelNpc.COMMANDER,
                commanderModel,
                commanderAnims,
                commanderAnimCount,
                seanAnim,
                time,
                px,
                pz,
                Menu.color(240, 240, 255));

        // Diana the Huntress (Archer Mentor) at Central Archery Range (-7.5, 0, -5.5)
        boolean nearArcher = CitadelNpc.ARCHER.isNear(px, pz, 4f);
        int archerAnim = nearArcher ? 57 : 54; // Cheering vs Ranged_Bow_Idle
        drawNpcCharacter(
                CitadelNpc.ARCHER,
                rangerModel,
                rangerAnims,
                rangerAnimCount,
                archerAnim,
                time,
                px,
                pz,
                Menu.color(140, 185, 130));

        // The merchant must be drawn at the same coordinates used by proximity and the map.
        drawNpcCharacter(
                CitadelNpc.CARAVAN_TRADER,
                barbarianModel,
                barbarianAnims,
                barbarianAnimCount,
                1,
                time,
                px,
                pz,
                Menu.color(225, 193, 140));

        // Every returned citizen owns a different mesh and its own remapped clips.
        CitadelNpc[] rescued = CitadelNpc.rescuedNpcs(rescuedCount);
        for (int i = 0; i < rescued.length; i++)
            drawNpcCharacter(
                    rescued[i],
                    citizenModels[i],
                    citizenAnimations[i],
                    citizenAnimationCounts[i],
                    0,
                    time,
                    px,
                    pz,
                    WHITE);
    }

    private ModelAnimation getAnimView(ModelAnimation source, int index, int count) {
        if (source == null || index < 0 || index >= count) return null;
        try {
            return new ModelAnimation(source).position(index);
        } catch (Exception ignored) {
            return null;
        }
    }

    private void drawNpcCharacter(
            CitadelNpc npc,
            Model model,
            ModelAnimation anims,
            int animCount,
            int animIndex,
            float time,
            float px,
            float pz,
            Color tint) {
        float x = npc.x(), y = npc.y(), z = npc.z();
        float yaw = npc.yaw();
        CitizenRoutine citizen = citizens.get(npc);
        if (citizen != null) {
            x = citizen.x;
            z = citizen.z;
            yaw = citizen.yaw;
            if (citizen.walking && anims != null) {
                for (int i = 0; i < animCount; i++) {
                    ModelAnimation candidate = getAnimView(anims, i, animCount);
                    if (candidate != null
                            && candidate.name().getString().toLowerCase().contains("walk")) {
                        animIndex = i;
                        break;
                    }
                }
            }
        }

        // Slight turn toward player when near
        boolean near = Math.hypot(px - x, pz - z) < 4f;
        if (near) {
            float dx = px - x, dz = pz - z;
            yaw = (float) Math.toDegrees(Math.atan2(dx, dz));
        }

        if (model != null && model.meshCount() > 0) {
            // Apply native matching animation clip without skeleton mismatch
            ModelAnimation clip = getAnimView(anims, animIndex, animCount);
            if (clip != null) {
                try {
                    if (IsModelAnimationValid(model, clip)) {
                        int frame =
                                (int)
                                        Entities.AnimationFrames.looping(
                                                time * 30f + npc.ordinal() * 12f,
                                                clip.keyframeCount());
                        UpdateModelAnimation(model, clip, frame);
                    }
                } catch (Exception ignored) {
                }
            }

            // Subtle breathing bounce
            float bounce = (float) Math.sin(time * 2f + x) * .02f;
            DrawModelEx(model, v(x, y + bounce, z), v(0, 1, 0), yaw, v(1.0f, 1.0f, 1.0f), tint);
        } else {
            DrawCapsule(v(x, y + .2f, z), v(x, y + 1.6f, z), .35f, 8, 8, tint);
            DrawSphere(v(x, y + 1.7f, z), .25f, Menu.GOLD);
        }

        // Circular floor aura if near
        if (near) {
            float pulse = .95f + (float) Math.sin(time * 4f) * .08f;
            DrawCircle3D(v(x, .04f, z), 1.2f * pulse, v(1, 0, 0), 90f, Menu.GOLD);
        }
    }

    @Override
    public void close() {
        if (!IsWindowReady()) return;
        if (lampPostTexture != null && lampPostTexture.id() > 0) {
            try {
                UnloadTexture(lampPostTexture);
            } catch (Exception ignored) {
            }
            lampPostTexture = null;
        }
        for (int i = 0; i < citizenAnimations.length; i++) {
            unloadAnims(citizenAnimations[i], citizenAnimationCounts[i]);
            citizenAnimations[i] = null;
            citizenAnimationCounts[i] = 0;
        }
        unloadAnims(commanderAnims, commanderAnimCount);
        commanderAnims = null;
        commanderAnimCount = 0;
        unloadAnims(knightAnims, knightAnimCount);
        knightAnims = null;
        knightAnimCount = 0;
        unloadAnims(mageAnims, mageAnimCount);
        mageAnims = null;
        mageAnimCount = 0;
        unloadAnims(rangerAnims, rangerAnimCount);
        rangerAnims = null;
        rangerAnimCount = 0;
        unloadAnims(barbarianAnims, barbarianAnimCount);
        barbarianAnims = null;
        barbarianAnimCount = 0;
        unloadAnims(archerAnims, archerAnimCount);
        archerAnims = null;
        archerAnimCount = 0;
        unloadAnims(shamanAnims, shamanAnimCount);
        shamanAnims = null;
        shamanAnimCount = 0;

        for (Model model : allLoadedModels) {
            if (model != null && model.meshCount() > 0) {
                try {
                    UnloadModel(model);
                } catch (Exception ignored) {
                }
            }
        }
        allLoadedModels.clear();
    }

    private void unloadAnims(ModelAnimation anims, int count) {
        if (anims != null && count > 0) {
            try {
                UnloadModelAnimations(anims, count);
            } catch (Exception ignored) {
            }
        }
    }
}
