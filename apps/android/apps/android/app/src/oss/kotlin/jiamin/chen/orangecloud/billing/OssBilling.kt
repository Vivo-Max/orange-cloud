package jiamin.chen.orangecloud.billing

import android.app.Activity
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import jiamin.chen.orangecloud.core.purchase.BillingGateway
import jiamin.chen.orangecloud.core.purchase.RedeemOutcome
import javax.inject.Inject
import javax.inject.Singleton

/** oss 风味：无 Billing 依赖，isPro 由 EntitlementStore 恒真，网关为空操作。 */
@Singleton
class OssBillingGateway @Inject constructor() : BillingGateway {
    override fun connect() = Unit
    override fun launchPurchase(activity: Activity, planId: String) = Unit
    override suspend fun redeem(code: String) = RedeemOutcome.NOT_SUPPORTED
    override suspend fun deactivate() = false
}

@Module
@InstallIn(SingletonComponent::class)
abstract class BillingModule {
    @Binds
    abstract fun bindBillingGateway(impl: OssBillingGateway): BillingGateway
}
