package se.mindphaser.skytte.ui.weapons

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import se.mindphaser.skytte.data.Weapon
import se.mindphaser.skytte.data.WeaponDao
import se.mindphaser.skytte.ui.database

class WeaponsViewModel(private val dao: WeaponDao) : ViewModel() {
    val weapons: Flow<List<Weapon>> = dao.observeAll()

    fun save(weapon: Weapon) {
        viewModelScope.launch {
            if (weapon.id == 0L) dao.insert(weapon) else dao.update(weapon)
        }
    }

    fun delete(weapon: Weapon) {
        viewModelScope.launch { dao.delete(weapon) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { WeaponsViewModel(database().weaponDao()) }
        }
    }
}
