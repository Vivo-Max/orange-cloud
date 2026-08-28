-- 各平台「上架状态机」。官网更新历史据此门控与标注：
--   live_version    最新已上架版本（单调前进），<= 它的条目正常展示。
--   pending_version 在审 / 待发布版本（可空）；非空即在官网对该版本标「审核中」。
--   pending_state   'in_review' | 'pending_release'。
-- iOS 由 ASC webhook（App 版本状态变更，全状态机）驱动；Android 由 fastlane 回调驱动。
CREATE TABLE IF NOT EXISTS release_state (
	track           TEXT PRIMARY KEY,    -- 'ios' | 'android'
	live_version    TEXT,
	pending_version TEXT,
	pending_state   TEXT,
	updated_at      INTEGER NOT NULL
);
