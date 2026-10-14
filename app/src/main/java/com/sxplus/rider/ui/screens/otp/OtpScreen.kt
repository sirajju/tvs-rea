package com.sxplus.rider.ui.screens.otp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sxplus.rider.ui.components.ErrorBanner
import com.sxplus.rider.ui.components.PrimaryButton
import com.sxplus.rider.ui.components.ScreenScaffold
import com.sxplus.rider.ui.screens.login.AuthViewModel

@Composable
fun OtpScreen(
    vm: AuthViewModel,
    onVerified: () -> Unit,
    onBack: () -> Unit,
) {
    val ui by vm.ui.collectAsStateWithLifecycle()

    androidx.compose.runtime.LaunchedEffect(ui.loggedIn) {
        if (ui.loggedIn) onVerified()
    }

    ScreenScaffold { pad ->
        Column(
            Modifier.fillMaxSize().padding(pad),
            verticalArrangement = Arrangement.Center,
        ) {
            Text("Verify OTP", fontSize = 28.sp, fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onBackground)
            Text("Sent to +91 ${ui.mobile}",
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(32.dp))

            OutlinedTextField(
                value = ui.otp,
                onValueChange = vm::onOtpChange,
                label = { Text("6-digit code") },
                singleLine = true,
                textStyle = MaterialTheme.typography.headlineSmall.copy(
                    textAlign = TextAlign.Center, letterSpacing = 8.sp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.height(72.dp),
            )
            Spacer(Modifier.height(20.dp))
            ErrorBanner(ui.error)
            Spacer(Modifier.height(12.dp))

            PrimaryButton(
                text = "Verify & Continue",
                onClick = vm::verifyOtp,
                enabled = ui.otp.length >= 4,
                loading = ui.loading,
                icon = Icons.Default.LockOpen,
            )
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = { vm.resetToPhone(); onBack() }) {
                Text("Change number", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
