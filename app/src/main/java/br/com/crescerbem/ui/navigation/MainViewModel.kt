package br.com.crescerbem.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.crescerbem.data.model.Baby
import br.com.crescerbem.data.repo.BabyRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class MainState(
    val loading: Boolean = true,
    val babies: List<Baby> = emptyList(),
    val active: Baby? = null,
)

class MainViewModel(private val babyRepository: BabyRepository) : ViewModel() {
    val state: StateFlow<MainState> = combine(babyRepository.babies, babyRepository.activeBaby) { list, active ->
        MainState(loading = false, babies = list, active = active)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MainState())

    fun select(id: Long) = babyRepository.setActive(id)
}
