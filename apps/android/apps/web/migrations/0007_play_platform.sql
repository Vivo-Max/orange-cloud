-- Google Play 入账：把既有 Apple 三表账本升级成「双平台统一账本」。
--
-- 不新建 play_* 平行表——两边的业务字段几乎一一对应，统一后看板的 KPI /
-- 图表 / 交易流水无需 UNION 即可同时覆盖两个平台，只多一个 platform 维度：
--   notifications.notification_uuid  <- Pub/Sub messageId（重投递同 id，幂等口径不变）
--   subscriptions.original_transaction_id <- purchaseToken（订阅续期内保持不变）
--   transactions.transaction_id      <- orderId（GPA.xxxx-...，续订每期一个）
-- 历史行全部是 Apple 的，故默认值 'apple'。
--
-- 注意：storefront 列在 Apple 侧是 ISO 3166-1 alpha-3（USA），Play 侧写的是
-- Orders API 的 buyerAddress.regionCode / 订阅的 regionCode，为 alpha-2（US）。
-- 两种写法共存，展示层原样显示，不做归一（无业务判断依赖该列）。

ALTER TABLE notifications ADD COLUMN platform TEXT NOT NULL DEFAULT 'apple';
ALTER TABLE subscriptions ADD COLUMN platform TEXT NOT NULL DEFAULT 'apple';
ALTER TABLE transactions  ADD COLUMN platform TEXT NOT NULL DEFAULT 'apple';

-- 订阅升降级 / 重新订阅时 Play 会换发新 purchaseToken，并在新购买上带
-- linkedPurchaseToken 指回旧的；存下来以便把旧行标记为已被替换。
ALTER TABLE subscriptions ADD COLUMN linked_token TEXT;

-- Play 的 Orders API 直接给出「开发者到手金额（买家币种）」，比 Apple 侧只能
-- 拿到售价强；Apple 行留 NULL。price_millis 仍是买家实付总额（含税），口径与
-- Apple 的 transaction.price 对齐，营收卡片继续用它。
ALTER TABLE transactions ADD COLUMN dev_revenue_millis INTEGER;

CREATE INDEX IF NOT EXISTS idx_notifications_platform ON notifications (platform);
CREATE INDEX IF NOT EXISTS idx_subscriptions_platform ON subscriptions (platform);
CREATE INDEX IF NOT EXISTS idx_transactions_platform ON transactions (platform);
