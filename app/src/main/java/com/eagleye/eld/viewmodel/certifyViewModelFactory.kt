package com.eagleye.eld.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
 import com.eagleye.eld.repository.CertifyRepository

class certifyViewModelFactory(private val certifyRepository: CertifyRepository): ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return CertifyViewModel(certifyRepository) as T
    }
}