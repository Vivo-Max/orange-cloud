-- IndexNow 推送台账：记住每个 URL 上次是以哪个「内容版本」推给搜索引擎的。
-- signature 取自 src/lib/site/urls.ts 的 updated（YYYY-MM-DD）：
-- 清单里的版本与这里不一致（或压根没有这一行）才需要重新推，避免把没变的页面反复提交。
CREATE TABLE IF NOT EXISTS indexnow_urls (
	url          TEXT    NOT NULL PRIMARY KEY,  -- 绝对 URL，如 https://oss.omail.us.kg/guides/…
	signature    TEXT    NOT NULL,              -- 推送时的内容版本 'YYYY-MM-DD'
	submitted_at INTEGER NOT NULL               -- 推送时刻, ms epoch
);
