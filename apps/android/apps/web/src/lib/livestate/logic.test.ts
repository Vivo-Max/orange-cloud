import { describe, expect, it } from "vitest";
import { bucketOfAppVersionState, reduceTrackState } from "./logic";

describe("bucketOfAppVersionState", () => {
	it("各 App 版本状态映射到展示桶", () => {
		expect(bucketOfAppVersionState("READY_FOR_DISTRIBUTION")).toBe("live");
		expect(bucketOfAppVersionState("READY_FOR_SALE")).toBe("live");
		expect(bucketOfAppVersionState("WAITING_FOR_REVIEW")).toBe("in_review");
		expect(bucketOfAppVersionState("IN_REVIEW")).toBe("in_review");
		expect(bucketOfAppVersionState("PENDING_DEVELOPER_RELEASE")).toBe("pending_release");
		expect(bucketOfAppVersionState("REJECTED")).toBe("cleared");
		expect(bucketOfAppVersionState("PREPARE_FOR_SUBMISSION")).toBe("cleared");
		expect(bucketOfAppVersionState("WHATEVER_UNKNOWN")).toBeNull();
	});
});

describe("reduceTrackState", () => {
	it("送审：把新版本标 pending（审核中）", () => {
		expect(reduceTrackState({ liveVersion: "1.3.0" }, "1.4.0", "in_review")).toEqual({
			liveVersion: "1.3.0",
			pendingVersion: "1.4.0",
			pendingState: "in_review",
		});
	});

	it("过审上架：推进 live、清掉 pending", () => {
		expect(
			reduceTrackState({ liveVersion: "1.3.0", pendingVersion: "1.4.0", pendingState: "in_review" }, "1.4.0", "live"),
		).toEqual({ liveVersion: "1.4.0" });
	});

	it("被拒 / 撤回：撤下 pending、保留已上架", () => {
		expect(
			reduceTrackState({ liveVersion: "1.3.0", pendingVersion: "1.4.0", pendingState: "in_review" }, "1.4.0", "cleared"),
		).toEqual({ liveVersion: "1.3.0" });
	});

	it("不把已上架（<= live）的版本标成 pending", () => {
		expect(reduceTrackState({ liveVersion: "1.4.0" }, "1.3.0", "in_review")).toEqual({ liveVersion: "1.4.0" });
	});

	it("live 单调前进：迟到的旧版本不回退", () => {
		expect(reduceTrackState({ liveVersion: "1.4.0" }, "1.3.0", "live").liveVersion).toBe("1.4.0");
	});

	it("撤回针对的若不是当前在审版本，不动 pending", () => {
		const cur = { liveVersion: "1.3.0", pendingVersion: "1.4.0", pendingState: "in_review" as const };
		expect(reduceTrackState(cur, "1.3.5", "cleared")).toEqual(cur);
	});
});
