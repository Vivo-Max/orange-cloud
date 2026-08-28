-- 用户自助重置设备绑定的频率限制：记录每枚码上次自助重置时间（30 天最多 1 次）。
-- 0005 已上线（生产有真实数据），故新增列用 ALTER，不改 0005。
ALTER TABLE codes ADD COLUMN last_self_reset_at INTEGER;
