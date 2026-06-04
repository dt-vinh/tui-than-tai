import SwiftData
import SwiftUI

@main
struct LuckyMoneyApp: App {
    private let modelContainer: ModelContainer

    init() {
        do {
            modelContainer = try ModelContainer(
                for: LMCategory.self,
                LMExpense.self,
                LMWallet.self,
                LMBudget.self,
                LMRecurringRule.self,
                LMSplitBill.self,
                LMUserSettings.self
            )
        } catch {
            fatalError("Unable to create SwiftData container: \(error)")
        }
    }

    var body: some Scene {
        WindowGroup {
            RootView()
        }
        .modelContainer(modelContainer)
    }
}
