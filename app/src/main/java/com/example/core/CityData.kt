package com.example.core

data class CityOption(
    val name: String,
    val state: String,
    val neighborhoods: List<String>,
    val emoji: String
)

object CityData {
    const val defaultCity = "Bengaluru"
    const val ADMIN_EMAIL = "amosanand18@gmail.com"

    val indianCities = listOf(
        CityOption("Bengaluru", "Karnataka", listOf("Indiranagar", "Koramangala", "HSR Layout", "Jayanagar", "Whitefield", "Malleshwaram"), "🌿"),
        CityOption("Mumbai", "Maharashtra", listOf("Bandra West", "Andheri", "Colaba", "Powai", "Dadar", "Lower Parel"), "🌊"),
        CityOption("Delhi NCR", "Delhi", listOf("Hauz Khas", "Connaught Place", "Saket", "Gurugram DLF", "Noida Sec 18"), "🏛️"),
        CityOption("Pune", "Maharashtra", listOf("Koregaon Park", "Kothrud", "Viman Nagar", "Baner", "FC Road"), "⛰️"),
        CityOption("Hyderabad", "Telangana", listOf("Jubilee Hills", "Banjara Hills", "Gachibowli", "Madhapur", "Secunderabad"), "💎"),
        CityOption("Chennai", "Tamil Nadu", listOf("Adyar", "Mylapore", "Anna Nagar", "Besant Nagar", "T. Nagar"), "🌴"),
        CityOption("Jaipur", "Rajasthan", listOf("C-Scheme", "Malviya Nagar", "Vaishali Nagar", "Bapu Nagar"), "🏰"),
        CityOption("Kolkata", "West Bengal", listOf("Park Street", "Salt Lake", "Ballygunge", "New Town"), "🚋"),
        CityOption("Ahmedabad", "Gujarat", listOf("Navrangpura", "Satellite", "Bodakdev", "Prahlad Nagar"), "🪁"),
        CityOption("Kochi", "Kerala", listOf("Fort Kochi", "Edappally", "Kakkanad", "Panampilly Nagar"), "⛵")
    )

    val interestOptions = listOf(
        "Chai & Cafes", "Startups & Tech", "Street Food", "Fitness & Runs",
        "Books & Art", "Live Music", "Local Deals", "Photography", "Cycling", "Volunteering"
    )

    val avatarEmojis = listOf("🧑🏽‍💻", "👩🏽‍🎨", "🧑🏽‍🍳", "🚴🏽", "📚", "🎸", "☕", "🌿", "🏙️", "✨")

    val communityCategories = listOf(
        "Neighborhood", "Food & Chai", "Fitness & Outdoors", "Tech & Startups",
        "Books & Culture", "Pets & Gardening", "Parents & Family"
    )

    val marketplaceCategories = listOf(
        "All", "Furniture", "Electronics", "Cycles & Mobility", "Books", "Home & Kitchen", "Fashion"
    )

    val businessCategories = listOf(
        "Cafe & Chai", "Bakery & Food", "Fitness & Yoga", "Books & Stationery",
        "Home Services", "Boutique & Crafts"
    )

    val eventCategories = listOf(
        "Meetup", "Music & Arts", "Fitness & Run", "Workshop", "Food Walk", "Books & Culture"
    )

    fun neighborhoodsFor(city: String): List<String> =
        indianCities.firstOrNull { it.name == city }?.neighborhoods ?: listOf("Central")
}
