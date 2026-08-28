package jiamin.chen.orangecloud.data.repository

import jiamin.chen.orangecloud.core.network.CfApiClient
import jiamin.chen.orangecloud.data.model.Account
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccountRepository @Inject constructor(
    private val api: CfApiClient,
) {
    /** 当前身份有权访问的账号列表。 */
    suspend fun listAccounts(): List<Account> = api.getList<Account>("accounts").items
}
