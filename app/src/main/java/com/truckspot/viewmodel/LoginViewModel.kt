

package com.truckspot.viewmodel

import android.text.TextUtils
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.truckspot.models.LoginResponse
import com.truckspot.repository.LoginRespository
import com.truckspot.request.LoginRequest
import com.truckspot.utils.Helper
import com.truckspot.utils.NetworkResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.net.CacheRequest
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(var loginRespository: LoginRespository):ViewModel() {
    val loginResponseLiveData: LiveData<NetworkResult<LoginResponse>>
        get() = loginRespository.loginResponseLiveData


    fun loginUser (loginRequest: LoginRequest){
         viewModelScope.launch {
            loginRespository.loginUser(loginRequest)
         }
    }

    fun validateCredentials(emailAddress: String, userName: String, password: String,
                            isLogin: Boolean) : Pair<Boolean, String> {

        var result = Pair(true, "")
        if(TextUtils.isEmpty(emailAddress) || (!isLogin && TextUtils.isEmpty(userName)) || TextUtils.isEmpty(password)){
            result = Pair(false, "Please provide the credentials")
        }
//        else if(!Helper.isValidEmail(emailAddress)){
//            result = Pair(false, "Email is invalid")
//        }
        else if(!TextUtils.isEmpty(password) && password.length <= 2){
            result = Pair(false, "Password length should be greater than 3")
        }
        return result
    }
}