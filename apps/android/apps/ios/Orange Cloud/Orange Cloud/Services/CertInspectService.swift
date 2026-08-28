//
//  CertInspectService.swift
//  Orange Cloud
//
//  SSL 证书检查（无需 CF 账号）：对 host:443 起 TLS，在服务器信任质询里抓取证书链，
//  叶子证书 DER 交 X509Lite 解析到期/SAN 等。即便证书过期/不受信也能取到（先抓后判）。
//  URLSession 回调在 delegate 队列（非主线程），故 delegate 标 nonisolated。
//

import Foundation
import Security

nonisolated struct CertInspectService {

    func inspect(host: String) async throws -> CertInfo {
        let raw = host.trimmingCharacters(in: .whitespacesAndNewlines)
        let stripped = raw
            .replacingOccurrences(of: "https://", with: "")
            .replacingOccurrences(of: "http://", with: "")
        let hostOnly = stripped.split(separator: "/").first.map(String.init) ?? stripped
        guard !hostOnly.isEmpty, let url = URL(string: "https://\(hostOnly)/") else {
            throw APIError.networkError(URLError(.badURL))
        }
        return try await withCheckedThrowingContinuation { continuation in
            let delegate = TrustCaptureDelegate(continuation: continuation)
            let session = URLSession(configuration: .ephemeral, delegate: delegate, delegateQueue: nil)
            delegate.session = session
            session.dataTask(with: url).resume()
        }
    }

    static func buildInfo(from trust: SecTrust) -> CertInfo {
        var chainSubjects: [String] = []
        var leafDER: [UInt8] = []
        var keyBits: Int?

        if let chain = SecTrustCopyCertificateChain(trust) as? [SecCertificate] {
            for (index, cert) in chain.enumerated() {
                let summary = (SecCertificateCopySubjectSummary(cert) as String?) ?? "—"
                chainSubjects.append(summary)
                if index == 0 {
                    leafDER = [UInt8](SecCertificateCopyData(cert) as Data)
                    if let key = SecCertificateCopyKey(cert) {
                        keyBits = SecKeyGetBlockSize(key) * 8
                    }
                }
            }
        }

        let parsed = X509Lite.parse(der: leafDER)
        return CertInfo(
            subjectCN: parsed?.subjectCN ?? chainSubjects.first ?? "—",
            issuerCN: parsed?.issuerCN ?? (chainSubjects.count > 1 ? chainSubjects[1] : "—"),
            notBefore: parsed?.notBefore,
            notAfter: parsed?.notAfter,
            sanDNSNames: parsed?.sanDNS ?? [],
            serialHex: parsed?.serialHex,
            publicKeyBits: keyBits,
            chainSubjects: chainSubjects
        )
    }
}

private nonisolated final class TrustCaptureDelegate: NSObject, URLSessionDelegate, @unchecked Sendable {

    private let lock = NSLock()
    private var finished = false
    private var continuation: CheckedContinuation<CertInfo, Error>?
    var session: URLSession?

    init(continuation: CheckedContinuation<CertInfo, Error>) {
        self.continuation = continuation
    }

    func urlSession(_ session: URLSession,
                    didReceive challenge: URLAuthenticationChallenge,
                    completionHandler: @escaping (URLSession.AuthChallengeDisposition, URLCredential?) -> Void) {
        guard challenge.protectionSpace.authenticationMethod == NSURLAuthenticationMethodServerTrust,
              let trust = challenge.protectionSpace.serverTrust else {
            completionHandler(.performDefaultHandling, nil)
            return
        }
        let info = CertInspectService.buildInfo(from: trust)
        completionHandler(.cancelAuthenticationChallenge, nil)   // 已取到证书，停止请求
        finish(.success(info))
    }

    func urlSession(_ session: URLSession, task: URLSessionTask, didCompleteWithError error: Error?) {
        finish(.failure(APIError.networkError(error ?? URLError(.badServerResponse))))
    }

    private func finish(_ result: Result<CertInfo, Error>) {
        lock.lock()
        if finished { lock.unlock(); return }
        finished = true
        let cont = continuation
        continuation = nil
        lock.unlock()
        session?.finishTasksAndInvalidate()
        switch result {
        case .success(let value): cont?.resume(returning: value)
        case .failure(let error): cont?.resume(throwing: error)
        }
    }
}
