package se.mindphaser.skytte.ui.sessions

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import se.mindphaser.skytte.data.Ammunition
import se.mindphaser.skytte.data.Session
import se.mindphaser.skytte.data.Weapon
import se.mindphaser.skytte.data.repo.AmmunitionRepository
import se.mindphaser.skytte.data.repo.SessionRepository
import se.mindphaser.skytte.data.repo.WeaponRepository
import se.mindphaser.skytte.ui.repositories
import java.time.LocalDate

class SessionEditViewModel(
    private val sessionRepo: SessionRepository,
    weaponRepo: WeaponRepository,
    ammunitionRepo: AmmunitionRepository
) : ViewModel() {

    var date by mutableStateOf(LocalDate.now())
    var location by mutableStateOf("")
    var weaponId: String? by mutableStateOf(null)
    var ammunitionId: String? by mutableStateOf(null)
    var ammoCountText by mutableStateOf("")
    var shootingType by mutableStateOf("")
    var feeText by mutableStateOf("")
    var feeIncludesAmmo by mutableStateOf(false)
    var loaded by mutableStateOf(false)
        private set

    private var editingId: String? by mutableStateOf(null)

    val weapons: StateFlow<List<Weapon>> =
        weaponRepo.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val ammunitionList: StateFlow<List<Ammunition>> =
        ammunitionRepo.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun load(id: String?) {
        if (loaded) return
        editingId = id
        if (id == null) {
            loaded = true
            return
        }
        viewModelScope.launch {
            sessionRepo.getSession(id)?.let { s ->
                date = s.date
                location = s.location
                weaponId = s.weaponId
                ammunitionId = s.ammunitionId
                ammoCountText = s.ammoCount.toString()
                shootingType = s.shootingType
                feeText = s.fee?.let {
                    String.format(java.util.Locale.forLanguageTag("sv-SE"), "%.2f", it)
                } ?: ""
                feeIncludesAmmo = s.feeIncludesAmmo
            }
            loaded = true
        }
    }

    val isEditing: Boolean get() = editingId != null

    fun save(onDone: () -> Unit) {
        viewModelScope.launch {
            val s = Session(
                id = editingId ?: "",
                date = date,
                location = location.trim(),
                weaponId = weaponId,
                ammunitionId = ammunitionId,
                ammoCount = ammoCountText.toIntOrNull() ?: 0,
                shootingType = shootingType.trim(),
                fee = feeText.replace(',', '.').toDoubleOrNull(),
                feeIncludesAmmo = feeIncludesAmmo
            )
            sessionRepo.save(s)
            onDone()
        }
    }

    fun delete(onDone: () -> Unit) {
        val id = editingId ?: return
        viewModelScope.launch {
            sessionRepo.getSession(id)?.let { sessionRepo.delete(it) }
            onDone()
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val repos = repositories()
                SessionEditViewModel(repos.sessions, repos.weapons, repos.ammunition)
            }
        }
    }
}
