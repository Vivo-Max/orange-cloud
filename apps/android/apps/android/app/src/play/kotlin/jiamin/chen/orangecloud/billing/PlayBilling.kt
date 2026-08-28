package jiamin.chen.orangecloud.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import jiamin.chen.orangecloud.core.purchase.BillingGateway
import jiamin.chen.orangecloud.core.purchase.EntitlementStore
import jiamin.chen.orangecloud.core.purchase.RedeemOutcome
import javax.inject.Inject
import javax.inject.Singleton

/**
 * play 风味：Play Billing v9。连接后查询既有购买回填 isPro；isPro = 活跃订阅 ∨ 持有买断。
 * 商品 ID：jiamin.chen.orange_cloud.pro.monthly/.yearly（SUBS）、.lifetime（INAPP）。
 */
@Singleton
class PlayBillingGateway @Inject constructor(
    @ApplicationContext private val context: Context,
    private val entitlementStore: EntitlementStore,
) : BillingGateway, PurchasesUpdatedListener {

    private val products = mapOf(
        BillingGateway.PLAN_MONTHLY to ("jiamin.chen.orange_cloud.pro.monthly" to BillingClient.ProductType.SUBS),
        BillingGateway.PLAN_YEARLY to ("jiamin.chen.orange_cloud.pro.yearly" to BillingClient.ProductType.SUBS),
        BillingGateway.PLAN_LIFETIME to ("jiamin.chen.orange_cloud.pro.lifetime" to BillingClient.ProductType.INAPP),
    )

    private val billingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .build()

    override fun connect() {
        if (billingClient.isReady) {
            queryAll()
            return
        }
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) queryAll()
            }

            override fun onBillingServiceDisconnected() {}
        })
    }

    private fun queryAll() {
        queryPurchases(BillingClient.ProductType.SUBS)
        queryPurchases(BillingClient.ProductType.INAPP)
    }

    private fun queryPurchases(type: String) {
        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(type).build(),
        ) { _, purchases ->
            handlePurchases(purchases)
        }
    }

    private fun handlePurchases(purchases: List<Purchase>) {
        val active = purchases.any { it.purchaseState == Purchase.PurchaseState.PURCHASED }
        if (active) {
            entitlementStore.setPro(true)
            purchases.forEach(::acknowledge)
        }
    }

    private fun acknowledge(p: Purchase) {
        if (p.purchaseState == Purchase.PurchaseState.PURCHASED && !p.isAcknowledged) {
            billingClient.acknowledgePurchase(
                AcknowledgePurchaseParams.newBuilder().setPurchaseToken(p.purchaseToken).build(),
            ) {}
        }
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        if (result.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            handlePurchases(purchases)
        }
    }

    override fun launchPurchase(activity: Activity, planId: String) {
        val (productId, type) = products[planId] ?: return
        val query = QueryProductDetailsParams.newBuilder().setProductList(
            listOf(
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(productId)
                    .setProductType(type)
                    .build(),
            ),
        ).build()
        billingClient.queryProductDetailsAsync(query) { _, result ->
            val pd = result.productDetailsList.firstOrNull() ?: return@queryProductDetailsAsync
            val offerToken = pd.subscriptionOfferDetails?.firstOrNull()?.offerToken
            val paramsBuilder = BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(pd)
            if (offerToken != null) paramsBuilder.setOfferToken(offerToken)
            billingClient.launchBillingFlow(
                activity,
                BillingFlowParams.newBuilder().setProductDetailsParamsList(listOf(paramsBuilder.build())).build(),
            )
        }
    }

    override suspend fun redeem(code: String) = RedeemOutcome.NOT_SUPPORTED
    override suspend fun deactivate() = false
}

@Module
@InstallIn(SingletonComponent::class)
abstract class BillingModule {
    @Binds
    abstract fun bindBillingGateway(impl: PlayBillingGateway): BillingGateway
}
