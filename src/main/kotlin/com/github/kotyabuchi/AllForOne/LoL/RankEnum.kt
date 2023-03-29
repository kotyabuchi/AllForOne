package com.github.kotyabuchi.AllForOne.LoL

enum class RankTier(val point: Int, val hasDivision: Boolean = true) {
    IRON(20),
    BRONZE(35),
    SILVER(50),
    GOLD(65),
    PLATINUM(80),
    DIAMOND(95),
    MASTER(120,false),
    GRAND_MASTER(140, false),
    CHALLENGER(160, false);

    companion object {
        fun valueOfOrNull(type: String?): RankTier? = values().find { it.name == type }
    }

    fun camelString(): String {
        return name.split("_").joinToString(separator = " ") { it.lowercase().replaceFirstChar { it.uppercase() } }
    }
}

enum class RankDivision(val point: Int) {
    I(10),
    II(6),
    III(3),
    IV(0);

    companion object {
        fun fromArabic(num: Int?): RankDivision? {
            return when (num) {
                1 -> I
                2 -> II
                3 -> III
                4 -> IV
                else -> null
            }
        }
    }
}