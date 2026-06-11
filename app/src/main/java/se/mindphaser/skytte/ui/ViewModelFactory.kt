package se.mindphaser.skytte.ui

import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.CreationExtras
import se.mindphaser.skytte.SkytteApp
import se.mindphaser.skytte.data.AppDatabase

fun CreationExtras.database(): AppDatabase =
    (this[APPLICATION_KEY] as SkytteApp).database
