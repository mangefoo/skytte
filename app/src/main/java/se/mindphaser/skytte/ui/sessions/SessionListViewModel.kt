package se.mindphaser.skytte.ui.sessions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.Flow
import se.mindphaser.skytte.data.SessionDao
import se.mindphaser.skytte.data.SessionWithRefs
import se.mindphaser.skytte.ui.database

class SessionListViewModel(private val dao: SessionDao) : ViewModel() {
    val sessions: Flow<List<SessionWithRefs>> = dao.observeAll()

    companion object {
        val Factory = viewModelFactory {
            initializer { SessionListViewModel(database().sessionDao()) }
        }
    }
}
