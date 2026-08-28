//
//  Account.swift
//  Orange Cloud
//

import Foundation

nonisolated struct Account: Codable, Identifiable, Hashable, Sendable {
    let id:   String
    let name: String
    let type: String?
}
