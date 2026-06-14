package se.mindphaser.skytte.ui.sessions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.Flow
import se.mindphaser.skytte.data.SessionWithRefs
import se.mindphaser.skytte.data.repo.SessionRepository
import se.mindphaser.skytte.ui.repositories

class SessionListViewModel(repo: SessionRepository) : ViewModel() {
    val sessions: Flow<List<SessionWithRefs>> = repo.observeAll()

    companion object {
        val Factory = viewModelFactory {
            initializer { SessionListViewModel(repositories().sessions) }
        }
    }
}
