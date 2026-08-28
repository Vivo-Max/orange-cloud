package jiamin.chen.orangecloud.data.repository

import jiamin.chen.orangecloud.core.network.CfApiClient
import jiamin.chen.orangecloud.data.model.TurnstileRotateRequest
import jiamin.chen.orangecloud.data.model.TurnstileWidget
import jiamin.chen.orangecloud.data.model.TurnstileWidgetInput
import javax.inject.Inject
import javax.inject.Singleton

/** Turnstile 人机验证组件管理（challenge-widgets.read / .write）。对应 iOS TurnstileService。 */
@Singleton
class TurnstileRepository @Inject constructor(
    private val api: CfApiClient,
) {
    /** 账号下全部 widget（页码分页拉全）。 */
    suspend fun listWidgets(accountId: String): List<TurnstileWidget> {
        val widgets = mutableListOf<TurnstileWidget>()
        var page = 1
        while (true) {
            val paged = api.getList<TurnstileWidget>(
                "accounts/$accountId/challenges/widgets",
                query = listOf("page" to page.toString(), "per_page" to "50"),
            )
            widgets += paged.items
            val totalPages = paged.info?.totalPages ?: 1
            if (page >= totalPages) break
            page++
        }
        return widgets
    }

    /** 单个 widget 详情（响应含 secret）。 */
    suspend fun widget(accountId: String, sitekey: String): TurnstileWidget =
        api.get("accounts/$accountId/challenges/widgets/$sitekey")

    /** 新建 widget（响应含 sitekey + secret）。 */
    suspend fun createWidget(accountId: String, input: TurnstileWidgetInput): TurnstileWidget =
        api.post("accounts/$accountId/challenges/widgets", input)

    /** 更新 widget（PUT，name / mode / domains 必填）。 */
    suspend fun updateWidget(accountId: String, sitekey: String, input: TurnstileWidgetInput): TurnstileWidget =
        api.put("accounts/$accountId/challenges/widgets/$sitekey", input)

    /** 删除 widget。 */
    suspend fun deleteWidget(accountId: String, sitekey: String) =
        api.delete("accounts/$accountId/challenges/widgets/$sitekey")

    /** 轮换服务端密钥。immediately = false 时旧密钥保留 2 小时宽限期。 */
    suspend fun rotateSecret(accountId: String, sitekey: String, immediately: Boolean): TurnstileWidget =
        api.post("accounts/$accountId/challenges/widgets/$sitekey/rotate_secret", TurnstileRotateRequest(immediately))
}
