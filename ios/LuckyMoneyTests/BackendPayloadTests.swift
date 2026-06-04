import XCTest
@testable import LuckyMoney

final class BackendPayloadTests: XCTestCase {
    func testExpensePayloadUsesBackendCamelCase() throws {
        let expense = LMExpense(
            id: "expense-1",
            title: "Coffee",
            amount: 25_000,
            categoryId: "coffee",
            wallet: "Momo",
            note: "Morning",
            spentAt: 1_700_000_000_000,
            updatedAt: 1_700_000_001_000,
            syncStatus: .pending
        )

        let data = try JSONEncoder().encode(LMExpenseDTO(expense: expense))
        let json = try XCTUnwrap(JSONSerialization.jsonObject(with: data) as? [String: Any])

        XCTAssertEqual(json["id"] as? String, "expense-1")
        XCTAssertEqual(json["categoryId"] as? String, "coffee")
        XCTAssertEqual((json["spentAt"] as? NSNumber)?.int64Value, 1_700_000_000_000)
        XCTAssertNil(json["deletedAt"])
    }

    func testPushResponseDecodesServerVersion() throws {
        let json = """
        {
          "expenses": [{
            "id": "expense-1",
            "title": "Coffee",
            "amount": 25000,
            "currency": "VND",
            "categoryId": "coffee",
            "wallet": "Momo",
            "note": "",
            "receiptPath": null,
            "ocrText": null,
            "spentAt": 1700000000000,
            "updatedAt": 1700000001000,
            "deletedAt": null,
            "serverVersion": 4
          }],
          "serverVersion": 4
        }
        """.data(using: .utf8)!

        let decoded = try JSONDecoder().decode(LMSyncPushResponse.self, from: json)
        XCTAssertEqual(decoded.serverVersion, 4)
        XCTAssertEqual(decoded.expenses.first?.serverVersion, 4)
    }
}
