//
//  ContinuedTaskRunner.swift
//  Orange Cloud
//
//  iOS 26 BGContinuedProcessingTask 封装：把用户发起的长任务（R2 copy/move 等）
//  放进系统「连续后台任务」里跑——带系统进度条 + 取消按钮，退到后台仍继续到完成。
//  低版本无此 API，调用方回退前台执行（见 R2ObjectListViewModel.runTransfer）。
//
//  设计：App 启动时 register() 注册固定标识符的处理器；run() 提交请求并 await 到完成。
//  实际工作跑在系统派发的处理器里（前台提交时立即开始，可延续到后台）。
//  标识符登记在 Info.plist 的 BGTaskSchedulerPermittedIdentifiers。
//

import Foundation
import BackgroundTasks

@available(iOS 26.0, *)
nonisolated enum ContinuedTaskRunner {

    static let taskIdentifier = "jiamin.chen.Orange-Cloud.transfer"

    typealias ProgressCallback = @Sendable (Double) -> Void
    typealias Operation = @Sendable (
        _ progress: @escaping ProgressCallback,
        _ isCancelled: @escaping @Sendable () -> Bool
    ) async throws -> Void

    /// 提交失败（设备不支持 / 已有同名任务在跑）→ 调用方据此回退前台执行
    struct SubmitFailed: Error {}

    private static let store = JobStore()

    /// App 启动时注册（App.init 内，iOS 26+），必须在 didFinishLaunching 前。
    static func register() {
        BGTaskScheduler.shared.register(forTaskWithIdentifier: taskIdentifier, using: nil) { task in
            guard let task = task as? BGContinuedProcessingTask else {
                task.setTaskCompleted(success: false)
                return
            }
            Task { await handle(task) }
        }
    }

    /// 提交一个连续后台任务跑 operation，await 至完成。提交失败抛 SubmitFailed。
    static func run(title: String, subtitle: String, operation: @escaping Operation) async throws {
        try await withCheckedThrowingContinuation { (continuation: CheckedContinuation<Void, Error>) in
            Task {
                await store.put(taskIdentifier, Job(operation: operation, continuation: continuation))
                do {
                    let request = BGContinuedProcessingTaskRequest(
                        identifier: taskIdentifier, title: title, subtitle: subtitle
                    )
                    request.strategy = .queue
                    try BGTaskScheduler.shared.submit(request)
                } catch {
                    _ = await store.take(taskIdentifier)
                    continuation.resume(throwing: SubmitFailed())
                }
            }
        }
    }

    private static func handle(_ task: BGContinuedProcessingTask) async {
        guard let job = await store.take(task.identifier) else {
            task.setTaskCompleted(success: false)
            return
        }
        task.progress.totalUnitCount = 100
        let reporter = ProgressReporter(task.progress)
        let work = Task {
            try await job.operation({ reporter.report($0) }, { Task.isCancelled })
        }
        // 系统取消按钮 / 预算耗尽 → expirationHandler 触发，取消工作 Task
        task.expirationHandler = { work.cancel() }
        do {
            try await work.value
            task.setTaskCompleted(success: true)
            job.continuation.resume()
        } catch {
            task.setTaskCompleted(success: false)
            job.continuation.resume(throwing: error)
        }
    }
}

@available(iOS 26.0, *)
private struct Job: Sendable {
    let operation: ContinuedTaskRunner.Operation
    let continuation: CheckedContinuation<Void, Error>
}

@available(iOS 26.0, *)
private actor JobStore {
    private var jobs: [String: Job] = [:]
    func put(_ id: String, _ job: Job) { jobs[id] = job }
    func take(_ id: String) -> Job? { jobs.removeValue(forKey: id) }
}

/// 把 0...1 进度写进 BGContinuedProcessingTask 的 Progress（系统 UI 读它）。
/// Progress 本身线程安全，故 @unchecked Sendable。
nonisolated final class ProgressReporter: @unchecked Sendable {
    private let progress: Progress

    init(_ progress: Progress) {
        self.progress = progress
    }

    func report(_ fraction: Double) {
        progress.completedUnitCount = Int64(min(max(fraction, 0), 1) * 100)
    }
}
