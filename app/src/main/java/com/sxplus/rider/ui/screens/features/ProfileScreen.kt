package com.sxplus.rider.ui.screens.features

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import com.sxplus.rider.data.local.TokenStore
import com.sxplus.rider.ui.components.FeatureScaffold
import com.sxplus.rider.ui.components.InfoRow
import com.sxplus.rider.ui.components.SectionCard
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(private val tokens: TokenStore) : ViewModel() {
    val name: String get() = tokens.userName ?: "Rider"
    val mobile: String get() = tokens.mobile ?: "—"
    val userId: String get() = tokens.userId ?: "—"
    val loggedIn: Boolean get() = tokens.isLoggedIn
}

@Composable
fun ProfileScreen(onBack: () -> Unit, vm: ProfileViewModel = hiltViewModel()) {
    FeatureScaffold(title = "Profile", onBack = onBack) { pad ->
        Column(
            Modifier.padding(pad).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Spacer(Modifier.height(8.dp))
            Box(
                Modifier.size(84.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(44.dp))
            }
            Text(vm.name, fontWeight = FontWeight.Black,
                style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onBackground)
            SectionCard("Account") {
                InfoRow(Icons.Default.Person, "Name", vm.name)
                InfoRow(Icons.Default.Phone, "Mobile", vm.mobile)
                InfoRow(Icons.Default.Badge, "User ID", vm.userId)
                InfoRow(Icons.Default.TwoWheeler, "Signed in", if (vm.loggedIn) "Yes" else "Direct BLE mode")
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
