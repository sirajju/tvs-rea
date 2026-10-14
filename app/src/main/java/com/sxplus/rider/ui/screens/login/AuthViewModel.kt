package com.sxplus.rider.ui.screens.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sxplus.rider.data.repo.RuntimeFlags
import com.sxplus.rider.data.repo.TvsRepository
import com.sxplus.rider.domain.Outcome
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val mobile: String = "",
    val otp: String = "",
    val loading: Boolean = false,
    val otpSent: Boolean = false,
    val error: String? = null,
    val loggedIn: Boolean = false,
    val mockEnabled: Boolean = false,
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repo: TvsRepository,
    private val flags: RuntimeFlags,
) : ViewModel() {

    private val _ui = MutableStateFlow(AuthUiState(mockEnabled = flags.mockEnabled))
    val ui: StateFlow<AuthUiState> = _ui.asStateFlow()

    fun onMobileChange(v: String) {
        _ui.update { it.copy(mobile = v.filter { c -> c.isDigit() }.take(10), error = null) }
    }

    fun onOtpChange(v: String) {
        _ui.update { it.copy(otp = v.filter { c -> c.isDigit() }.take(6), error = null) }
    }

    fun toggleMock(enabled: Boolean) {
        flags.mockEnabled = enabled
        _ui.update { it.copy(mockEnabled = flags.mockEnabled) }
    }

    fun requestOtp() {
        val mobile = _ui.value.mobile
        if (mobile.length != 10) {
            _ui.update { it.copy(error = "Enter a valid 10-digit number") }
            return
        }
        _ui.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            when (val r = repo.requestOtp(mobile)) {
                is Outcome.Ok -> _ui.update { it.copy(loading = false, otpSent = true) }
                is Outcome.Err -> _ui.update { it.copy(loading = false, error = r.message) }
            }
        }
    }

    fun verifyOtp() {
        val s = _ui.value
        if (s.otp.length < 4) {
            _ui.update { it.copy(error = "Enter the OTP") }
            return
        }
        _ui.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            when (val r = repo.verifyOtp(s.mobile, s.otp)) {
                is Outcome.Ok -> _ui.update { it.copy(loading = false, loggedIn = true) }
                is Outcome.Err -> _ui.update { it.copy(loading = false, error = r.message) }
            }
        }
    }

    fun resetToPhone() = _ui.update { it.copy(otpSent = false, otp = "", error = null) }
}
