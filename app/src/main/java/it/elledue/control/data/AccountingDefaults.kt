package it.elledue.control.data

object AccountingDefaults {
    val categories = listOf(
        ExpenseCategoryEntity("cleaning", "Pulizie", "CLEANING"),
        ExpenseCategoryEntity("laundry", "Lavanderia", "LAUNDRY"),
        ExpenseCategoryEntity("detergents", "Detergenti"), ExpenseCategoryEntity("amenities", "Kit cortesia"),
        ExpenseCategoryEntity("grocery", "Supermercato"), ExpenseCategoryEntity("maintenance", "Manutenzione"),
        ExpenseCategoryEntity("suppliers", "Fornitori"), ExpenseCategoryEntity("other", "Altre spese"),
    )
}
