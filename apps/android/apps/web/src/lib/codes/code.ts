// 激活码字符串：生成 + 规范化。纯函数，无 I/O。
// 字母表用 Crockford Base32（去掉易混的 I/L/O/U），核心 10 位 = 50 bit 熵；
// 展示形态 OC-XXXXX-XXXXX，存库用规范化核心串（大写、无前缀/分隔）。

const ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ"; // Crockford32，无 I L O U
const CORE_LEN = 10;

/** 生成一枚新码的规范化核心串（10 位）。byte & 31 取低 5 位，2^5=32 无取模偏置。 */
export function generateCode(): string {
	const bytes = crypto.getRandomValues(new Uint8Array(CORE_LEN));
	let core = "";
	for (const b of bytes) core += ALPHABET[b & 31];
	return core;
}

/** 核心串 -> 展示形态 OC-XXXXX-XXXXX。 */
export function formatCode(core: string): string {
	return `OC-${core.slice(0, 5)}-${core.slice(5, 10)}`;
}

/**
 * 用户输入 -> 规范化核心串；非法返回 null。
 * 容错：大写、去空格/分隔、剥 OC 前缀，再 Crockford 把 I/L->1、O->0（U 不在码内）。
 * 注意顺序：先剥前缀再做 O->0 映射，否则会把前缀的 O 误改。
 */
export function normalizeCode(input: string): string | null {
	if (!input) return null;
	let s = input.toUpperCase().replace(/[^0-9A-Z]/g, ""); // 去掉 - 空格等非字母数字
	if (s.length === CORE_LEN + 2 && s.startsWith("OC")) s = s.slice(2); // 剥 OC 前缀
	s = s.replace(/[ILO]/g, (c) => (c === "O" ? "0" : "1")); // I,L->1; O->0
	if (s.length !== CORE_LEN) return null;
	for (const c of s) if (!ALPHABET.includes(c)) return null;
	return s;
}
