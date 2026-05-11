@file:OptIn(ExperimentalMaterial3Api::class)

package ch.lottomatchapp.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ch.lottomatchapp.app.R
import ch.lottomatchapp.app.billing.BillingEvent
import ch.lottomatchapp.app.billing.LocalBillingManager
import ch.lottomatchapp.app.util.findActivity

@Composable
fun PaywallSheet(onDismiss: () -> Unit) {
    val billing = LocalBillingManager.current
    if (billing == null) {
        // Defensive — sollte in Production nie passieren, weil MainActivity
        // den Manager via CompositionLocal bereitstellt.
        LaunchedEffect(Unit) { onDismiss() }
        return
    }

    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val snackbarHostState = remember { SnackbarHostState() }

    val productDetails by billing.productDetails.collectAsState()
    val isReady by billing.isReady.collectAsState()
    val price = productDetails?.oneTimePurchaseOfferDetails?.formattedPrice

    LaunchedEffect(billing) {
        billing.events.collect { event ->
            when (event) {
                is BillingEvent.PurchaseSuccess -> {
                    snackbarHostState.showSnackbar(
                        context.getString(R.string.paywall_purchase_success)
                    )
                    onDismiss()
                }
                is BillingEvent.PurchaseCancelled -> {
                    snackbarHostState.showSnackbar(
                        context.getString(R.string.paywall_purchase_cancelled)
                    )
                }
                is BillingEvent.PurchaseError -> {
                    snackbarHostState.showSnackbar(
                        context.getString(R.string.paywall_purchase_error, event.message)
                    )
                }
                is BillingEvent.NotAvailable -> {
                    snackbarHostState.showSnackbar(
                        context.getString(R.string.paywall_unavailable)
                    )
                }
            }
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.WorkspacePremium,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.paywall_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.paywall_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))

            FeatureBullet(stringResource(R.string.paywall_feature_cards))
            FeatureBullet(stringResource(R.string.paywall_feature_heatmap))
            FeatureBullet(stringResource(R.string.paywall_feature_support))

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    val act = activity ?: return@Button
                    billing.launchPurchase(act)
                },
                enabled = isReady && price != null && activity != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = MaterialTheme.shapes.large
            ) {
                Text(
                    text = if (price != null) {
                        stringResource(R.string.paywall_buy_button, price)
                    } else {
                        stringResource(R.string.paywall_buy_loading)
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            TextButton(
                onClick = { billing.queryPurchases() },
                enabled = isReady
            ) {
                Text(stringResource(R.string.paywall_restore))
            }

            Spacer(Modifier.height(8.dp))
        }

        SnackbarHost(snackbarHostState)
    }
}

@Composable
private fun FeatureBullet(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
    }
}
