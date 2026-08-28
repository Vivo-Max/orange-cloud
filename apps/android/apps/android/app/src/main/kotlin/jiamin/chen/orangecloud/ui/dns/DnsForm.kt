package jiamin.chen.orangecloud.ui.dns

/** DNS 编辑表单的领域选项与规则（与 iOS DNSRecordFormView 对齐）。 */
object DnsForm {
    /** 支持编辑的记录类型。 */
    val recordTypes = listOf("A", "AAAA", "CNAME", "TXT", "MX", "NS")

    /** TTL 秒值；1 = 自动。标签在 UI 层按值映射 strings.xml。 */
    val ttlValues = listOf(1, 60, 300, 1800, 3600, 86400)

    /** 只有 A / AAAA / CNAME 支持 Cloudflare 代理。 */
    fun supportsProxy(type: String): Boolean = type == "A" || type == "AAAA" || type == "CNAME"

    /** 仅 MX 需要优先级。 */
    fun needsPriority(type: String): Boolean = type == "MX"
}
