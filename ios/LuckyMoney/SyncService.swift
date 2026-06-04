import Foundation
import SwiftData

enum LMSyncService {
    @MainActor
    static func sync(context: ModelContext, settings: LMUserSettings) async throws {
        guard !settings.accessToken.isEmpty else { return }
        let client = try LMBackendClient(baseURL: settings.backendUrl)

        let synced = LMSyncStatus.synced.rawValue
        let pendingDescriptor = FetchDescriptor<LMExpense>(predicate: #Predicate { $0.syncStatusRaw != synced })
        let pending = try context.fetch(pendingDescriptor)
        if !pending.isEmpty {
            let pushed = try await client.push(token: settings.accessToken, expenses: pending.map(LMExpenseDTO.init(expense:)))
            merge(expenses: pushed.expenses, context: context)
            settings.lastServerVersion = max(settings.lastServerVersion, pushed.serverVersion)
        }

        let pulled = try await client.pull(token: settings.accessToken, sinceVersion: settings.lastServerVersion)
        merge(categories: pulled.categories, context: context)
        merge(expenses: pulled.expenses, context: context)
        settings.lastServerVersion = pulled.serverVersion
        try context.save()
    }

    @MainActor
    static func merge(categories: [LMCategoryDTO], context: ModelContext) {
        for dto in categories {
            let id = dto.id
            let descriptor = FetchDescriptor<LMCategory>(predicate: #Predicate { $0.id == id })
            if let existing = try? context.fetch(descriptor).first {
                existing.nameVi = dto.nameVi
                existing.nameEn = dto.nameEn
                existing.keywords = dto.keywords
                existing.updatedAt = dto.updatedAt ?? existing.updatedAt
                existing.deletedAt = dto.deletedAt
            } else {
                context.insert(LMCategory(id: dto.id, nameVi: dto.nameVi, nameEn: dto.nameEn, keywords: dto.keywords, sortOrder: 100, updatedAt: dto.updatedAt ?? LMClock.nowMillis, deletedAt: dto.deletedAt))
            }
        }
    }

    @MainActor
    static func merge(expenses: [LMExpenseDTO], context: ModelContext) {
        for dto in expenses {
            let id = dto.id
            let descriptor = FetchDescriptor<LMExpense>(predicate: #Predicate { $0.id == id })
            if let existing = try? context.fetch(descriptor).first {
                guard existing.updatedAt <= dto.updatedAt || existing.serverVersion <= dto.serverVersion else { continue }
                existing.title = dto.title
                existing.amount = dto.amount
                existing.currency = dto.currency
                existing.categoryId = dto.categoryId
                existing.wallet = dto.wallet
                existing.note = dto.note ?? ""
                existing.receiptPath = dto.receiptPath
                existing.ocrText = dto.ocrText
                existing.spentAt = dto.spentAt
                existing.updatedAt = dto.updatedAt
                existing.deletedAt = dto.deletedAt
                existing.serverVersion = dto.serverVersion
                existing.syncStatus = .synced
            } else {
                context.insert(LMExpense(
                    id: dto.id,
                    title: dto.title,
                    amount: dto.amount,
                    currency: dto.currency,
                    categoryId: dto.categoryId,
                    wallet: dto.wallet,
                    note: dto.note ?? "",
                    receiptPath: dto.receiptPath,
                    ocrText: dto.ocrText,
                    spentAt: dto.spentAt,
                    updatedAt: dto.updatedAt,
                    deletedAt: dto.deletedAt,
                    syncStatus: .synced,
                    serverVersion: dto.serverVersion
                ))
            }
        }
    }
}

enum LMSeeder {
    @MainActor
    static func seedIfNeeded(context: ModelContext) {
        if ((try? context.fetch(FetchDescriptor<LMCategory>())) ?? []).isEmpty {
            LMSeedData.categories.forEach(context.insert)
        }
        if ((try? context.fetch(FetchDescriptor<LMWallet>())) ?? []).isEmpty {
            LMSeedData.wallets.forEach(context.insert)
        }
        if ((try? context.fetch(FetchDescriptor<LMBudget>())) ?? []).isEmpty {
            LMSeedData.budgets.forEach(context.insert)
        }
        if ((try? context.fetch(FetchDescriptor<LMRecurringRule>())) ?? []).isEmpty {
            LMSeedData.recurring.forEach(context.insert)
        }
        if ((try? context.fetch(FetchDescriptor<LMUserSettings>())) ?? []).isEmpty {
            context.insert(LMUserSettings())
        }
        try? context.save()
    }
}

extension Int64 {
    var vnd: String {
        let formatter = NumberFormatter()
        formatter.numberStyle = .decimal
        formatter.groupingSeparator = "."
        return "\(formatter.string(from: NSNumber(value: self)) ?? "\(self)") đ"
    }
}
