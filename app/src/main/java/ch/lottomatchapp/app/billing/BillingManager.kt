package ch.lottomatchapp.app.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import ch.lottomatchapp.app.data.PremiumRepository
import ch.lottomatchapp.app.util.Analytics
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

private const val TAG = "BillingManager"

/** Muss exakt mit der SKU im Google Play Console übereinstimmen. */
const val PREMIUM_SKU = "lottomatch_premium"

sealed class BillingEvent {
    data object PurchaseSuccess : BillingEvent()
    data object PurchaseCancelled : BillingEvent()
    data class PurchaseError(val message: String) : BillingEvent()
    data object NotAvailable : BillingEvent()
}

/**
 * Wrapper um den Play Billing Client.
 * Lifecycle: connect() bei Activity-Start, disconnect() bei Destroy.
 */
class BillingManager(
    context: Context,
    private val premiumRepo: PremiumRepository
) {
    private val appContext = context.applicationContext

    private val _events = MutableSharedFlow<BillingEvent>(extraBufferCapacity = 4)
    val events: SharedFlow<BillingEvent> = _events.asSharedFlow()

    private val _productDetails = MutableStateFlow<ProductDetails?>(null)
    val productDetails: StateFlow<ProductDetails?> = _productDetails.asStateFlow()

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    private val purchasesUpdatedListener = PurchasesUpdatedListener { result, purchases ->
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                purchases?.forEach { handlePurchase(it) }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                _events.tryEmit(BillingEvent.PurchaseCancelled)
            }
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                // Schon gekauft — Status nochmals abfragen.
                queryPurchases()
            }
            else -> {
                _events.tryEmit(BillingEvent.PurchaseError(result.debugMessage))
            }
        }
    }

    private val billingClient = BillingClient.newBuilder(appContext)
        .setListener(purchasesUpdatedListener)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .build()
        )
        // Billing 8+: Client verbindet sich nach Abbruch selbst neu.
        .enableAutoServiceReconnection()
        .build()

    fun connect() {
        if (billingClient.isReady) {
            _isReady.value = true
            refresh()
            return
        }
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    _isReady.value = true
                    refresh()
                } else {
                    Log.w(TAG, "Billing setup failed: ${result.debugMessage}")
                    _events.tryEmit(BillingEvent.NotAvailable)
                }
            }

            override fun onBillingServiceDisconnected() {
                _isReady.value = false
                Log.w(TAG, "Billing service disconnected")
            }
        })
    }

    fun disconnect() {
        if (billingClient.isReady) billingClient.endConnection()
        _isReady.value = false
    }

    /** Product-Details und bisherige Käufe nachfragen. */
    fun refresh() {
        queryProductDetails()
        queryPurchases()
    }

    private fun queryProductDetails() {
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(PREMIUM_SKU)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build()
                )
            )
            .build()

        billingClient.queryProductDetailsAsync(params) { result, detailsResult ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                // Billing 8+: Liste steckt im QueryProductDetailsResult.
                _productDetails.value = detailsResult.productDetailsList.firstOrNull()
                if (detailsResult.unfetchedProductList.isNotEmpty()) {
                    Log.w(TAG, "Unfetched products: ${detailsResult.unfetchedProductList}")
                }
            } else {
                Log.w(TAG, "queryProductDetails failed: ${result.debugMessage}")
            }
        }
    }

    fun queryPurchases() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()

        billingClient.queryPurchasesAsync(params) { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                val hasPremium = purchases.any { purchase ->
                    purchase.products.contains(PREMIUM_SKU) &&
                        purchase.purchaseState == Purchase.PurchaseState.PURCHASED
                }
                premiumRepo.setPremium(hasPremium)
                purchases.forEach { handlePurchase(it) }
            }
        }
    }

    /** Startet den Kauf-Flow. Aufzurufen aus einer Activity. */
    fun launchPurchase(activity: Activity): Boolean {
        val details = _productDetails.value
        if (details == null) {
            _events.tryEmit(BillingEvent.NotAvailable)
            return false
        }
        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(details)
                        .build()
                )
            )
            .build()
        val result = billingClient.launchBillingFlow(activity, flowParams)
        return result.responseCode == BillingClient.BillingResponseCode.OK
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) return
        if (!purchase.products.contains(PREMIUM_SKU)) return

        val wasAlreadyPremium = premiumRepo.isPremium.value
        premiumRepo.setPremium(true)
        if (!wasAlreadyPremium) {
            Analytics.premiumPurchased()
        }
        _events.tryEmit(BillingEvent.PurchaseSuccess)

        if (!purchase.isAcknowledged) {
            val params = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()
            billingClient.acknowledgePurchase(params) { result ->
                if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                    Log.w(TAG, "acknowledgePurchase failed: ${result.debugMessage}")
                }
            }
        }
    }

    fun getFormattedPrice(): String? =
        _productDetails.value?.oneTimePurchaseOfferDetails?.formattedPrice
}
