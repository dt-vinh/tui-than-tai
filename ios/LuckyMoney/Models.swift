import Foundation
import SwiftData

enum LMSyncStatus: String, Codable, CaseIterable {
    case pending
    case synced
    case failed
}

@Model
final class LMCategory {
    @Attribute(.unique) var id: String
    var nameVi: String
    var nameEn: String
    var keywords: String
    var sortOrder: Int
    var updatedAt: Int64
    var deletedAt: Int64?

    init(id: String, nameVi: String, nameEn: String, keywords: String, sortOrder: Int, updatedAt: Int64 = LMClock.nowMillis, deletedAt: Int64? = nil) {
        self.id = id
        self.nameVi = nameVi
        self.nameEn = nameEn
        self.keywords = keywords
        self.sortOrder = sortOrder
        self.updatedAt = updatedAt
        self.deletedAt = deletedAt
    }

    func localizedName(language: String) -> String {
        language == "en" ? nameEn : nameVi
    }
}

@Model
final class LMExpense {
    @Attribute(.unique) var id: String
    var title: String
    var amount: Int64
    var currency: String
    var categoryId: String
    var wallet: String
    var note: String
    var receiptPath: String?
    var ocrText: String?
    var spentAt: Int64
    var updatedAt: Int64
    var deletedAt: Int64?
    var syncStatusRaw: String
    var serverVersion: Int64

    init(
        id: String = UUID().uuidString,
        title: String,
        amount: Int64,
        currency: String = "VND",
        categoryId: String,
        wallet: String = "Personal",
        note: String = "",
        receiptPath: String? = nil,
        ocrText: String? = nil,
        spentAt: Int64 = LMClock.nowMillis,
        updatedAt: Int64 = LMClock.nowMillis,
        deletedAt: Int64? = nil,
        syncStatus: LMSyncStatus = .pending,
        serverVersion: Int64 = 0
    ) {
        self.id = id
        self.title = title
        self.amount = amount
        self.currency = currency
        self.categoryId = categoryId
        self.wallet = wallet
        self.note = note
        self.receiptPath = receiptPath
        self.ocrText = ocrText
        self.spentAt = spentAt
        self.updatedAt = updatedAt
        self.deletedAt = deletedAt
        self.syncStatusRaw = syncStatus.rawValue
        self.serverVersion = serverVersion
    }

    var syncStatus: LMSyncStatus {
        get { LMSyncStatus(rawValue: syncStatusRaw) ?? .pending }
        set { syncStatusRaw = newValue.rawValue }
    }
}

@Model
final class LMWallet {
    @Attribute(.unique) var id: String
    var name: String
    var kind: String
    var balance: Int64
    var sortOrder: Int

    init(id: String = UUID().uuidString, name: String, kind: String, balance: Int64, sortOrder: Int) {
        self.id = id
        self.name = name
        self.kind = kind
        self.balance = balance
        self.sortOrder = sortOrder
    }
}

@Model
final class LMBudget {
    @Attribute(.unique) var id: String
    var categoryId: String
    var name: String
    var monthlyLimit: Int64

    init(id: String = UUID().uuidString, categoryId: String, name: String, monthlyLimit: Int64) {
        self.id = id
        self.categoryId = categoryId
        self.name = name
        self.monthlyLimit = monthlyLimit
    }
}

@Model
final class LMRecurringRule {
    @Attribute(.unique) var id: String
    var title: String
    var amount: Int64
    var categoryId: String
    var dayOfMonth: Int

    init(id: String = UUID().uuidString, title: String, amount: Int64, categoryId: String, dayOfMonth: Int) {
        self.id = id
        self.title = title
        self.amount = amount
        self.categoryId = categoryId
        self.dayOfMonth = dayOfMonth
    }
}

@Model
final class LMSplitBill {
    @Attribute(.unique) var id: String
    var title: String
    var totalAmount: Int64
    var people: String
    var paidMask: String

    init(id: String = UUID().uuidString, title: String, totalAmount: Int64, people: String, paidMask: String = "") {
        self.id = id
        self.title = title
        self.totalAmount = totalAmount
        self.people = people
        self.paidMask = paidMask
    }
}

@Model
final class LMUserSettings {
    @Attribute(.unique) var id: String
    var backendUrl: String
    var language: String
    var accessToken: String
    var refreshToken: String
    var email: String
    var lastServerVersion: Int64
    var onboardingDone: Bool

    init(
        id: String = "settings",
        backendUrl: String = "https://api.your-domain.com",
        language: String = Locale.preferredLanguages.first?.hasPrefix("en") == true ? "en" : "vi",
        accessToken: String = "",
        refreshToken: String = "",
        email: String = "",
        lastServerVersion: Int64 = 0,
        onboardingDone: Bool = false
    ) {
        self.id = id
        self.backendUrl = backendUrl
        self.language = language
        self.accessToken = accessToken
        self.refreshToken = refreshToken
        self.email = email
        self.lastServerVersion = lastServerVersion
        self.onboardingDone = onboardingDone
    }
}

enum LMClock {
    static var nowMillis: Int64 {
        Int64(Date().timeIntervalSince1970 * 1000)
    }
}

enum LMSeedData {
    static let categories: [LMCategory] = [
        LMCategory(id: "food", nameVi: "Ăn uống", nameEn: "Food & drink", keywords: "food,restaurant,banh mi,meal,com,pho,bread,noodle,rice", sortOrder: 0),
        LMCategory(id: "coffee", nameVi: "Cafe", nameEn: "Coffee", keywords: "coffee,cafe,tra sua,drink,milk tea", sortOrder: 1),
        LMCategory(id: "transport", nameVi: "Đi lại", nameEn: "Transport", keywords: "taxi,grab,bus,fuel,xang,parking,train", sortOrder: 2),
        LMCategory(id: "shopping", nameVi: "Mua sắm", nameEn: "Shopping", keywords: "shop,market,store,clothes,shoes,supermarket", sortOrder: 3),
        LMCategory(id: "bills", nameVi: "Hóa đơn cố định", nameEn: "Bills", keywords: "electricity,water,internet,phone,bill,paid,total", sortOrder: 4),
        LMCategory(id: "home", nameVi: "Nhà cửa", nameEn: "Home", keywords: "household,gia dung,furniture,kitchen", sortOrder: 5),
        LMCategory(id: "health", nameVi: "Sức khỏe", nameEn: "Health", keywords: "medicine,pharmacy,clinic,hospital,doctor", sortOrder: 6),
        LMCategory(id: "entertainment", nameVi: "Giải trí", nameEn: "Entertainment", keywords: "movie,game,karaoke,cinema,show", sortOrder: 7),
        LMCategory(id: "travel", nameVi: "Du lịch", nameEn: "Travel", keywords: "hotel,flight,trip,travel,ticket", sortOrder: 8),
        LMCategory(id: "family", nameVi: "Gia đình", nameEn: "Family", keywords: "parents,bo me,family,kids,school", sortOrder: 9),
        LMCategory(id: "gifts", nameVi: "Quà tặng", nameEn: "Gifts", keywords: "gift,donate,present", sortOrder: 10),
        LMCategory(id: "repair", nameVi: "Sửa chữa", nameEn: "Repair", keywords: "repair,service,maintenance", sortOrder: 11),
        LMCategory(id: "other", nameVi: "Khác", nameEn: "Other", keywords: "other,unknown", sortOrder: 12)
    ]

    static let wallets: [LMWallet] = [
        LMWallet(name: "Personal", kind: "Cash", balance: 2_450_000, sortOrder: 0),
        LMWallet(name: "Momo", kind: "E-wallet", balance: 520_000, sortOrder: 1),
        LMWallet(name: "Bank", kind: "Bank account", balance: 3_000_000, sortOrder: 2)
    ]

    static let budgets: [LMBudget] = [
        LMBudget(categoryId: "food", name: "Food & drink", monthlyLimit: 3_000_000),
        LMBudget(categoryId: "coffee", name: "Coffee", monthlyLimit: 800_000),
        LMBudget(categoryId: "transport", name: "Transport", monthlyLimit: 1_500_000)
    ]

    static let recurring: [LMRecurringRule] = [
        LMRecurringRule(title: "Internet", amount: 220_000, categoryId: "bills", dayOfMonth: 5),
        LMRecurringRule(title: "Rent", amount: 3_500_000, categoryId: "home", dayOfMonth: 1)
    ]
}
