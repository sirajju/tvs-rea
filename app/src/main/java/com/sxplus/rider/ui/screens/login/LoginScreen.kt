package com.sxplus.rider.ui.screens.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sxplus.rider.BuildConfig
import com.sxplus.rider.ui.components.ErrorBanner
import com.sxplus.rider.ui.components.PrimaryButton
import com.sxplus.rider.ui.components.ScreenScaffold

@Composable
fun LoginScreen(
    vm: AuthViewModel = hiltViewModel(),
    onOtpSent: () -> Unit,
    onConnectDirectly: () -> Unit = {},
) {
    val ui by vm.ui.collectAsStateWithLifecycle()

    // Navigate when OTP is dispatched
    androidx.compose.runtime.LaunchedEffect(ui.otpSent) {
        if (ui.otpSent) onOtpSent()
    }

    ScreenScaffold { pad ->
        Column(
            Modifier.fillMaxSize().padding(pad),
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                Icons.Default.TwoWheeler, null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(64.dp),
            )
            Spacer(Modifier.height(16.dp))
            Text("SmartXonnect+", fontSize = 32.sp, fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onBackground)
            Text("Keyless. Connected. Yours.",
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(40.dp))

            OutlinedTextField(
                value = ui.mobile,
                onValueChange = vm::onMobileChange,
                label = { Text("Mobile number") },
                prefix = { Text("+91  ") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.height(64.dp),
            )
            Spacer(Modifier.height(20.dp))

            ErrorBanner(ui.error)
            Spacer(Modifier.height(12.dp))

            PrimaryButton(
                text = "Send OTP",
                onClick = vm::requestOtp,
                enabled = ui.mobile.length == 10,
                loading = ui.loading,
                icon = Icons.Default.ArrowForward,
            )

            Spacer(Modifier.height(8.dp))
            androidx.compose.material3.TextButton(
                onClick = onConnectDirectly,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Default.Bluetooth, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
                Text("Connect to my bike directly (no sign-in)")
            }

            if (BuildConfig.ALLOW_MOCK) {
                Spacer(Modifier.height(24.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = ui.mockEnabled, onCheckedChange = vm::toggleMock)
                    Spacer(Modifier.size(10.dp))
                    Text("Dev mock mode (offline, any number/OTP)",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
