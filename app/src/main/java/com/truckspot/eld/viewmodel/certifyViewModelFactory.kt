package com.truckspot.eld.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
 import com.truckspot.eld.repository.CertifyRepository

class certifyViewModelFactory(private val certifyRepository: CertifyRepository): ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return CertifyViewModel(certifyRepository) as T
    }
}