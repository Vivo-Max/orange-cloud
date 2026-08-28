-- App Store 榜单排名的每日快照（定时任务 captureRanks 写入）。
-- 每天 UTC 08:00 抓 Apple 目录接口，把各地区当日榜单名次落一行。
--   position 为榜单名次；若该地区已上架但当时未上榜则为 NULL。
--   不可用地区（接口 404，如 cn 当前未上架）不写入。
-- 主键 (captured_date, country) 保证一地区一天一行；同日重跑 upsert 覆盖。
CREATE TABLE IF NOT EXISTS app_store_ranks (
	captured_date TEXT    NOT NULL,   -- 'YYYY-MM-DD'（UTC 抓取日）
	country       TEXT    NOT NULL,   -- App Store 商店区码：us / tw / my / ca / ng / tr / cn …
	position      INTEGER,            -- 榜单名次；上架但未上榜为 NULL
	chart         TEXT,               -- 'top-free' | 'top-paid' …
	genre         INTEGER,            -- 类目 id，如 6026（Developer Tools）
	genre_name    TEXT,               -- 类目名，如 'Developer Tools'
	captured_at   INTEGER NOT NULL,   -- 抓取时刻, ms epoch
	PRIMARY KEY (captured_date, country)
);

CREATE INDEX IF NOT EXISTS idx_ranks_country_date ON app_store_ranks (country, captured_date);
