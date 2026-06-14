package se.mindphaser.skytte.ui

import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.CreationExtras
import se.mindphaser.skytte.SkytteApp
import se.mindphaser.skytte.data.repo.Repositories

fun CreationExtras.repositories(): Repositories =
    (this[APPLICATION_KEY] as SkytteApp).currentRepositories
        ?: error("Repositories unavailable: no signed-in user")
