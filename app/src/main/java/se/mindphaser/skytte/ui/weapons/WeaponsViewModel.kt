package se.mindphaser.skytte.ui.weapons

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import se.mindphaser.skytte.data.Weapon
import se.mindphaser.skytte.data.repo.WeaponRepository
import se.mindphaser.skytte.ui.repositories

class WeaponsViewModel(private val repo: WeaponRepository) : ViewModel() {
    val weapons: Flow<List<Weapon>> = repo.observeAll()

    fun save(weapon: Weapon) {
        viewModelScope.launch { repo.save(weapon) }
    }

    fun delete(weapon: Weapon) {
        viewModelScope.launch { repo.delete(weapon) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { WeaponsViewModel(repositories().weapons) }
        }
    }
}
