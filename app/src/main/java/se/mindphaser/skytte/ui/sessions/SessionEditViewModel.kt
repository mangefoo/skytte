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
import se.mindphaser.skytte.data.AmmunitionDao
import se.mindphaser.skytte.data.Session
import se.mindphaser.skytte.data.SessionDao
import se.mindphaser.skytte.data.Weapon
import se.mindphaser.skytte.data.WeaponDao
import se.mindphaser.skytte.ui.database
import java.time.LocalDate

class SessionEditViewModel(
    private val sessionDao: SessionDao,
    weaponDao: WeaponDao,
    ammunitionDao: AmmunitionDao
) : ViewModel() {

    var date by mutableStateOf(LocalDate.now())
    var location by mutableStateOf("")
    var weaponId: Long? by mutableStateOf(null)
    var ammunitionId: Long? by mutableStateOf(null)
    var ammoCountText by mutableStateOf("")
    var shootingType by mutableStateOf("")
    var loaded by mutableStateOf(false)
        private set

    private var editingId: Long? = null

    val weapons: StateFlow<List<Weapon>> =
        weaponDao.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val ammunitionList: StateFlow<List<Ammunition>> =
        ammunitionDao.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun load(id: Long?) {
        if (loaded) return
        editingId = id
        if (id == null) {
            loaded = true
            return
        }
        viewModelScope.launch {
            sessionDao.byId(id)?.let { s ->
                date = s.session.date
                location = s.session.location
                weaponId = s.session.weaponId
                ammunitionId = s.session.ammunitionId
                ammoCountText = s.session.ammoCount.toString()
                shootingType = s.session.shootingType
            }
            loaded = true
        }
    }

    val isEditing: Boolean get() = editingId != null

    fun save(onDone: () -> Unit) {
        viewModelScope.launch {
            val s = Session(
                id = editingId ?: 0L,
                date = date,
                location = location.trim(),
                weaponId = weaponId,
                ammunitionId = ammunitionId,
                ammoCount = ammoCountText.toIntOrNull() ?: 0,
                shootingType = shootingType.trim()
            )
            if (editingId == null) sessionDao.insert(s) else sessionDao.update(s)
            onDone()
        }
    }

    fun delete(onDone: () -> Unit) {
        val id = editingId ?: return
        viewModelScope.launch {
            sessionDao.byId(id)?.session?.let { sessionDao.delete(it) }
            onDone()
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val db = database()
                SessionEditViewModel(db.sessionDao(), db.weaponDao(), db.ammunitionDao())
            }
        }
    }
}
