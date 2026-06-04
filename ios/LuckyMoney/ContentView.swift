import PhotosUI
import SwiftData
import SwiftUI

enum LMTab: Hashable {
    case home
    case transactions
    case reports
    case tools
    case settings
}

struct RootView: View {
    @Environment(\.modelContext) private var context
    @Query(sort: \LMCategory.sortOrder) private var categories: [LMCategory]
    @Query(filter: #Predicate<LMExpense> { $0.deletedAt == nil }, sort: \LMExpense.spentAt, order: .reverse) private var expenses: [LMExpense]
    @Query(sort: \LMWallet.sortOrder) private var wallets: [LMWallet]
    @Query private var budgets: [LMBudget]
    @Query private var recurringRules: [LMRecurringRule]
    @Query private var splitBills: [LMSplitBill]
    @Query private var settingsRows: [LMUserSettings]

    @AppStorage("luckyMoneyOnboardingDone") private var onboardingDone = false
    @State private var tab: LMTab = .home
    @State private var showingManualEntry = false
    @State private var showingCapture = false
    @State private var reviewDraft: LMExpenseDraft?

    private var settings: LMUserSettings {
        settingsRows.first ?? LMUserSettings()
    }

    var body: some View {
        TabView(selection: $tab) {
            HomeView(
                expenses: expenses,
                categories: categories,
                settings: settings,
                onManualEntry: { showingManualEntry = true },
                onCapture: { showingCapture = true },
                onReports: { tab = .reports },
                onTransactions: { tab = .transactions }
            )
            .tabItem { Label("home", systemImage: "house.fill") }
            .tag(LMTab.home)

            TransactionsView(expenses: expenses, categories: categories, settings: settings)
                .tabItem { Label("transactions", systemImage: "list.bullet.rectangle") }
                .tag(LMTab.transactions)

            ReportsView(expenses: expenses, categories: categories, settings: settings)
                .tabItem { Label("reports", systemImage: "chart.bar.fill") }
                .tag(LMTab.reports)

            ToolsView(
                wallets: wallets,
                budgets: budgets,
                recurringRules: recurringRules,
                splitBills: splitBills,
                categories: categories,
                expenses: expenses,
                settings: settings,
                onCapture: { showingCapture = true }
            )
            .tabItem { Label("tools", systemImage: "slider.horizontal.3") }
            .tag(LMTab.tools)

            SettingsView(settings: settings)
                .tabItem { Label("settings", systemImage: "gearshape.fill") }
                .tag(LMTab.settings)
        }
        .tint(.green)
        .task {
            LMSeeder.seedIfNeeded(context: context)
            if let persisted = settingsRows.first, persisted.onboardingDone {
                onboardingDone = true
            }
        }
        .fullScreenCover(isPresented: Binding(
            get: { !onboardingDone },
            set: { showing in
                if !showing {
                    onboardingDone = true
                }
            }
        )) {
            OnboardingView(
                onStart: {
                    onboardingDone = true
                    settingsRows.first?.onboardingDone = true
                    try? context.save()
                },
                onAuth: {
                    onboardingDone = true
                    settingsRows.first?.onboardingDone = true
                    tab = .settings
                    try? context.save()
                }
            )
        }
        .sheet(isPresented: $showingManualEntry) {
            NavigationStack {
                ExpenseEditorView(
                    draft: LMExpenseDraft(),
                    categories: categories,
                    wallets: wallets,
                    settings: settings,
                    onSave: saveExpense,
                    onCancel: { showingManualEntry = false }
                )
            }
        }
        .sheet(isPresented: $showingCapture) {
            CaptureFlowView(categories: categories) { draft in
                showingCapture = false
                reviewDraft = draft
            }
        }
        .sheet(item: $reviewDraft) { draft in
            NavigationStack {
                ExpenseEditorView(
                    draft: draft,
                    categories: categories,
                    wallets: wallets,
                    settings: settings,
                    onSave: saveExpense,
                    onCancel: { reviewDraft = nil }
                )
            }
        }
    }

    private func saveExpense(_ draft: LMExpenseDraft) {
        let now = LMClock.nowMillis
        context.insert(LMExpense(
            title: draft.title.isEmpty ? "Expense" : draft.title,
            amount: draft.amount,
            categoryId: draft.categoryId,
            wallet: draft.wallet,
            note: draft.note,
            receiptPath: draft.receiptPath,
            ocrText: draft.ocrText,
            spentAt: draft.spentAt,
            updatedAt: now,
            syncStatus: .pending
        ))
        try? context.save()
        showingManualEntry = false
        reviewDraft = nil
        tab = .transactions
    }
}

struct LMExpenseDraft: Identifiable, Equatable {
    let id = UUID()
    var title: String = ""
    var amount: Int64 = 0
    var categoryId: String = "other"
    var wallet: String = "Personal"
    var note: String = ""
    var receiptPath: String?
    var ocrText: String?
    var spentAt: Int64 = LMClock.nowMillis

    init() {}

    init(suggestion: LMExpenseSuggestion, receiptPath: String?) {
        title = suggestion.title
        amount = suggestion.amount
        categoryId = suggestion.categoryId
        self.receiptPath = receiptPath
        ocrText = suggestion.ocrText
    }
}

struct OnboardingView: View {
    let onStart: () -> Void
    let onAuth: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 24) {
            Spacer(minLength: 36)
            Image(systemName: "camera.viewfinder")
                .font(.system(size: 58, weight: .bold))
                .foregroundStyle(.green)
            Text("onboarding_title")
                .font(.largeTitle.bold())
            Text("onboarding_body")
                .font(.title3)
                .foregroundStyle(.secondary)

            VStack(spacing: 14) {
                FeatureRow(icon: "text.viewfinder", title: "Scan receipt", body: "Vision OCR extracts title and total amount for confirmation.")
                FeatureRow(icon: "fork.knife", title: "Object photo", body: "Image classification and keywords suggest the right category.")
                FeatureRow(icon: "wifi.slash", title: "Offline first", body: "Manual entry and local save work without network.")
            }

            Spacer()
            Button(action: onStart) {
                Text("start_now").frame(maxWidth: .infinity)
            }
            .buttonStyle(.borderedProminent)
            .controlSize(.large)
            Button(action: onAuth) {
                Text("sign_in_register").frame(maxWidth: .infinity)
            }
            .buttonStyle(.bordered)
            .controlSize(.large)
        }
        .padding(24)
    }
}

struct FeatureRow: View {
    let icon: String
    let title: String
    let body: String

    var body: some View {
        HStack(alignment: .top, spacing: 14) {
            Image(systemName: icon)
                .frame(width: 34, height: 34)
                .foregroundStyle(.green)
            VStack(alignment: .leading, spacing: 4) {
                Text(title).font(.headline)
                Text(body).font(.subheadline).foregroundStyle(.secondary)
            }
        }
    }
}

struct HomeView: View {
    let expenses: [LMExpense]
    let categories: [LMCategory]
    let settings: LMUserSettings
    let onManualEntry: () -> Void
    let onCapture: () -> Void
    let onReports: () -> Void
    let onTransactions: () -> Void

    private var monthTotal: Int64 {
        expenses.filter { Date(timeIntervalSince1970: TimeInterval($0.spentAt) / 1000).isInCurrentMonth }.reduce(0) { $0 + $1.amount }
    }

    var body: some View {
        NavigationStack {
            List {
                Section {
                    VStack(alignment: .leading, spacing: 10) {
                        Text("this_month").foregroundStyle(.secondary)
                        Text(monthTotal.vnd).font(.largeTitle.bold())
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                }

                Section {
                    HStack(spacing: 12) {
                        Button(action: onManualEntry) { Label("manual_entry", systemImage: "plus.circle.fill") }
                            .buttonStyle(.borderedProminent)
                        Button(action: onReports) { Label("reports", systemImage: "chart.bar") }
                            .buttonStyle(.bordered)
                    }
                    Button(action: onCapture) { Label("capture", systemImage: "camera.fill").frame(maxWidth: .infinity) }
                        .buttonStyle(.bordered)
                }

                Section("Recent") {
                    if expenses.isEmpty {
                        Text("no_expenses").foregroundStyle(.secondary)
                    } else {
                        ForEach(expenses.prefix(5)) { expense in
                            ExpenseRow(expense: expense, category: categories.category(id: expense.categoryId), settings: settings)
                        }
                        Button("View all", action: onTransactions)
                    }
                }
            }
            .navigationTitle("app_name")
        }
    }
}

struct TransactionsView: View {
    let expenses: [LMExpense]
    let categories: [LMCategory]
    let settings: LMUserSettings

    var body: some View {
        NavigationStack {
            List {
                if expenses.isEmpty {
                    ContentUnavailableView("no_expenses", systemImage: "tray")
                } else {
                    ForEach(expenses) { expense in
                        ExpenseRow(expense: expense, category: categories.category(id: expense.categoryId), settings: settings)
                    }
                }
            }
            .navigationTitle("transactions")
        }
    }
}

struct ReportsView: View {
    let expenses: [LMExpense]
    let categories: [LMCategory]
    let settings: LMUserSettings

    private var totals: [(LMCategory, Int64)] {
        categories.compactMap { category in
            let total = expenses.filter { $0.categoryId == category.id }.reduce(0) { $0 + $1.amount }
            return total > 0 ? (category, total) : nil
        }
        .sorted { $0.1 > $1.1 }
    }

    var body: some View {
        NavigationStack {
            List {
                Section("this_month") {
                    Text(expenses.reduce(0) { $0 + $1.amount }.vnd)
                        .font(.title.bold())
                }
                Section("category") {
                    if totals.isEmpty {
                        Text("no_expenses").foregroundStyle(.secondary)
                    } else {
                        ForEach(totals, id: \.0.id) { item in
                            HStack {
                                Text(item.0.localizedName(language: settings.language))
                                Spacer()
                                Text(item.1.vnd).bold()
                            }
                        }
                    }
                }
            }
            .navigationTitle("reports")
        }
    }
}

struct ExpenseRow: View {
    let expense: LMExpense
    let category: LMCategory?
    let settings: LMUserSettings

    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: icon(for: expense.categoryId))
                .frame(width: 34, height: 34)
                .foregroundStyle(.green)
            VStack(alignment: .leading, spacing: 4) {
                Text(expense.title).font(.headline)
                Text([category?.localizedName(language: settings.language), expense.syncStatus == .synced ? nil : String(localized: "pending_sync")]
                    .compactMap { $0 }
                    .joined(separator: " · "))
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
            Spacer()
            Text(expense.amount.vnd).font(.headline)
        }
        .padding(.vertical, 4)
    }

    private func icon(for categoryId: String) -> String {
        switch categoryId {
        case "food", "coffee": "fork.knife"
        case "transport": "car.fill"
        case "shopping": "bag.fill"
        case "bills": "doc.text.fill"
        case "health": "cross.case.fill"
        default: "creditcard.fill"
        }
    }
}

struct ExpenseEditorView: View {
    @State private var draft: LMExpenseDraft
    @State private var amountText: String
    let categories: [LMCategory]
    let wallets: [LMWallet]
    let settings: LMUserSettings
    let onSave: (LMExpenseDraft) -> Void
    let onCancel: () -> Void

    init(draft: LMExpenseDraft, categories: [LMCategory], wallets: [LMWallet], settings: LMUserSettings, onSave: @escaping (LMExpenseDraft) -> Void, onCancel: @escaping () -> Void) {
        _draft = State(initialValue: draft)
        _amountText = State(initialValue: draft.amount == 0 ? "" : "\(draft.amount)")
        self.categories = categories
        self.wallets = wallets
        self.settings = settings
        self.onSave = onSave
        self.onCancel = onCancel
    }

    var body: some View {
        Form {
            Section("review_expense") {
                TextField("title", text: $draft.title)
                TextField("amount", text: $amountText)
                    .keyboardType(.numberPad)
                Picker("category", selection: $draft.categoryId) {
                    ForEach(categories) { category in
                        Text(category.localizedName(language: settings.language)).tag(category.id)
                    }
                }
                Picker("wallet", selection: $draft.wallet) {
                    ForEach(wallets) { wallet in
                        Text(wallet.name).tag(wallet.name)
                    }
                }
                TextField("note", text: $draft.note, axis: .vertical)
            }
            if let ocrText = draft.ocrText, !ocrText.isEmpty {
                Section("OCR") {
                    Text(ocrText).font(.caption).foregroundStyle(.secondary)
                }
            }
        }
        .navigationTitle("review_expense")
        .toolbar {
            ToolbarItem(placement: .cancellationAction) {
                Button("cancel", action: onCancel)
            }
            ToolbarItem(placement: .confirmationAction) {
                Button("save") {
                    draft.amount = Int64(amountText.filter(\.isNumber)) ?? 0
                    onSave(draft)
                }
                .disabled(draft.title.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty || amountText.filter(\.isNumber).isEmpty)
            }
        }
    }
}

extension [LMCategory] {
    func category(id: String) -> LMCategory? {
        first { $0.id == id }
    }
}

extension Date {
    var isInCurrentMonth: Bool {
        Calendar.current.isDate(self, equalTo: Date(), toGranularity: .month)
    }
}
