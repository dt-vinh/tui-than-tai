package com.phuongnn14.tuithantai.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class LuckyWalletRepositoryTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: LuckyWalletRepository

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        )
            .allowMainThreadQueries()
            .build()
        repository = LuckyWalletRepository(db)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun insertTransactionAdjustsAccountBalanceAtomically() = runBlocking {
        repository.insertAccount(AccountEntity("Cash", 1_000_000.0))

        repository.insertTransaction(
            TransactionEntity(
                title = "Dinner",
                amount = 250_000.0,
                type = "EXPENSE",
                category = "Ăn uống",
                accountName = "Cash",
                date = 1L
            )
        )

        val transactions = db.transactionDao().getAllTransactionsList()
        val account = db.accountDao().getAllAccountsList().single()
        assertEquals(1, transactions.size)
        assertEquals(750_000.0, account.balance, 0.0)
    }

    @Test
    fun insertTransactionRollsBackWhenAccountDoesNotExist() = runBlocking {
        var thrown = false

        try {
            repository.insertTransaction(
                TransactionEntity(
                    title = "Bad account",
                    amount = 1_000_000.0,
                    type = "EXPENSE",
                    category = "Khác",
                    accountName = "Typo",
                    date = 1L
                )
            )
        } catch (e: IllegalStateException) {
            thrown = true
        }

        assertTrue(thrown)
        assertTrue(db.transactionDao().getAllTransactionsList().isEmpty())
    }
}
