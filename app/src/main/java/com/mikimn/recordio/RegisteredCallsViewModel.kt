package com.mikimn.recordio

import androidx.lifecycle.*
import kotlinx.coroutines.launch

class RegisteredCallsViewModel(
    private val repository: RegisteredCallsRepository
) : ViewModel() {

    val calls: LiveData<List<RegisteredCall>> = repository.calls.asLiveData()

    suspend fun findById(id: Int) = repository.findById(id)

    fun delete(callRecording: RegisteredCall) = viewModelScope.launch {
        repository.delete(callRecording)
    }
}

class RegisteredCallsViewModelFactory(
    private val repository: RegisteredCallsRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RegisteredCallsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RegisteredCallsViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}