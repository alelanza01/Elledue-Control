package it.elledue.control.data

import kotlinx.coroutines.flow.map

class SettingsRepository(private val dao: SettingsDao) {
    val settings = dao.observe().map { it ?: AppSettings() }

    suspend fun saveOrganizationName(name: String) {
        val normalized = name.trim()
        require(normalized.isNotEmpty()) { "Organization name must not be blank" }
        dao.save(AppSettings(organizationName = normalized))
    }
}
