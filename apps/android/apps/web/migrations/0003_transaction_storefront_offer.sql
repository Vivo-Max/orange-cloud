-- 交易表补充两列：
--   offer_identifier  Apple 优惠标识字符串（offerIdentifier，促销 / 兑换码用，无则 NULL）
--   storefront        App Store 商店地区，ISO 3166-1 alpha-3（如 USA / CHN，无则 NULL）
-- 两者在购买时即固定，store.ts 落库时一并写入；历史行从 notifications.raw_payload 回填
-- （raw_payload 存的是 JSON.stringify({payload,transaction,renewal}) 全量解码 JSON）。

ALTER TABLE transactions ADD COLUMN offer_identifier TEXT;
ALTER TABLE transactions ADD COLUMN storefront TEXT;

-- 回填：取该交易最近一条带值的关联通知
UPDATE transactions
SET offer_identifier = (
	SELECT json_extract(n.raw_payload, '$.transaction.offerIdentifier')
	FROM notifications n
	WHERE n.transaction_id = transactions.transaction_id
	  AND json_extract(n.raw_payload, '$.transaction.offerIdentifier') IS NOT NULL
	ORDER BY n.received_at DESC
	LIMIT 1
)
WHERE offer_identifier IS NULL;

UPDATE transactions
SET storefront = (
	SELECT json_extract(n.raw_payload, '$.transaction.storefront')
	FROM notifications n
	WHERE n.transaction_id = transactions.transaction_id
	  AND json_extract(n.raw_payload, '$.transaction.storefront') IS NOT NULL
	ORDER BY n.received_at DESC
	LIMIT 1
)
WHERE storefront IS NULL;
