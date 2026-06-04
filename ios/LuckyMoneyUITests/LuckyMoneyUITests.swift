import XCTest

final class LuckyMoneyUITests: XCTestCase {
    func testOnboardingHomeManualEntrySmoke() {
        let app = XCUIApplication()
        app.launch()

        if app.buttons["Start now"].exists {
            app.buttons["Start now"].tap()
        } else if app.buttons["Trải nghiệm ngay"].exists {
            app.buttons["Trải nghiệm ngay"].tap()
        }

        XCTAssertTrue(app.tabBars.buttons.firstMatch.waitForExistence(timeout: 5))
    }
}
