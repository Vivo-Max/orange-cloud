//
//  PushCenterViewModel.swift
//  Orange Cloud
//

import Foundation
import Observation

@Observable
@MainActor
final class PushCenterViewModel {

    let registrar = PushRegistrar.shared
    private(set) var inbox: [PushMessage] = []
    var error: String?
    var testSending = false
    var testResult: String?
    private(set) var e2eKey: String? = PushConfig.e2eKey

    func refresh() {
        inbox = PushInbox.all()
        error = registrar.error
        e2eKey = PushConfig.e2eKey
    }

    func generateE2EKey() {
        let key = PushCrypto.generateKey()
        PushConfig.e2eKey = key
        e2eKey = key
    }

    func clearE2EKey() {
        PushConfig.e2eKey = nil
        e2eKey = nil
    }

    func enable() async {
        await registrar.enable()
        refresh()
    }

    func clearInbox() {
        PushInbox.clear()
        inbox = []
    }

    func sendTest() async {
        guard let endpoint = PushConfig.endpointURL else { return }
        let title = "Orange Cloud".addingPercentEncoding(withAllowedCharacters: .urlPathAllowed) ?? "Orange%20Cloud"
        let body = String(localized: "测试推送").addingPercentEncoding(withAllowedCharacters: .urlPathAllowed) ?? "test"
        guard let url = URL(string: "\(endpoint)/\(title)/\(body)") else { return }
        testSending = true
        testResult = nil
        error = nil
        do {
            let (_, response) = try await URLSession.shared.data(from: url)
            let code = (response as? HTTPURLResponse)?.statusCode ?? -1
            testResult = (200...299).contains(code) ? String(localized: "已发送") : String(localized: "失败（\(code)）")
        } catch {
            self.error = error.localizedDescription
        }
        testSending = false
    }
}
