import SwiftData
import SwiftUI

struct ToolsView: View {
    let wallets: [LMWallet]
    let budgets: [LMBudget]
    let recurringRules: [LMRecurringRule]
    let splitBills: [LMSplitBill]
    let categories: [LMCategory]
    let expenses: [LMExpense]
    let settings: LMUserSettings
    let onCapture: () -> Void

    var body: some View {
        NavigationStack {
            List {
                Button(action: onCapture) {
                    ToolRow(icon: "camera.fill", title: "Scan / AI ghi chi", subtitle: "Chụp bill hoặc đồ vật để tạo khoản chi.")
                }
                NavigationLink { WalletsView(wallets: wallets) } label: {
                    ToolRow(icon: "wallet.pass.fill", title: String(localized: "wallets"), subtitle: "Cash, Momo, bank accounts.")
                }
                NavigationLink { BudgetsView(budgets: budgets, expenses: expenses, categories: categories, settings: settings) } label: {
                    ToolRow(icon: "target", title: String(localized: "budgets"), subtitle: "Monthly limits by category.")
                }
                NavigationLink { CategoryManagerView(categories: categories, settings: settings) } label: {
                    ToolRow(icon: "square.grid.2x2.fill", title: String(localized: "categories"), subtitle: "Video-derived expense taxonomy.")
                }
                NavigationLink { RecurringView(rules: recurringRules, categories: categories, settings: settings) } label: {
                    ToolRow(icon: "repeat", title: String(localized: "recurring"), subtitle: "Rent, Internet, utilities.")
                }
                NavigationLink { SplitBillView(splitBills: splitBills) } label: {
                    ToolRow(icon: "person.3.fill", title: String(localized: "split_bill"), subtitle: "Split shared meals and trips.")
                }
                NavigationLink { SyncAuthView(settings: settings) } label: {
                    ToolRow(icon: "icloud.and.arrow.up.fill", title: String(localized: "sync"), subtitle: "Sign in and sync with backend PC.")
                }
            }
            .navigationTitle("tools")
        }
    }
}

struct ToolRow: View {
    let icon: String
    let title: String
    let subtitle: String

    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: icon)
                .foregroundStyle(.green)
                .frame(width: 34, height: 34)
            VStack(alignment: .leading, spacing: 4) {
                Text(title).font(.headline)
                Text(subtitle).font(.caption).foregroundStyle(.secondary)
            }
        }
    }
}

struct WalletsView: View {
    let wallets: [LMWallet]

    var body: some View {
        List(wallets) { wallet in
            HStack {
                VStack(alignment: .leading) {
                    Text(wallet.name).font(.headline)
                    Text(wallet.kind).font(.caption).foregroundStyle(.secondary)
                }
                Spacer()
                Text(wallet.balance.vnd).bold()
            }
        }
        .navigationTitle("wallets")
    }
}

struct BudgetsView: View {
    let budgets: [LMBudget]
    let expenses: [LMExpense]
    let categories: [LMCategory]
    let settings: LMUserSettings

    var body: some View {
        List(budgets) { budget in
            let spent = expenses.filter { $0.categoryId == budget.categoryId }.reduce(0) { $0 + $1.amount }
            VStack(alignment: .leading, spacing: 8) {
                HStack {
                    Text(categories.category(id: budget.categoryId)?.localizedName(language: settings.language) ?? budget.name)
                        .font(.headline)
                    Spacer()
                    Text("\(spent.vnd) / \(budget.monthlyLimit.vnd)")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
                ProgressView(value: min(Double(spent) / Double(max(budget.monthlyLimit, 1)), 1))
            }
            .padding(.vertical, 4)
        }
        .navigationTitle("budgets")
    }
}

struct CategoryManagerView: View {
    let categories: [LMCategory]
    let settings: LMUserSettings

    var body: some View {
        List(categories) { category in
            VStack(alignment: .leading, spacing: 4) {
                Text(category.localizedName(language: settings.language)).font(.headline)
                Text(category.keywords).font(.caption).foregroundStyle(.secondary)
            }
        }
        .navigationTitle("categories")
    }
}

struct RecurringView: View {
    let rules: [LMRecurringRule]
    let categories: [LMCategory]
    let settings: LMUserSettings

    var body: some View {
        List(rules) { rule in
            HStack {
                VStack(alignment: .leading) {
                    Text(rule.title).font(.headline)
                    Text("Day \(rule.dayOfMonth) · \(categories.category(id: rule.categoryId)?.localizedName(language: settings.language) ?? "")")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
                Spacer()
                Text(rule.amount.vnd).bold()
            }
        }
        .navigationTitle("recurring")
    }
}

struct SplitBillView: View {
    @Environment(\.modelContext) private var context
    let splitBills: [LMSplitBill]
    @State private var title = "Lunch"
    @State private var amount = ""
    @State private var people = "An,Binh,Chi"

    var body: some View {
        List {
            Section("New split") {
                TextField("title", text: $title)
                TextField("amount", text: $amount).keyboardType(.numberPad)
                TextField("People comma separated", text: $people)
                Button("save") {
                    context.insert(LMSplitBill(title: title, totalAmount: Int64(amount.filter(\.isNumber)) ?? 0, people: people))
                    try? context.save()
                    amount = ""
                }
            }

            Section("split_bill") {
                ForEach(splitBills) { bill in
                    VStack(alignment: .leading, spacing: 6) {
                        Text(bill.title).font(.headline)
                        Text("\(bill.totalAmount.vnd) · \(bill.people)")
                            .font(.caption)
                            .foregroundStyle(.secondary)
                        Text("Each: \((bill.totalAmount / Int64(max(bill.people.split(separator: ",").count, 1))).vnd)")
                            .font(.caption.bold())
                    }
                }
            }
        }
        .navigationTitle("split_bill")
    }
}

struct SettingsView: View {
    @Environment(\.modelContext) private var context
    @Bindable var settings: LMUserSettings

    var body: some View {
        NavigationStack {
            Form {
                Section("backend_url") {
                    TextField("backend_url", text: $settings.backendUrl)
                        .textInputAutocapitalization(.never)
                        .keyboardType(.URL)
                    NavigationLink("sync") { SyncAuthView(settings: settings) }
                }

                Section("language") {
                    Picker("language", selection: $settings.language) {
                        Text("Tiếng Việt").tag("vi")
                        Text("English").tag("en")
                    }
                    .pickerStyle(.segmented)
                }

                Section("Account") {
                    Text(settings.email.isEmpty ? "Not signed in" : settings.email)
                    Button("save") { try? context.save() }
                }
            }
            .navigationTitle("settings")
        }
    }
}

struct SyncAuthView: View {
    @Environment(\.modelContext) private var context
    @Bindable var settings: LMUserSettings
    @State private var name = ""
    @State private var email = ""
    @State private var password = ""
    @State private var status = ""
    @State private var busy = false

    var body: some View {
        Form {
            Section("backend_url") {
                TextField("backend_url", text: $settings.backendUrl)
                    .textInputAutocapitalization(.never)
                    .keyboardType(.URL)
            }

            Section("sign_in_register") {
                TextField("name", text: $name)
                TextField("email", text: $email)
                    .textInputAutocapitalization(.never)
                    .keyboardType(.emailAddress)
                SecureField("password", text: $password)
                HStack {
                    Button("login") { Task { await login() } }
                    Button("register") { Task { await register() } }
                }
                .disabled(busy || email.isEmpty || password.isEmpty)
            }

            Section("sync") {
                Button("Sync now") { Task { await syncNow() } }
                    .disabled(busy || settings.accessToken.isEmpty)
                if !status.isEmpty {
                    Text(status).font(.caption).foregroundStyle(.secondary)
                }
            }
        }
        .navigationTitle("sync")
    }

    @MainActor
    private func login() async {
        await run {
            let client = try LMBackendClient(baseURL: settings.backendUrl)
            let auth = try await client.login(email: email, password: password)
            settings.accessToken = auth.accessToken
            settings.refreshToken = auth.refreshToken
            settings.email = auth.user.email
            try context.save()
            status = "Signed in"
        }
    }

    @MainActor
    private func register() async {
        await run {
            let client = try LMBackendClient(baseURL: settings.backendUrl)
            let auth = try await client.register(email: email, password: password, name: name.isEmpty ? email : name)
            settings.accessToken = auth.accessToken
            settings.refreshToken = auth.refreshToken
            settings.email = auth.user.email
            try context.save()
            status = "Registered"
        }
    }

    @MainActor
    private func syncNow() async {
        await run {
            try await LMSyncService.sync(context: context, settings: settings)
            status = "Sync complete"
        }
    }

    @MainActor
    private func run(_ operation: () async throws -> Void) async {
        busy = true
        defer { busy = false }
        do {
            try await operation()
        } catch {
            status = error.localizedDescription
        }
    }
}
