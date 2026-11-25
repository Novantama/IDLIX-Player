package com.idlix.mobile.ui.screens.movies

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idlix.data.model.MovieItem
import com.idlix.data.repository.IdlixRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MoviesUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val movies: List<MovieItem> = emptyList(),
    val selectedGenre: String = "all",
    val selectedSort: String = "popular",
    val currentPage: Int = 1,
    val hasMorePages: Boolean = true,
    val error: String? = null
)

class MoviesViewModel(
    private val repository: IdlixRepository = IdlixRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(MoviesUiState())
    val uiState: StateFlow<MoviesUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    init {
        loadMovies(reset = true)
    }

    fun selectGenre(genre: String) {
        if (_uiState.value.selectedGenre != genre) {
            _uiState.update { it.copy(selectedGenre = genre, currentPage = 1, isLoading = true, error = null) }
            loadMovies(reset = true)
        }
    }

    fun selectSort(sort: String) {
        if (_uiState.value.selectedSort != sort) {
            _uiState.update { it.copy(selectedSort = sort, currentPage = 1, isLoading = true, error = null) }
            loadMovies(reset = true)
        }
    }

    fun refresh() {
        _uiState.update { it.copy(isRefreshing = true, currentPage = 1) }
        loadMovies(reset = true)
    }

    fun loadMore() {
        val state = _uiState.value
        if (state.isLoading || state.isLoadingMore || !state.hasMorePages) return
        _uiState.update { it.copy(isLoadingMore = true) }
        loadMovies(reset = false)
    }

    private fun loadMovies(reset: Boolean) {
        if (reset) {
            loadJob?.cancel()
        }
        loadJob = viewModelScope.launch {
            val state = _uiState.value
            val targetPage = if (reset) 1 else state.currentPage + 1
            val genreParam = if (state.selectedGenre == "all") null else state.selectedGenre
            val sortParam = if (state.selectedSort == "popular") null else state.selectedSort

            try {
                val response = repository.getMovies(page = targetPage, sort = sortParam, genre = genreParam)
                val newItems = response.data
                _uiState.update { latest ->
                    val updatedList = if (reset) {
                        newItems.distinctBy { it.id }
                    } else {
                        (latest.movies + newItems).distinctBy { it.id }
                    }
                    latest.copy(
                        isLoading = false,
                        isRefreshing = false,
                        isLoadingMore = false,
                        movies = updatedList,
                        currentPage = targetPage,
                        hasMorePages = newItems.isNotEmpty() && targetPage < (response.pagination?.totalPages ?: targetPage),
                        error = null
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { latest ->
                    latest.copy(
                        isLoading = false,
                        isRefreshing = false,
                        isLoadingMore = false,
                        error = e.localizedMessage ?: "Failed to load movies"
                    )
                }
            }
        }
    }
}
