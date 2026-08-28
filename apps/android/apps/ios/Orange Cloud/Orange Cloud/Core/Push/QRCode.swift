//
//  QRCode.swift
//  Orange Cloud
//
//  端点 URL 的二维码（CoreImage）。
//

import UIKit
import CoreImage.CIFilterBuiltins

nonisolated enum QRCode {
    static func image(_ string: String) -> UIImage? {
        let filter = CIFilter.qrCodeGenerator()
        filter.message = Data(string.utf8)
        filter.correctionLevel = "M"
        guard let output = filter.outputImage?.transformed(by: CGAffineTransform(scaleX: 8, y: 8)),
              let cgImage = CIContext().createCGImage(output, from: output.extent) else { return nil }
        return UIImage(cgImage: cgImage)
    }
}
