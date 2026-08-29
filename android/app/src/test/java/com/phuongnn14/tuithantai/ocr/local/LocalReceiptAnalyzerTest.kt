package com.phuongnn14.tuithantai.ocr.local

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

class LocalReceiptAnalyzerTest {
    @Test
    fun semanticModelOnlyReceivesAmountsThatSurviveHardGuards() {
        val candidates = LocalAmountCandidates.extract(
            """
                Total due 415,000 VND
                Cash received 500,000 VND
                Change returned 85,000 VND
            """.trimIndent()
        )

        assertEquals(listOf(415_000L), candidates.map { it.amount.toLong() })
    }

    @Test
    fun candidateExtractionRejectsMeasurementsDatesAndIds() {
        val candidates = LocalAmountCandidates.extract(
            """
                Date 12/06/2026 Time 12:24
                Invoice 100097932
                Maximum weight 30,000 g
                Item 81,600
                Discount -32,000
                Final amount 49,600
            """.trimIndent()
        )

        assertEquals(listOf(81_600L, 49_600L), candidates.map { it.amount.toLong() })
    }

    @Test
    fun duplicateAmountsRemainAttachedToTheirSemanticLines() {
        val candidates = LocalAmountCandidates.extract(
            "Total amount 38,000 VND\nPayment by transfer 38,000 VND"
        )

        assertEquals(2, candidates.size)
        assertEquals("Payment by transfer 38,000 VND", candidates.last().sourceLine)
    }

    @Test
    fun orderCodeAndWeightCanNeverBecomeMoney() {
        val candidates = LocalAmountCandidates.extract(
            "Invoice 260629C13TP4BS\nMaximum weight 30,000 g\nAmount collected 0 VND"
        )

        assertEquals(emptyList<LocalAmountCandidate>(), candidates)
    }

    @Test
    fun discountsAndServicePhoneNumbersNeverReachMiniLm() {
        val candidates = LocalAmountCandidates.extract(
            """
                Giảm giá 246.400
                Tổng đài góp ý 1800 1063
                Tổng đài bảo hành 1900.232.465
                Thanh toán 527.600
            """.trimIndent()
        )

        assertEquals(listOf(527_600L), candidates.map { it.amount.toLong() })
    }

    @Test
    fun longReceiptKeepsHeaderAndFinalPayableRows() {
        val receipt = buildString {
            appendLine("WINMART PHIEU TINH TIEN")
            repeat(180) { appendLine("Product $it 12,000 VND") }
            appendLine("Total goods discount 32,000 VND")
            appendLine("Final amount payable 666,100 VND")
        }

        val compact = LocalReceiptAnalyzer.compactOcr(receipt)

        org.junit.Assert.assertTrue(compact.startsWith("WINMART PHIEU TINH TIEN"))
        org.junit.Assert.assertTrue(compact.contains("Final amount payable 666,100 VND"))
        org.junit.Assert.assertTrue(compact.length <= 2_400)
    }

    @Test
    fun modelChecksumHelperUsesSha256() {
        val file = File.createTempFile("minilm-test", ".bin").apply { writeText("Tui Than Tai") }
        try {
            assertEquals(
                "594c7eb0f72f84a721422446cb3f1a0b1a893d2e26c464ca772f59218855016e",
                LocalSemanticModel.sha256(file)
            )
        } finally {
            file.delete()
        }
    }
}
