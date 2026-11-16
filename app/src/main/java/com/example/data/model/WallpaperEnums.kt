package com.example.data.model

import androidx.compose.ui.graphics.Color

enum class WallpaperCategory(
    val displayName: String,
    val searchQuery: String,
    val wallhavenCategories: String = "100",
    val isFetchableOnline: Boolean = true,
    val fallbackSearchQueries: List<String> = emptyList(),
) {
    ALL("Všetko", "", "110", isFetchableOnline = false),
    KAWAII("Kawaii & Anime", "kawaii anime", "010"),
    BLACK_GIRLY("Black Girly", "black", "100"),
    COQUETTE("Coquette", "bow", "100"),
    PETS("Zvieratká", "cute", "100"),
    NATURE("Príroda", "nature", "100"),
    FLOWERS("Kvety", "flowers", "100"),
    PASTEL("Pastel", "pastel", "100", fallbackSearchQueries = listOf("clouds")),
    CUSTOM("Moje", "", "100", isFetchableOnline = false);

    companion object {
        fun fromName(value: String?): WallpaperCategory {
            return entries.firstOrNull { it.name == value } ?: ALL
        }
    }
}

enum class WallpaperColorFilter(
    val displayName: String,
    val wallhavenHex: String?,
    val swatchColor: Color,
) {
    ALL("Všetky", null, Color(0xFFF3D5E3)),
    PINK("Ružová", "ff99cc", Color(0xFFFF80AB)),
    BLACK("Čierna", "000000", Color(0xFF221D21)),
    LAVENDER("Fialová", "cc99ff", Color(0xFFCE93D8)),
    PEACH("Broskyňová", "ffcc99", Color(0xFFFFAB91)),
    MINT("Mätová", "99ffcc", Color(0xFF80CBC4));
}

enum class LiveEffectType(
    val displayName: String,
    val description: String,
) {
    PETALS("Sakurové lupene", "Jemné padajúce ružové lupienky"),
    SPARKLES("Trblietky", "Iskrivé hviezdičky a svetelný prach"),
    HEARTS("Srdiečka", "Vznášajúce sa pastelové srdiečka"),
    NONE("Bez efektu", "Čisté statické pozadie");

    companion object {
        fun fromName(value: String?): LiveEffectType {
            return entries.firstOrNull { it.name == value } ?: PETALS
        }
    }
}

enum class EditorTextPosition(val displayName: String) {
    TOP("Hore"),
    CENTER("Stred"),
    BOTTOM("Dole"),
}

enum class EditorSticker(val displayName: String, val symbol: String) {
    NONE("Žiadna", ""),
    SPARKLES("Trblietky", "✦ ✧ ✦"),
    HEARTS("Srdiečka", "♡ ♥ ♡"),
    BOW("Mašlička", "୨ৎ"),
    BUTTERFLY("Motýľ", "Ƹ̵̡Ӝ̵̨̄Ʒ"),
}

enum class WallpaperTarget(
    val displayName: String,
    val description: String,
) {
    HOME("Domovská plocha", "Plocha s ikonami a aplikáciami"),
    LOCK("Zamknutá plocha", "Obrazovka pred odomknutím telefónu"),
    BOTH("Obe plochy", "Domovská aj zamknutá obrazovka naraz");

    companion object {
        fun fromName(value: String?): WallpaperTarget {
            return entries.firstOrNull { it.name == value } ?: BOTH
        }
    }
}

enum class RotationMode(
    val displayName: String,
    val description: String,
    val intervalMinutes: Long? = null,
) {
    STATIC("Statická tapeta", "Tapeta sa nemení automaticky"),
    ON_UNLOCK("Pri každom odomknutí", "Nový obrázok hneď po odomknutí telefónu"),
    INTERVAL_15M("Každých 15 minút", "Svieža tapeta každú štvrťhodinu", 15L),
    INTERVAL_1H("Každú hodinu", "Jemná zmena každých 60 minút", 60L),
    INTERVAL_6H("Každých 6 hodín", "Nová atmosféra ráno, na obed aj večer", 360L),
    INTERVAL_24H("Raz za deň", "Každý deň jedno nové prekvapenie", 1440L);

    companion object {
        fun fromName(value: String?): RotationMode {
            return entries.firstOrNull { it.name == value } ?: STATIC
        }
    }
}

enum class RotationPool(
    val displayName: String,
    val description: String,
) {
    FAVORITES("Len obľúbené", "Strieda iba tvoje označené srdiečkové tapety"),
    CUSTOM_ONLY("Moja galéria", "Strieda obrázky nahrané z tvojho telefónu"),
    CATEGORY("Vybraná kategória", "Strieda tapety z jednej vybranej témy"),
    ALL_SAVED("Všetky v knižnici", "Strieda naprieč celou bankou tapiet");

    companion object {
        fun fromName(value: String?): RotationPool {
            return entries.firstOrNull { it.name == value } ?: FAVORITES
        }
    }
}

enum class ThemeMode(
    val displayName: String,
    val description: String,
) {
    SYSTEM("Podľa systému", "Prispôsobí sa nastaveniu telefónu"),
    LIGHT("Svetlý režim", "Jemný ružovo-krémový denný vzhľad"),
    DARK("Tmavý režim", "Elegantný zamatovo slivkový nočný vzhľad");

    companion object {
        fun fromName(value: String?): ThemeMode {
            return entries.firstOrNull { it.name == value } ?: SYSTEM
        }
    }
}
