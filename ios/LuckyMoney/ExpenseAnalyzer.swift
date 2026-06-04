import Foundation
import UIKit
import Vision

struct LMExpenseSuggestion: Equatable {
    var title: String
    var amount: Int64
    var categoryId: String
    var ocrText: String
    var labels: [String]

    static let empty = LMExpenseSuggestion(title: "", amount: 0, categoryId: "other", ocrText: "", labels: [])
}

enum LMExpenseAnalyzer {
    static func analyze(image: UIImage, categories: [LMCategory]) async -> LMExpenseSuggestion {
        guard let cgImage = image.cgImage else { return .empty }

        var recognizedText = ""
        var labels: [String] = []

        let textRequest = VNRecognizeTextRequest { request, _ in
            let observations = request.results as? [VNRecognizedTextObservation] ?? []
            recognizedText = observations
                .compactMap { $0.topCandidates(1).first?.string }
                .joined(separator: "\n")
        }
        textRequest.recognitionLevel = .accurate
        textRequest.usesLanguageCorrection = true
        textRequest.recognitionLanguages = ["vi-VN", "en-US"]

        let classifyRequest = VNClassifyImageRequest { request, _ in
            labels = (request.results as? [VNClassificationObservation] ?? [])
                .prefix(8)
                .map(\.identifier)
        }

        let handler = VNImageRequestHandler(cgImage: cgImage, options: [:])
        do {
            try handler.perform([textRequest, classifyRequest])
        } catch {
            return .empty
        }

        return LMExpenseSuggestion(
            title: inferTitle(text: recognizedText, labels: labels),
            amount: extractAmount(from: recognizedText),
            categoryId: inferCategory(text: recognizedText, labels: labels, categories: categories),
            ocrText: recognizedText,
            labels: labels
        )
    }

    static func extractAmount(from text: String) -> Int64 {
        let normalized = text
            .replacingOccurrences(of: ",", with: ".")
            .folding(options: [.diacriticInsensitive, .caseInsensitive], locale: .current)
            .lowercased()

        let totalWeightedPattern = #"(?i)(?:total|tong|thanh\s*toan|paid|payment|cong|vnd|d)\D{0,12}([0-9]{1,3}(?:[.\s][0-9]{3})+|[0-9]{4,9})"#
        let generalPattern = #"([0-9]{1,3}(?:[.\s][0-9]{3})+|[0-9]{4,9})\s*(?:vnd|d)?"#

        let weighted = amounts(in: normalized, pattern: totalWeightedPattern)
        if let max = weighted.max() { return max }
        return amounts(in: normalized, pattern: generalPattern).max() ?? 0
    }

    static func inferTitle(text: String, labels: [String]) -> String {
        if let line = text
            .components(separatedBy: .newlines)
            .map({ $0.trimmingCharacters(in: .whitespacesAndNewlines) })
            .first(where: { $0.count >= 3 && $0.count <= 48 && !$0.contains(where: \.isNumber) }) {
            return line
        }
        return labels.first?.capitalized ?? ""
    }

    static func inferCategory(text: String, labels: [String], categories: [LMCategory]) -> String {
        let haystack = normalize(text + " " + labels.joined(separator: " "))
        var best = "other"
        var bestScore = 0

        for category in categories {
            let score = category.keywords
                .split(separator: ",")
                .map { normalize(String($0)) }
                .filter { !$0.isEmpty && haystack.contains($0) }
                .count
            if score > bestScore {
                bestScore = score
                best = category.id
            }
        }

        return best
    }

    private static func amounts(in text: String, pattern: String) -> [Int64] {
        guard let regex = try? NSRegularExpression(pattern: pattern) else { return [] }
        let range = NSRange(text.startIndex..<text.endIndex, in: text)
        return regex.matches(in: text, range: range).compactMap { match in
            guard match.numberOfRanges > 1, let amountRange = Range(match.range(at: 1), in: text) else { return nil }
            let digits = text[amountRange].filter(\.isNumber)
            guard let value = Int64(String(digits)), value >= 1_000, value <= 99_999_999 else { return nil }
            return value
        }
    }

    private static func normalize(_ value: String) -> String {
        value
            .folding(options: [.diacriticInsensitive, .caseInsensitive], locale: .current)
            .lowercased()
            .replacingOccurrences(of: "đ", with: "d")
    }
}
