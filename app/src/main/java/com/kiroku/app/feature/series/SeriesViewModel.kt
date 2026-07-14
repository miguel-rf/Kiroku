package com.kiroku.app.feature.series

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kiroku.app.core.common.UiError
import com.kiroku.app.core.common.toUiError
import com.kiroku.app.core.network.NetworkMonitor
import com.kiroku.app.data.repository.CatalogueRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SeriesUiState(
    val seriesId: Long,
    val content: SeriesDetailsUiModel? = null,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isOffline: Boolean = false,
    val error: UiError? = null,
)

class SeriesViewModel(
    private val seriesId: Long,
    private val repository: CatalogueRepository,
    networkMonitor: NetworkMonitor,
) : ViewModel() {
    private data class RefreshState(
        val isRefreshing: Boolean = false,
        val error: UiError? = null,
    )

    private val refreshState = MutableStateFlow(RefreshState(isRefreshing = true))
    private var refreshJob: Job? = null

    private val content =
        repository
            .observeSeries(seriesId)
            .map { details -> details?.toUiModel() }
            .catch { failure ->
                refreshState.value = RefreshState(error = failure.toUiError())
                emit(null)
            }

    private val isOnline =
        networkMonitor.isOnline
            .onStart { emit(true) }
            .catch { emit(true) }

    val uiState =
        combine(content, refreshState, isOnline) { content, refresh, online ->
            SeriesUiState(
                seriesId = seriesId,
                content = content,
                isLoading = content == null && refresh.error == null,
                isRefreshing = refresh.isRefreshing,
                isOffline = !online,
                error = refresh.error,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = SeriesUiState(seriesId = seriesId),
        )

    init {
        refresh()
    }

    fun refresh() {
        if (refreshJob?.isActive == true) return
        refreshState.update { RefreshState(isRefreshing = true) }
        refreshJob =
            viewModelScope.launch {
                refreshState.value =
                    try {
                        repository.refreshSeries(seriesId)
                        RefreshState()
                    } catch (failure: Throwable) {
                        RefreshState(error = failure.toUiError())
                    }
            }
    }

    fun dismissError() {
        refreshState.update { it.copy(error = null) }
    }

    class Factory(
        private val seriesId: Long,
        private val repository: CatalogueRepository,
        private val networkMonitor: NetworkMonitor,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(SeriesViewModel::class.java)) {
                "Unsupported ViewModel type: " + modelClass.name
            }
            return SeriesViewModel(seriesId, repository, networkMonitor) as T
        }
    }
}
