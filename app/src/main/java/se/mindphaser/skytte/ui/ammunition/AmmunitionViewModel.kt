package se.mindphaser.skytte.ui.ammunition

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import se.mindphaser.skytte.data.Ammunition
import se.mindphaser.skytte.data.AmmunitionDao
import se.mindphaser.skytte.ui.database

class AmmunitionViewModel(private val dao: AmmunitionDao) : ViewModel() {
    val items: Flow<List<Ammunition>> = dao.observeAll()

    fun save(ammo: Ammunition) {
        viewModelScope.launch {
            if (ammo.id == 0L) dao.insert(ammo) else dao.update(ammo)
        }
    }

    fun delete(ammo: Ammunition) {
        viewModelScope.launch { dao.delete(ammo) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { AmmunitionViewModel(database().ammunitionDao()) }
        }
    }
}
