package it.elledue.control.data

import kotlinx.coroutines.flow.map

class SettingsRepository(private val dao: SettingsDao) {
    val settings = dao.observe().map { it ?: AppSettings() }

    suspend fun cover(value:Int) { require(value in 0..3); dao.save((dao.get() ?: AppSettings()).copy(homeCover=value)) }

    suspend fun saveOrganizationName(name: String) {
        val normalized = name.trim()
        require(normalized.isNotEmpty()) { "Organization name must not be blank" }
        dao.save((dao.get() ?: AppSettings()).copy(organizationName = normalized))
    }
    suspend fun saveProfile(first: String, last: String, photo: String?) {
        require(first.length <= 100 && last.length <= 100)
        LocalImages.validate(photo)
        dao.save((dao.get() ?: AppSettings()).copy(operatorFirstName = first.trim(), operatorLastName = last.trim(), operatorPhotoBase64 = photo))
    }
}
