//
//  WorkerBuildModels.swift
//  Orange Cloud
//
//  Workers Builds —— Worker 的 CI 构建记录。
//  GET /accounts/{account_id}/builds/workers/{external_script_id}/builds
//  GET /accounts/{account_id}/builds/builds/{build_uuid}/logs
//
//  注意 status 与 outcome 是两个维度：status 只到 stopped 为止（queued/initializing/
//  running/stopped），成功还是失败要看 build_outcome。UI 必须合起来判断，
//  只看 status 会把「失败」显示成「已结束」。
//

import Foundation

nonisolated struct WorkerBuild: Codable, Identifiable, Hashable, Sendable {
    let buildUuid:  String
    /// queued / initializing / running / stopped
    let status:     String?
    /// success / fail / skipped / cancelled / terminated（仅 stopped 后有值）
    let buildOutcome: String?
    let createdOn:  String?
    let modifiedOn: String?
    let buildTriggerMetadata: BuildTriggerMetadata?

    var id: String { buildUuid }

    enum CodingKeys: String, CodingKey {
        case status
        case buildUuid            = "build_uuid"
        case buildOutcome         = "build_outcome"
        case createdOn            = "created_on"
        case modifiedOn           = "modified_on"
        case buildTriggerMetadata = "build_trigger_metadata"
    }

    /// 把 status + outcome 合成用户能看懂的一个状态
    var displayState: BuildDisplayState {
        if status != "stopped" {
            switch status {
            case "queued":       return .queued
            case "initializing": return .running
            case "running":      return .running
            default:             return .unknown
            }
        }
        switch buildOutcome {
        case "success":                 return .success
        case "fail", "terminated":      return .failed
        case "cancelled":               return .cancelled
        case "skipped":                 return .skipped
        default:                        return .unknown
        }
    }
}

nonisolated enum BuildDisplayState: Sendable {
    case queued, running, success, failed, cancelled, skipped, unknown

    var label: String {
        switch self {
        case .queued:    String(localized: "排队中")
        case .running:   String(localized: "构建中")
        case .success:   String(localized: "成功")
        case .failed:    String(localized: "失败")
        case .cancelled: String(localized: "已取消")
        case .skipped:   String(localized: "已跳过")
        case .unknown:   String(localized: "未知")
        }
    }

    var isRunning: Bool { self == .queued || self == .running }
}

nonisolated struct BuildTriggerMetadata: Codable, Hashable, Sendable {
    let author:       String?
    let branch:       String?
    let commitHash:   String?
    let buildCommand: String?
    let buildTriggerSource: String?

    enum CodingKeys: String, CodingKey {
        case author, branch
        case commitHash         = "commit_hash"
        case buildCommand       = "build_command"
        case buildTriggerSource = "build_trigger_source"
    }

    /// commit 只取前 7 位，和 git 短哈希习惯一致
    var shortCommit: String? {
        guard let commitHash, commitHash.count >= 7 else { return commitHash }
        return String(commitHash.prefix(7))
    }
}

/// 构建日志：接口是游标分页的行数组
nonisolated struct BuildLogsPage: Codable, Sendable {
    let lines:  [BuildLogLine]?
    let cursor: String?
}

nonisolated struct BuildLogLine: Codable, Hashable, Sendable {
    let line: String?
    let ts:   String?
}
