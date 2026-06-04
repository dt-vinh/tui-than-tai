import XCTest
@testable import LuckyMoney

final class ExpenseAnalyzerTests: XCTestCase {
    func testExtractsVietnameseTotalAmount() {
        let text = """
        Banh Mi Store
        Banh mi 25.000
        Cafe 18.000
        Tong thanh toan 43.000 VND
        """
        XCTAssertEqual(LMExpenseAnalyzer.extractAmount(from: text), 43_000)
    }

    func testExtractsLargestWhenNoTotalKeyword() {
        let text = "Milk 12.000\nBread 18.000\nCash 50.000"
        XCTAssertEqual(LMExpenseAnalyzer.extractAmount(from: text), 50_000)
    }

    func testNoAmountReturnsZero() {
        XCTAssertEqual(LMExpenseAnalyzer.extractAmount(from: "Thank you"), 0)
    }

    func testInfersFoodCategory() {
        let category = LMExpenseAnalyzer.inferCategory(
            text: "banh mi pho restaurant",
            labels: [],
            categories: LMSeedData.categories
        )
        XCTAssertEqual(category, "food")
    }

    func testInfersTransportCategoryFromLabels() {
        let category = LMExpenseAnalyzer.inferCategory(
            text: "",
            labels: ["taxi", "car"],
            categories: LMSeedData.categories
        )
        XCTAssertEqual(category, "transport")
    }

    func testUnknownCategoryFallsBackToOther() {
        let category = LMExpenseAnalyzer.inferCategory(
            text: "unmatched object",
            labels: [],
            categories: LMSeedData.categories
        )
        XCTAssertEqual(category, "other")
    }
}
