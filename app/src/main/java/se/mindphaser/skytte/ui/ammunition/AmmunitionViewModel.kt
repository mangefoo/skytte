package se.mindphaser.skytte.ui.ammunition

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import se.mindphaser.skytte.data.Ammunition
import se.mindphaser.skytte.data.repo.AmmunitionRepository
import se.mindphaser.skytte.ui.repositories

class AmmunitionViewModel(private val repo: AmmunitionRepository) : ViewModel() {
    val items: Flow<List<Ammunition>> = repo.observeAll()

    fun save(ammo: Ammunition) {
        viewModelScope.launch { repo.save(ammo) }
    }

    fun delete(ammo: Ammunition) {
        viewModelScope.launch { repo.delete(ammo) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { AmmunitionViewModel(repositories().ammunition) }
        }
    }
}
