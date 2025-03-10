package gdx.liftoff.game;

import com.badlogic.gdx.utils.IntSet;
import com.badlogic.gdx.utils.ObjectIntMap;

/**
 * Constants to provide names for the numbered tiles and entities in the {@code isometric-trpg.atlas}, a map
 * {@link #TILES} that allows looking up tile constants with a String name, and a map {@link #ENTITIES} that allows
 * looking up entity constants (for use in the four Animation Arrays).
 * <br>
 * There are several terms used in the tile names for grouping logically.
 * Tiles that are meant to be approximately half as tall as a unit voxel contain "HALF" in the constant name.
 * Tiles that are meant to just barely cover the top of a unit voxel contain "BASE" in their constant name.
 * Tiles that are meant to flow over the top of a surface of their height contain "COVER" in their constant name.
 * Cover tiles have a suffix containing "F", "G", and/or "H" depending on whether they cover the f or g vertical faces,
 * or the h face on top.
 * Tiles that have a path on them that changes depending on view angle have "PATH" in their constant name. Path tiles
 * have a suffix containing "F", "G", "T", and/or "R" depending on which faces are connected to the path, where fgtr are
 * the recommended keys on a QWERTY keyboard to move in those directions, or on a map of Europe, the locations of
 * France, Germany, Tallinn (in Estonia), and Reykjavík (in Iceland) relative to Amsterdam in the center.
 * Tiles that are not meant to be stackable have "DECO" in their constant name.
 * Bed tiles have "BED" in the name and a suffix indicating the faces that touch the footboard and headboard.
 * Some tiles have "CAP" in their name to indicate that only the top face is a given type, and the rest of the unit
 * voxel is simply dirt.
 */
@SuppressWarnings("PointlessArithmeticExpression")
public final class AssetNames {
    /**
     * No need to instantiate.
     */
    private AssetNames() {}

    public static final int DIRT                 =   0 +  0;
    public static final int GRASS                =   1 +  0;
    public static final int BASALT               =   2 +  0;
    public static final int SAND                 =   3 +  0;
    public static final int HALF_COVER_WATER_FGH =   4 +  0;
    public static final int HALF_COVER_WATER_FH  =   5 +  0;
    public static final int HALF_COVER_WATER_GH  =   6 +  0;
    public static final int HALF_COVER_WATER_H   =   7 +  0;
    public static final int DECO_ROCKS           =   8 +  0;
    public static final int DECO_BOULDER         =   9 +  0;
    public static final int DECO_CATTAIL         =  10 +  0;
    public static final int HALF_DIRT            =   0 + 11;
    public static final int HALF_GRASS           =   1 + 11;
    public static final int HALF_BASALT          =   2 + 11;
    public static final int HALF_SAND            =   3 + 11;
    public static final int BASE_COVER_WATER_FGH =   4 + 11;
    public static final int BASE_COVER_WATER_FH  =   5 + 11;
    public static final int BASE_COVER_WATER_GH  =   6 + 11;
    public static final int BASE_COVER_WATER_H   =   7 + 11;
    public static final int DECO_BUSH            =   8 + 11;
    public static final int DECO_STUMP           =   9 + 11;
    public static final int DECO_FLAME           =  10 + 11;
    public static final int SNOW                 =   0 + 22;
    public static final int ICE                  =   1 + 22;
    public static final int LAVA                 =   2 + 22;
    public static final int DRY                  =   3 + 22;
    public static final int HALF_COVER_SWAMP_FGH =   4 + 22;
    public static final int HALF_COVER_SWAMP_FH  =   5 + 22;
    public static final int HALF_COVER_SWAMP_GH  =   6 + 22;
    public static final int HALF_COVER_SWAMP_H   =   7 + 22;
    public static final int DECO_BUNDLE          =   8 + 22;
    public static final int DECO_HEDGE           =   9 + 22;
    public static final int DECO_CRYSTALS        =  10 + 22;
    public static final int HALF_SNOW            =   0 + 33;
    public static final int HALF_ICE             =   1 + 33;
    public static final int HALF_LAVA            =   2 + 33;
    public static final int HALF_DRY             =   3 + 33;
    public static final int BASE_COVER_SWAMP_FGH =   4 + 33;
    public static final int BASE_COVER_SWAMP_FH  =   5 + 33;
    public static final int BASE_COVER_SWAMP_GH  =   6 + 33;
    public static final int BASE_COVER_SWAMP_H   =   7 + 33;
    public static final int DECO_LOG             =   8 + 33;
    public static final int DECO_CACTUS          =   9 + 33;
    public static final int DECO_SPIKE           =  10 + 33;
    public static final int PATH_GRASS_GR        =   0 + 44;
    public static final int PATH_GRASS_FT        =   1 + 44;
    public static final int PATH_GRASS_FTR       =   2 + 44;
    public static final int PATH_GRASS_FGT       =   3 + 44;
    public static final int PATH_GRASS_FGTR      =   4 + 44;
    public static final int PATH_GRASS_GTR       =   5 + 44;
    public static final int PATH_GRASS_FGR       =   6 + 44;
    public static final int PATH_GRASS_FG        =   7 + 44;
    public static final int PATH_GRASS_GT        =   8 + 44;
    public static final int PATH_GRASS_TR        =   9 + 44;
    public static final int PATH_GRASS_FR        =  10 + 44;
    public static final int HALF_PATH_GRASS_GR   =   0 + 55;
    public static final int HALF_PATH_GRASS_FT   =   1 + 55;
    public static final int HALF_PATH_GRASS_FTR  =   2 + 55;
    public static final int HALF_PATH_GRASS_FGT  =   3 + 55;
    public static final int HALF_PATH_GRASS_FGTR =   4 + 55;
    public static final int HALF_PATH_GRASS_GTR  =   5 + 55;
    public static final int HALF_PATH_GRASS_FGR  =   6 + 55;
    public static final int HALF_PATH_GRASS_FG   =   7 + 55;
    public static final int HALF_PATH_GRASS_GT   =   8 + 55;
    public static final int HALF_PATH_GRASS_TR   =   9 + 55;
    public static final int HALF_PATH_GRASS_FR   =  10 + 55;
    public static final int PATH_DRY_GR          =   0 + 66;
    public static final int PATH_DRY_FT          =   1 + 66;
    public static final int PATH_DRY_FTR         =   2 + 66;
    public static final int PATH_DRY_FGT         =   3 + 66;
    public static final int PATH_DRY_FGTR        =   4 + 66;
    public static final int PATH_DRY_GTR         =   5 + 66;
    public static final int PATH_DRY_FGR         =   6 + 66;
    public static final int PATH_DRY_FG          =   7 + 66;
    public static final int PATH_DRY_GT          =   8 + 66;
    public static final int PATH_DRY_TR          =   9 + 66;
    public static final int PATH_DRY_FR          =  10 + 66;
    public static final int HALF_PATH_DRY_GR     =   0 + 77;
    public static final int HALF_PATH_DRY_FT     =   1 + 77;
    public static final int HALF_PATH_DRY_FTR    =   2 + 77;
    public static final int HALF_PATH_DRY_FGT    =   3 + 77;
    public static final int HALF_PATH_DRY_FGTR   =   4 + 77;
    public static final int HALF_PATH_DRY_GTR    =   5 + 77;
    public static final int HALF_PATH_DRY_FGR    =   6 + 77;
    public static final int HALF_PATH_DRY_FG     =   7 + 77;
    public static final int HALF_PATH_DRY_GT     =   8 + 77;
    public static final int HALF_PATH_DRY_TR     =   9 + 77;
    public static final int HALF_PATH_DRY_FR     =  10 + 77;
    public static final int SANDSTONE            =   0 + 88;
    public static final int SLATE                =   1 + 88;
    public static final int BRICK                =   2 + 88;
    public static final int WOOD                 =   3 + 88;
    public static final int CAP_SANDSTONE        =   4 + 88;
    public static final int CAP_SLATE            =   5 + 88;
    public static final int CAP_WOOD             =   6 + 88;
    public static final int CAP_THATCHED         =   7 + 88;
    public static final int DECO_BED_GR          =   8 + 88;
    public static final int DECO_BED_FT          =   9 + 88;
    public static final int DECO_FENCE_GR        =  10 + 88;
    public static final int HALF_SANDSTONE       =   0 + 99;
    public static final int HALF_SLATE           =   1 + 99;
    public static final int HALF_BRICK           =   2 + 99;
    public static final int HALF_WOOD            =   3 + 99;
    public static final int BASE_SANDSTONE       =   4 + 99;
    public static final int BASE_SLATE           =   5 + 99;
    public static final int BASE_WOOD            =   6 + 99;
    public static final int BASE_THATCHED        =   7 + 99;
    public static final int HALF_DECO_BED_GR     =   8 + 99;
    public static final int HALF_DECO_BED_FT     =   9 + 99;
    public static final int DECO_FENCE_FT        =  10 + 99;

    public static final int KNIGHT      = 0;
    public static final int LANCER      = 1;
    public static final int ARCHER      = 2;
    public static final int ROGUE       = 3;
    public static final int ORC_BRUTE   = 4;
    public static final int ORC_GUARD   = 5;
    public static final int ORC_BUTCHER = 6;
    public static final int ORC_SLINGER = 7;
    public static final int SHADOW      = 8;
    public static final int WIZARD      = 9;
    public static final int DEMON       = 10;
    public static final int CROSSBOWMAN = 11;
    public static final int MITE        = 12;
    public static final int FISH        = 13;
    public static final int BAT         = 14;
    public static final int JELLY       = 15;

    /** Special index; not an animation. */
    public static final int TOMBSTONE   = 128;
    /** Special index; not an animation. */
    public static final int UNKNOWN     = 129;

    public static final ObjectIntMap<String> TILES = new ObjectIntMap<>(128);
    public static final ObjectIntMap<String> ENTITIES = new ObjectIntMap<>(24);
    public static final IntSet UNIT_VOXELS;
    public static final IntSet HALF_VOXELS;
    public static final IntSet BASE_VOXELS;
    public static final IntSet HALF_COVERS;
    public static final IntSet BASE_COVERS;
    public static final IntSet DECORATIONS;
    public static final IntSet HALF_DECORATIONS;

    static {
        TILES.put("dirt"                , DIRT                );
        TILES.put("grass"               , GRASS               );
        TILES.put("basalt"              , BASALT              );
        TILES.put("sand"                , SAND                );
        TILES.put("half cover water fgh", HALF_COVER_WATER_FGH);
        TILES.put("half cover water fh" , HALF_COVER_WATER_FH );
        TILES.put("half cover water gh" , HALF_COVER_WATER_GH );
        TILES.put("half cover water h"  , HALF_COVER_WATER_H  );
        TILES.put("deco rocks"          , DECO_ROCKS          );
        TILES.put("deco boulder"        , DECO_BOULDER        );
        TILES.put("deco cattail"        , DECO_CATTAIL        );
        TILES.put("half dirt"           , HALF_DIRT           );
        TILES.put("half grass"          , HALF_GRASS          );
        TILES.put("half basalt"         , HALF_BASALT         );
        TILES.put("half sand"           , HALF_SAND           );
        TILES.put("base cover water fgh", BASE_COVER_WATER_FGH);
        TILES.put("base cover water fh" , BASE_COVER_WATER_FH );
        TILES.put("base cover water gh" , BASE_COVER_WATER_GH );
        TILES.put("base cover water h"  , BASE_COVER_WATER_H  );
        TILES.put("deco bush"           , DECO_BUSH           );
        TILES.put("deco stump"          , DECO_STUMP          );
        TILES.put("deco flame"          , DECO_FLAME          );
        TILES.put("snow"                , SNOW                );
        TILES.put("ice"                 , ICE                 );
        TILES.put("lava"                , LAVA                );
        TILES.put("dry"                 , DRY                 );
        TILES.put("half cover swamp fgh", HALF_COVER_SWAMP_FGH);
        TILES.put("half cover swamp fh" , HALF_COVER_SWAMP_FH );
        TILES.put("half cover swamp gh" , HALF_COVER_SWAMP_GH );
        TILES.put("half cover swamp h"  , HALF_COVER_SWAMP_H  );
        TILES.put("deco bundle"         , DECO_BUNDLE         );
        TILES.put("deco hedge"          , DECO_HEDGE          );
        TILES.put("deco crystals"       , DECO_CRYSTALS       );
        TILES.put("half snow"           , HALF_SNOW           );
        TILES.put("half ice"            , HALF_ICE            );
        TILES.put("half lava"           , HALF_LAVA           );
        TILES.put("half dry"            , HALF_DRY            );
        TILES.put("base cover swamp fgh", BASE_COVER_SWAMP_FGH);
        TILES.put("base cover swamp fh" , BASE_COVER_SWAMP_FH );
        TILES.put("base cover swamp gh" , BASE_COVER_SWAMP_GH );
        TILES.put("base cover swamp h"  , BASE_COVER_SWAMP_H  );
        TILES.put("deco log"            , DECO_LOG            );
        TILES.put("deco cactus"         , DECO_CACTUS         );
        TILES.put("deco spike"          , DECO_SPIKE          );
        TILES.put("path grass gr"       , PATH_GRASS_GR       );
        TILES.put("path grass ft"       , PATH_GRASS_FT       );
        TILES.put("path grass ftr"      , PATH_GRASS_FTR      );
        TILES.put("path grass fgt"      , PATH_GRASS_FGT      );
        TILES.put("path grass fgtr"     , PATH_GRASS_FGTR     );
        TILES.put("path grass gtr"      , PATH_GRASS_GTR      );
        TILES.put("path grass fgr"      , PATH_GRASS_FGR      );
        TILES.put("path grass fg"       , PATH_GRASS_FG       );
        TILES.put("path grass gt"       , PATH_GRASS_GT       );
        TILES.put("path grass tr"       , PATH_GRASS_TR       );
        TILES.put("path grass fr"       , PATH_GRASS_FR       );
        TILES.put("half path grass gr"  , HALF_PATH_GRASS_GR  );
        TILES.put("half path grass ft"  , HALF_PATH_GRASS_FT  );
        TILES.put("half path grass ftr" , HALF_PATH_GRASS_FTR );
        TILES.put("half path grass fgt" , HALF_PATH_GRASS_FGT );
        TILES.put("half path grass fgtr", HALF_PATH_GRASS_FGTR);
        TILES.put("half path grass gtr" , HALF_PATH_GRASS_GTR );
        TILES.put("half path grass fgr" , HALF_PATH_GRASS_FGR );
        TILES.put("half path grass fg"  , HALF_PATH_GRASS_FG  );
        TILES.put("half path grass gt"  , HALF_PATH_GRASS_GT  );
        TILES.put("half path grass tr"  , HALF_PATH_GRASS_TR  );
        TILES.put("half path grass fr"  , HALF_PATH_GRASS_FR  );
        TILES.put("path dry gr"         , PATH_DRY_GR         );
        TILES.put("path dry ft"         , PATH_DRY_FT         );
        TILES.put("path dry ftr"        , PATH_DRY_FTR        );
        TILES.put("path dry fgt"        , PATH_DRY_FGT        );
        TILES.put("path dry fgtr"       , PATH_DRY_FGTR       );
        TILES.put("path dry gtr"        , PATH_DRY_GTR        );
        TILES.put("path dry fgr"        , PATH_DRY_FGR        );
        TILES.put("path dry fg"         , PATH_DRY_FG         );
        TILES.put("path dry gt"         , PATH_DRY_GT         );
        TILES.put("path dry tr"         , PATH_DRY_TR         );
        TILES.put("path dry fr"         , PATH_DRY_FR         );
        TILES.put("half path dry gr"    , HALF_PATH_DRY_GR    );
        TILES.put("half path dry ft"    , HALF_PATH_DRY_FT    );
        TILES.put("half path dry ftr"   , HALF_PATH_DRY_FTR   );
        TILES.put("half path dry fgt"   , HALF_PATH_DRY_FGT   );
        TILES.put("half path dry fgtr"  , HALF_PATH_DRY_FGTR  );
        TILES.put("half path dry gtr"   , HALF_PATH_DRY_GTR   );
        TILES.put("half path dry fgr"   , HALF_PATH_DRY_FGR   );
        TILES.put("half path dry fg"    , HALF_PATH_DRY_FG    );
        TILES.put("half path dry gt"    , HALF_PATH_DRY_GT    );
        TILES.put("half path dry tr"    , HALF_PATH_DRY_TR    );
        TILES.put("half path dry fr"    , HALF_PATH_DRY_FR    );
        TILES.put("sandstone"           , SANDSTONE           );
        TILES.put("slate"               , SLATE               );
        TILES.put("brick"               , BRICK               );
        TILES.put("wood"                , WOOD                );
        TILES.put("cap sandstone"       , CAP_SANDSTONE       );
        TILES.put("cap slate"           , CAP_SLATE           );
        TILES.put("cap wood"            , CAP_WOOD            );
        TILES.put("cap thatched"        , CAP_THATCHED        );
        TILES.put("deco bed gr"         , DECO_BED_GR         );
        TILES.put("deco bed ft"         , DECO_BED_FT         );
        TILES.put("deco fence gr"       , DECO_FENCE_GR       );
        TILES.put("half sandstone"      , HALF_SANDSTONE      );
        TILES.put("half slate"          , HALF_SLATE          );
        TILES.put("half brick"          , HALF_BRICK          );
        TILES.put("half wood"           , HALF_WOOD           );
        TILES.put("base sandstone"      , BASE_SANDSTONE      );
        TILES.put("base slate"          , BASE_SLATE          );
        TILES.put("base wood"           , BASE_WOOD           );
        TILES.put("base thatched"       , BASE_THATCHED       );
        TILES.put("half deco bed gr"    , HALF_DECO_BED_GR    );
        TILES.put("half deco bed ft"    , HALF_DECO_BED_FT    );
        TILES.put("deco fence ft"       , DECO_FENCE_FT       );

        ENTITIES.put("knight"      , KNIGHT      );
        ENTITIES.put("lancer"      , LANCER      );
        ENTITIES.put("archer"      , ARCHER      );
        ENTITIES.put("rogue"       , ROGUE       );
        ENTITIES.put("orc brute"   , ORC_BRUTE   );
        ENTITIES.put("orc guard"   , ORC_GUARD   );
        ENTITIES.put("orc butcher" , ORC_BUTCHER );
        ENTITIES.put("orc slinger" , ORC_SLINGER );
        ENTITIES.put("shadow"      , SHADOW      );
        ENTITIES.put("wizard"      , WIZARD      );
        ENTITIES.put("demon"       , DEMON       );
        ENTITIES.put("crossbowman" , CROSSBOWMAN );
        ENTITIES.put("mite"        , MITE        );
        ENTITIES.put("fish"        , FISH        );
        ENTITIES.put("bat"         , BAT         );
        ENTITIES.put("jelly"       , JELLY       );

        UNIT_VOXELS = new IntSet(128);
        HALF_VOXELS = new IntSet(64);
        BASE_VOXELS = new IntSet(64);
        HALF_COVERS = new IntSet(16);
        BASE_COVERS = new IntSet(16);
        DECORATIONS = new IntSet(32);
        HALF_DECORATIONS = new IntSet(4);

        UNIT_VOXELS.addAll(TILES.values().toArray());

        for(ObjectIntMap.Entry<String> e : TILES.entries()){
            if(e.key.contains("half")){
                if(e.key.contains("cover")) HALF_COVERS.add(e.value);
                else HALF_VOXELS.add(e.value);
                UNIT_VOXELS.remove(e.value);
            }
            if(e.key.contains("base")){
                if(e.key.contains("cover")) BASE_COVERS.add(e.value);
                else BASE_VOXELS.add(e.value);
                UNIT_VOXELS.remove(e.value);
            }
            if(e.key.contains("deco")){
                if(e.key.contains("half")) HALF_DECORATIONS.add(e.value);
                else DECORATIONS.add(e.value);
                UNIT_VOXELS.remove(e.value);
            }
        }
    }
}
