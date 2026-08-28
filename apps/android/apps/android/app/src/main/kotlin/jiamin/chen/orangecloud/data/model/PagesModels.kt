package jiamin.chen.orangecloud.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// MARK: - Cloudflare Pages（account 级）：项目 + 部署。对应 iOS PagesModels。
// 读 page.read，写 page.write。Android v1：项目列表 + 详情 + 部署列表 + 重试/回滚部署。
// Direct Upload（手机端上传构建产物）不做，与 iOS 一致地略过。

@Serializable
data class PagesProject(
    val name: String,
    val subdomain: String? = null,
    val domains: List<String>? = null,
    @SerialName("production_branch") val productionBranch: String? = null,
    @SerialName("created_on") val createdOn: String? = null,
    @SerialName("build_config") val buildConfig: PagesBuildConfig? = null,
    @SerialName("latest_deployment") val latestDeployment: PagesDeployment? = null,
    val source: PagesSource? = null,
)

@Serializable
data class PagesBuildConfig(
    @SerialName("build_command") val buildCommand: String? = null,
    @SerialName("destination_dir") val destinationDir: String? = null,
    @SerialName("root_dir") val rootDir: String? = null,
)

@Serializable
data class PagesSource(
    val type: String? = null,
    val config: PagesSourceConfig? = null,
)

@Serializable
data class PagesSourceConfig(
    val owner: String? = null,
    @SerialName("repo_name") val repoName: String? = null,
    @SerialName("production_branch") val productionBranch: String? = null,
) {
    val repoLabel: String?
        get() = repoName?.let { if (owner != null) "$owner/$it" else it }
}

@Serializable
data class PagesDeployment(
    val id: String,
    @SerialName("short_id") val shortId: String? = null,
    @SerialName("project_name") val projectName: String? = null,
    val environment: String? = null, // production | preview
    val url: String? = null,
    @SerialName("created_on") val createdOn: String? = null,
    @SerialName("modified_on") val modifiedOn: String? = null,
    val aliases: List<String>? = null,
    @SerialName("is_skipped") val isSkipped: Boolean? = null,
    @SerialName("latest_stage") val latestStage: PagesStage? = null,
    @SerialName("deployment_trigger") val deploymentTrigger: PagesDeploymentTrigger? = null,
) {
    val statusRaw: String get() = latestStage?.status ?: ""
    val isProduction: Boolean get() = environment == "production"
}

@Serializable
data class PagesStage(
    val name: String? = null,
    val status: String? = null, // success | idle | active | failure | canceled
    @SerialName("started_on") val startedOn: String? = null,
    @SerialName("ended_on") val endedOn: String? = null,
)

@Serializable
data class PagesDeploymentTrigger(
    val type: String? = null,
    val metadata: PagesTriggerMetadata? = null,
)

@Serializable
data class PagesTriggerMetadata(
    val branch: String? = null,
    @SerialName("commit_hash") val commitHash: String? = null,
    @SerialName("commit_message") val commitMessage: String? = null,
) {
    val shortHash: String? get() = commitHash?.take(8)
}

// MARK: - 自定义域名

/** 项目自定义域名。GET /accounts/{id}/pages/projects/{name}/domains */
@Serializable
data class PagesDomain(
    val id: String,
    val name: String,
    val status: String? = null, // initializing | pending | active | deactivated | blocked | error
    @SerialName("zone_tag") val zoneTag: String? = null,
    @SerialName("created_on") val createdOn: String? = null,
    @SerialName("certificate_authority") val certificateAuthority: String? = null,
    @SerialName("validation_data") val validationData: PagesDomainValidationData? = null,
    @SerialName("verification_data") val verificationData: PagesDomainVerificationData? = null,
)

/** 证书验证信息（method == txt 时给出待添加的 TXT 记录）。 */
@Serializable
data class PagesDomainValidationData(
    val status: String? = null,
    val method: String? = null, // http | txt
    @SerialName("txt_name") val txtName: String? = null,
    @SerialName("txt_value") val txtValue: String? = null,
    @SerialName("error_message") val errorMessage: String? = null,
)

/** 域名归属验证信息。 */
@Serializable
data class PagesDomainVerificationData(
    val status: String? = null,
    @SerialName("error_message") val errorMessage: String? = null,
)

/** POST .../domains 请求体。 */
@Serializable
data class PagesDomainAddRequest(val name: String)

/** retry / rollback 的空 POST 体。 */
@Serializable
class PagesEmptyBody

/** POST /pages/projects（建 Direct Upload 空项目）。 */
@Serializable
data class PagesCreateRequest(
    val name: String,
    @SerialName("production_branch") val productionBranch: String,
)

/** PATCH 项目（仅传要改的字段，顶层合并）。 */
@Serializable
data class PagesProjectUpdate(
    @SerialName("build_config") val buildConfig: PagesBuildConfig? = null,
    @SerialName("production_branch") val productionBranch: String? = null,
)

// MARK: - 直接上传部署（Direct Upload）

@Serializable
data class PagesUploadToken(val jwt: String)

@Serializable
data class PagesHashesBody(val hashes: List<String>)

/** pages/assets/upload 单条载荷（key=blake3 资源键，value=base64 内容）。CF 期望 camelCase contentType。 */
@Serializable
data class PagesAssetUpload(
    val key: String,
    val value: String,
    val metadata: PagesAssetMetadata,
    val base64: Boolean,
)

@Serializable
data class PagesAssetMetadata(val contentType: String)

/** 待部署文件（非序列化）。path 以 / 开头（如 /index.html）。 */
class PagesDeployFile(val path: String, val data: ByteArray) {
    val contentType: String get() = pagesMime(path)
}

/** 按扩展名推断 MIME（覆盖常见静态资源，其余 octet-stream）。对齐 iOS PagesMime。 */
fun pagesMime(path: String): String = when (path.substringAfterLast('.', "").lowercase()) {
    "html", "htm" -> "text/html"
    "css" -> "text/css"
    "js", "mjs" -> "application/javascript"
    "json", "map" -> "application/json"
    "webmanifest" -> "application/manifest+json"
    "svg" -> "image/svg+xml"
    "png" -> "image/png"
    "jpg", "jpeg" -> "image/jpeg"
    "gif" -> "image/gif"
    "webp" -> "image/webp"
    "avif" -> "image/avif"
    "ico" -> "image/x-icon"
    "txt" -> "text/plain"
    "md" -> "text/markdown"
    "xml" -> "application/xml"
    "pdf" -> "application/pdf"
    "wasm" -> "application/wasm"
    "woff" -> "font/woff"
    "woff2" -> "font/woff2"
    "ttf" -> "font/ttf"
    "otf" -> "font/otf"
    else -> "application/octet-stream"
}
