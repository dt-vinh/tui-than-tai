import Foundation

struct LMUserDTO: Codable {
    let id: String
    let email: String
    let name: String
}

struct LMAuthResponse: Codable {
    let user: LMUserDTO
    let accessToken: String
    let refreshToken: String
}

struct LMCategoryDTO: Codable {
    let id: String
    let nameVi: String
    let nameEn: String
    let keywords: String
    let updatedAt: Int64?
    let deletedAt: Int64?
}

struct LMExpenseDTO: Codable, Equatable {
    let id: String
    let title: String
    let amount: Int64
    let currency: String
    let categoryId: String
    let wallet: String
    let note: String?
    let receiptPath: String?
    let ocrText: String?
    let spentAt: Int64
    let updatedAt: Int64
    let deletedAt: Int64?
    let serverVersion: Int64

    init(expense: LMExpense) {
        id = expense.id
        title = expense.title
        amount = expense.amount
        currency = expense.currency
        categoryId = expense.categoryId
        wallet = expense.wallet
        note = expense.note
        receiptPath = expense.receiptPath
        ocrText = expense.ocrText
        spentAt = expense.spentAt
        updatedAt = expense.updatedAt
        deletedAt = expense.deletedAt
        serverVersion = expense.serverVersion
    }
}

struct LMCategoriesResponse: Codable {
    let categories: [LMCategoryDTO]
}

struct LMExpensesResponse: Codable {
    let expenses: [LMExpenseDTO]
}

struct LMSyncPullResponse: Codable {
    let expenses: [LMExpenseDTO]
    let categories: [LMCategoryDTO]
    let serverVersion: Int64
}

struct LMSyncPushResponse: Codable {
    let expenses: [LMExpenseDTO]
    let serverVersion: Int64
}

struct LMReceiptResponse: Codable {
    let id: String
    let path: String
}

struct LMHealthResponse: Codable {
    let ok: Bool
    let version: Int64
    let time: Int64
}

enum LMBackendError: Error, LocalizedError {
    case invalidBaseURL
    case invalidResponse
    case http(Int)

    var errorDescription: String? {
        switch self {
        case .invalidBaseURL:
            return "Invalid backend URL"
        case .invalidResponse:
            return "Invalid server response"
        case .http(let code):
            return "Server returned HTTP \(code)"
        }
    }
}

final class LMBackendClient {
    let baseURL: URL
    private let session: URLSession

    init(baseURL: String, session: URLSession = .shared) throws {
        guard let url = URL(string: baseURL.trimmingCharacters(in: .whitespacesAndNewlines)) else {
            throw LMBackendError.invalidBaseURL
        }
        self.baseURL = url
        self.session = session
    }

    func health() async throws -> LMHealthResponse {
        try await request("health", method: "GET")
    }

    func register(email: String, password: String, name: String) async throws -> LMAuthResponse {
        try await request("auth/register", method: "POST", body: ["email": email, "password": password, "name": name])
    }

    func login(email: String, password: String) async throws -> LMAuthResponse {
        try await request("auth/login", method: "POST", body: ["email": email, "password": password])
    }

    func categories(token: String) async throws -> LMCategoriesResponse {
        try await request("categories", method: "GET", token: token)
    }

    func push(token: String, expenses: [LMExpenseDTO]) async throws -> LMSyncPushResponse {
        try await request("sync/push", method: "POST", token: token, body: ["expenses": expenses])
    }

    func pull(token: String, sinceVersion: Int64) async throws -> LMSyncPullResponse {
        try await request("sync/pull?sinceVersion=\(sinceVersion)", method: "GET", token: token)
    }

    func uploadReceipt(token: String, imageData: Data, fileName: String, expenseId: String?) async throws -> LMReceiptResponse {
        let body: [String: String] = [
            "dataBase64": imageData.base64EncodedString(),
            "fileName": fileName,
            "mimeType": "image/jpeg",
            "expenseId": expenseId ?? ""
        ]
        return try await request("receipts", method: "POST", token: token, body: body)
    }

    private func request<Response: Decodable, Body: Encodable>(
        _ path: String,
        method: String,
        token: String? = nil,
        body: Body
    ) async throws -> Response {
        var request = URLRequest(url: try makeURL(path))
        request.httpMethod = method
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        if let token, !token.isEmpty {
            request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        }
        request.httpBody = try JSONEncoder().encode(body)

        let (data, response) = try await session.data(for: request)
        guard let http = response as? HTTPURLResponse else { throw LMBackendError.invalidResponse }
        guard (200..<300).contains(http.statusCode) else { throw LMBackendError.http(http.statusCode) }
        return try JSONDecoder().decode(Response.self, from: data)
    }
}

extension LMBackendClient {
    private func request<Response: Decodable>(
        _ path: String,
        method: String,
        token: String? = nil
    ) async throws -> Response {
        var request = URLRequest(url: try makeURL(path))
        request.httpMethod = method
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        if let token, !token.isEmpty {
            request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        }

        let (data, response) = try await session.data(for: request)
        guard let http = response as? HTTPURLResponse else { throw LMBackendError.invalidResponse }
        guard (200..<300).contains(http.statusCode) else { throw LMBackendError.http(http.statusCode) }
        return try JSONDecoder().decode(Response.self, from: data)
    }

    private func makeURL(_ path: String) throws -> URL {
        let rootString = baseURL.absoluteString.hasSuffix("/") ? baseURL.absoluteString : baseURL.absoluteString + "/"
        guard let root = URL(string: rootString), let url = URL(string: path, relativeTo: root)?.absoluteURL else {
            throw LMBackendError.invalidBaseURL
        }
        return url
    }
}
