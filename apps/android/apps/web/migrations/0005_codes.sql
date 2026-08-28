-- 激活码渠道（非 Play 中国大陆 Android）—— 买断激活码 + 设备绑定。
-- 与 Apple/Play 的 IAP 账本（notifications/subscriptions/transactions）并存、不交叉。
-- 时间戳一律毫秒 epoch（与既有三表口径一致）。

-- 激活码：每笔 Stripe 付款生成一枚（也可后台手动生成赠码）。
CREATE TABLE IF NOT EXISTS codes (
	code                  TEXT PRIMARY KEY,    -- 规范化核心串（10 位 Crockford32，无 OC- 前缀/分隔），展示时拼 OC-XXXXX-XXXXX
	product               TEXT NOT NULL DEFAULT 'pro.lifetime',
	status                TEXT NOT NULL DEFAULT 'active', -- active | revoked
	source                TEXT NOT NULL DEFAULT 'stripe', -- stripe | manual | gift
	stripe_session_id     TEXT UNIQUE,         -- 生成此码的 Checkout Session（幂等：一会话一码）
	stripe_payment_intent TEXT,                -- 退款回查用
	amount_total          INTEGER,             -- 实付，货币最小单位（¥29.90 -> 2990）
	currency              TEXT,                -- ISO 4217
	buyer_email           TEXT,                -- 找回用
	note                  TEXT,                -- 后台备注（手动码用途等）
	created_at            INTEGER NOT NULL,
	revoked_at            INTEGER,
	revoke_reason         TEXT,                -- refund | manual | ...
	-- 退款申请（官网自助申请 -> 后台审批 -> Stripe 退款）。30 天政策见 lib/codes/refund.ts。
	refund_status         TEXT NOT NULL DEFAULT 'none', -- none | requested | approved | rejected
	refund_requested_at   INTEGER,
	refund_reason         TEXT
);

CREATE INDEX IF NOT EXISTS idx_codes_status ON codes (status);
CREATE INDEX IF NOT EXISTS idx_codes_refund ON codes (refund_status);
CREATE INDEX IF NOT EXISTS idx_codes_pi ON codes (stripe_payment_intent);
CREATE INDEX IF NOT EXISTS idx_codes_email ON codes (buyer_email);
CREATE INDEX IF NOT EXISTS idx_codes_created ON codes (created_at);

-- 设备绑定：一码可激活有限台设备（默认 3），公开外泄的码也用不爆。
CREATE TABLE IF NOT EXISTS code_activations (
	code         TEXT NOT NULL,
	install_id   TEXT NOT NULL,       -- App 安装实例 ID（非账号、非设备硬件 ID）
	activated_at INTEGER NOT NULL,
	last_seen_at INTEGER NOT NULL,    -- 最近一次在线校验心跳
	PRIMARY KEY (code, install_id)
);

CREATE INDEX IF NOT EXISTS idx_activations_code ON code_activations (code);
