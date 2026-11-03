package com.example.paisa.parser

import com.example.paisa.data.local.entity.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BankSmsParserTest {

    private lateinit var parser: BankSmsParser

    @Before
    fun setUp() {
        parser = DefaultBankSmsParser()
    }

    // =========================================================================
    // 1. Debit Alerts Across Major Indian Banks
    // =========================================================================

    @Test
    fun parse_hdfcUpiDebit_extractsAllFieldsAccurately() {
        val sms = "Dear Customer, INR 1,450.50 debited from A/c **4821 on 26-Oct-26 to VPA swiggy@icici (UPI Ref 428910482101). Avail Bal: INR 45,210.00."
        val result = parser.parse(sms)

        assertTrue(result is SmsParseResult.Success)
        val success = result as SmsParseResult.Success
        val tx = success.transaction

        assertEquals(145050L, tx.amountPaise) // ₹1,450.50
        assertEquals(TransactionType.DEBIT, tx.type)
        assertEquals("4821", tx.accountLastFourDigits)
        assertEquals(4521000L, tx.availableBalancePaise) // ₹45,210.00
        assertEquals("428910482101", tx.reference)
        assertEquals(PaymentChannel.UPI, tx.paymentChannel)
        assertEquals(ConfidenceLevel.HIGH, success.confidence)

        // Ensure available balance is never confused with transaction amount
        assertNotEquals(tx.amountPaise, tx.availableBalancePaise)
    }

    @Test
    fun parse_iciciCardSpend_extractsCorrectly() {
        val sms = "You have spent INR 1,299.00 on your ICICI Bank Card XX9104 at AMAZON RETAIL on 25-Oct-26. Avail Limit: INR 85,000.00."
        val result = parser.parse(sms)

        assertTrue(result is SmsParseResult.Success)
        val tx = (result as SmsParseResult.Success).transaction

        assertEquals(129900L, tx.amountPaise) // ₹1,299.00
        assertEquals(TransactionType.DEBIT, tx.type)
        assertEquals("9104", tx.accountLastFourDigits)
        assertEquals(8500000L, tx.availableBalancePaise) // ₹85,000.00
        assertEquals(PaymentChannel.CARD, tx.paymentChannel)
    }

    @Test
    fun parse_sbiNetBankingDebit_extractsCorrectly() {
        val sms = "Your A/c ending 6231 debited by Rs.2,150.00 on 20/10/26 through Net Banking. Avail Bal: Rs.52,000.00. -SBI"
        val result = parser.parse(sms)

        assertTrue(result is SmsParseResult.Success)
        val tx = (result as SmsParseResult.Success).transaction

        assertEquals(215000L, tx.amountPaise) // ₹2,150.00
        assertEquals(TransactionType.DEBIT, tx.type)
        assertEquals("6231", tx.accountLastFourDigits)
        assertEquals(5200000L, tx.availableBalancePaise) // ₹52,000.00
        assertEquals(PaymentChannel.NET_BANKING, tx.paymentChannel)
    }

    @Test
    fun parse_atmWithdrawal_extractsAtmChannelAndAmount() {
        val sms = "Rs 12,000.00 withdrawn from ATM using Card ending 4821 on 24-Oct-26. Avail Bal: Rs 33,210.00."
        val result = parser.parse(sms)

        assertTrue(result is SmsParseResult.Success)
        val tx = (result as SmsParseResult.Success).transaction

        assertEquals(1200000L, tx.amountPaise) // ₹12,000.00
        assertEquals(TransactionType.DEBIT, tx.type)
        assertEquals("4821", tx.accountLastFourDigits)
        assertEquals(3321000L, tx.availableBalancePaise) // ₹33,210.00
        assertEquals(PaymentChannel.ATM, tx.paymentChannel)
    }

    @Test
    fun parse_sbiUpiWithSingleDecimal_extractsCorrectPaise() {
        val sms = "Dear SBI UPI User, A/C 6231 debited by 450.0 on 26Oct26 trf to Ramesh Kumar Ref No 428910482101. Bal: Rs 52,000.00."
        val result = parser.parse(sms)

        assertTrue(result is SmsParseResult.Success)
        val tx = (result as SmsParseResult.Success).transaction

        assertEquals(45000L, tx.amountPaise) // 450.0 = ₹450.00 = 45000 paise
        assertEquals(TransactionType.DEBIT, tx.type)
        assertEquals("6231", tx.accountLastFourDigits)
        assertEquals(5200000L, tx.availableBalancePaise)
        assertEquals("428910482101", tx.reference)
    }

    @Test
    fun parse_axisBankFlightPayment_extractsCorrectly() {
        val sms = "INR 9,800.00 debited from Axis Bank A/c no. XX1159 on 18-10-26 towards IndiGo. Avail Bal: INR 2,800.00."
        val result = parser.parse(sms)

        assertTrue(result is SmsParseResult.Success)
        val tx = (result as SmsParseResult.Success).transaction

        assertEquals(980000L, tx.amountPaise) // ₹9,800.00
        assertEquals(TransactionType.DEBIT, tx.type)
        assertEquals("1159", tx.accountLastFourDigits)
        assertEquals(280000L, tx.availableBalancePaise) // ₹2,800.00
    }

    @Test
    fun parse_kotakWithdrawal_extractsCorrectly() {
        val sms = "Rs 1,500.00 was withdrawn from your Kotak Bank A/C 7834 on 26-Oct-26. Bal: Rs 15,000.00."
        val result = parser.parse(sms)

        assertTrue(result is SmsParseResult.Success)
        val tx = (result as SmsParseResult.Success).transaction

        assertEquals(150000L, tx.amountPaise)
        assertEquals(TransactionType.DEBIT, tx.type)
        assertEquals("7834", tx.accountLastFourDigits)
        assertEquals(1500000L, tx.availableBalancePaise)
    }

    // =========================================================================
    // 2. Credit Alerts Across Major Variations
    // =========================================================================

    @Test
    fun parse_hdfcSalaryCredit_extractsCreditTypeAndHighAmount() {
        val sms = "A/c **4821 credited with INR 95,000.00 on 25-Oct-26 by A/c linked to VPA techcorp@hdfc (UPI Ref 428900112233). Avail Bal: INR 1,24,580.00."
        val result = parser.parse(sms)

        assertTrue(result is SmsParseResult.Success)
        val tx = (result as SmsParseResult.Success).transaction

        assertEquals(9500000L, tx.amountPaise) // ₹95,000.00
        assertEquals(TransactionType.CREDIT, tx.type)
        assertEquals("4821", tx.accountLastFourDigits)
        assertEquals("428900112233", tx.reference)
        assertEquals(12458000L, tx.availableBalancePaise) // ₹1,24,580.00
    }

    @Test
    fun parse_neftImpsCredit_extractsNeftChannel() {
        val sms = "Your A/C ending 4821 is credited with Rs 28,000.00 on 22-Oct-26 by NEFT/IMPS-CMS4821. Bal Rs 1,12,000.00."
        val result = parser.parse(sms)

        assertTrue(result is SmsParseResult.Success)
        val tx = (result as SmsParseResult.Success).transaction

        assertEquals(2800000L, tx.amountPaise)
        assertEquals(TransactionType.CREDIT, tx.type)
        assertEquals("4821", tx.accountLastFourDigits)
        assertEquals(PaymentChannel.NEFT_IMPS, tx.paymentChannel)
    }

    @Test
    fun parse_dividendSmallCredit_extractsCorrectly() {
        val sms = "Your A/C 6231 has been credited with Rs 4,200.00 on 15Oct26 by Dividend Payout. Avail Bal Rs 56,200.00."
        val result = parser.parse(sms)

        assertTrue(result is SmsParseResult.Success)
        val tx = (result as SmsParseResult.Success).transaction

        assertEquals(420000L, tx.amountPaise)
        assertEquals(TransactionType.CREDIT, tx.type)
        assertEquals("6231", tx.accountLastFourDigits)
        assertEquals(5620000L, tx.availableBalancePaise)
    }

    // =========================================================================
    // 3. Negative Cases / Ignored Messages
    // =========================================================================

    @Test
    fun parse_otpMessage_isRejectedAsOtp() {
        val sms = "Your OTP for transaction of Rs 1,450.00 on Card ending 4821 is 849201. Valid for 10 mins. Do not share with anyone."
        val result = parser.parse(sms)

        assertTrue(result is SmsParseResult.Rejected)
        assertEquals(RejectionReason.OTP_MESSAGE, (result as SmsParseResult.Rejected).reason)
    }

    @Test
    fun parse_loginOtp_isRejectedAsOtp() {
        val sms = "389210 is your secret OTP for HDFC Bank NetBanking login. Never share OTP with anyone."
        val result = parser.parse(sms)

        assertTrue(result is SmsParseResult.Rejected)
        assertEquals(RejectionReason.OTP_MESSAGE, (result as SmsParseResult.Rejected).reason)
    }

    @Test
    fun parse_declinedTransaction_isRejectedAsFailed() {
        val sms = "Transaction of INR 1,500.00 on A/c **4821 declined due to insufficient funds."
        val result = parser.parse(sms)

        assertTrue(result is SmsParseResult.Rejected)
        assertEquals(RejectionReason.TRANSACTION_FAILED_OR_DECLINED, (result as SmsParseResult.Rejected).reason)
    }

    @Test
    fun parse_failedPayment_isRejectedAsFailed() {
        val sms = "Payment of Rs 890.00 to Uber failed. Any amount debited will be refunded within 3-5 business days."
        val result = parser.parse(sms)

        assertTrue(result is SmsParseResult.Rejected)
        assertEquals(RejectionReason.TRANSACTION_FAILED_OR_DECLINED, (result as SmsParseResult.Rejected).reason)
    }

    @Test
    fun parse_paymentRequest_isRejectedAsPaymentRequest() {
        val sms = "John Doe has requested Rs. 500.00 from you on PhonePe. Click here to approve or decline payment."
        val result = parser.parse(sms)

        assertTrue(result is SmsParseResult.Rejected)
        assertEquals(RejectionReason.PAYMENT_REQUEST, (result as SmsParseResult.Rejected).reason)
    }

    @Test
    fun parse_promotionalLoanMessage_isRejectedAsPromotional() {
        val sms = "Congratulations! Pre-approved personal loan of Rs 5,00,000 waiting for you. Apply now on HDFC Bank."
        val result = parser.parse(sms)

        assertTrue(result is SmsParseResult.Rejected)
        assertEquals(RejectionReason.PROMOTIONAL_OR_OFFER, (result as SmsParseResult.Rejected).reason)
    }

    @Test
    fun parse_promotionalCardCashback_isRejectedAsPromotional() {
        val sms = "Special offer! Flat 20% off on your next spend on Card ending 4821. Claim now."
        val result = parser.parse(sms)

        assertTrue(result is SmsParseResult.Rejected)
        assertEquals(RejectionReason.PROMOTIONAL_OR_OFFER, (result as SmsParseResult.Rejected).reason)
    }

    @Test
    fun parse_balanceInquiryOnly_isRejectedAsBalanceInquiry() {
        val sms = "Your available balance for A/C XX4821 is INR 45,210.00 as on 26-Oct-26."
        val result = parser.parse(sms)

        assertTrue(result is SmsParseResult.Rejected)
        assertEquals(RejectionReason.BALANCE_INQUIRY_ONLY, (result as SmsParseResult.Rejected).reason)
    }

    @Test
    fun parse_miniStatement_isRejectedAsBalanceInquiry() {
        val sms = "Clear balance in A/c 6231 is Rs 52,000.00. Mini statement generated."
        val result = parser.parse(sms)

        assertTrue(result is SmsParseResult.Rejected)
        assertEquals(RejectionReason.BALANCE_INQUIRY_ONLY, (result as SmsParseResult.Rejected).reason)
    }

    // =========================================================================
    // 4. Duplicate Detection Tests
    // =========================================================================

    @Test
    fun deduplicationKey_isIdenticalForIdenticalTransactions() {
        val sms1 = "Dear Customer, INR 1,450.50 debited from A/c **4821 on 26-Oct-26 to VPA swiggy@icici (UPI Ref 428910482101). Avail Bal: INR 45,210.00."
        val sms2 = "Dear Customer, INR 1,450.50 debited from A/c **4821 on 26-Oct-26 to VPA swiggy@icici (UPI Ref 428910482101). Avail Bal: INR 45,210.00."

        val res1 = parser.parse(sms1) as SmsParseResult.Success
        val res2 = parser.parse(sms2) as SmsParseResult.Success

        assertEquals(res1.transaction.deduplicationKey, res2.transaction.deduplicationKey)
        assertTrue(res1.transaction.deduplicationKey.contains("4821"))
        assertTrue(res1.transaction.deduplicationKey.contains("428910482101"))
    }

    @Test
    fun deduplicationKey_isIdenticalAcrossSlightlyDifferentPhrasingWithSameReference() {
        val smsVariantA = "A/c **4821 debited INR 1,450.50 on 26-Oct-26. UPI Ref: 428910482101. Avail Bal: INR 45,210.00"
        val smsVariantB = "Dear Customer, INR 1,450.50 debited from A/c **4821 on 26-Oct-26 to VPA swiggy@icici (UPI Ref 428910482101). Avail Bal: INR 45,210.00."

        val resA = parser.parse(smsVariantA) as SmsParseResult.Success
        val resB = parser.parse(smsVariantB) as SmsParseResult.Success

        assertEquals(resA.transaction.deduplicationKey, resB.transaction.deduplicationKey)
    }

    @Test
    fun deduplicationKey_differsForDifferentAmountsOrAccounts() {
        val sms1 = "INR 500.00 debited from A/c **4821 on 26-Oct-26. UPI Ref 111122223333."
        val sms2 = "INR 600.00 debited from A/c **4821 on 26-Oct-26. UPI Ref 111122223333."
        val sms3 = "INR 500.00 debited from A/c **9104 on 26-Oct-26. UPI Ref 111122223333."

        val res1 = parser.parse(sms1) as SmsParseResult.Success
        val res2 = parser.parse(sms2) as SmsParseResult.Success
        val res3 = parser.parse(sms3) as SmsParseResult.Success

        assertNotEquals(res1.transaction.deduplicationKey, res2.transaction.deduplicationKey)
        assertNotEquals(res1.transaction.deduplicationKey, res3.transaction.deduplicationKey)
    }

    // =========================================================================
    // 5. Amount & Balance Parsing Tests
    // =========================================================================

    @Test
    fun amountParser_parsesComplexIndianCommasAndDecimals() {
        assertEquals(25000000L, AmountParser.parseToPaise("2,50,000.00")) // 2.5 Lakhs
        assertEquals(100000000L, AmountParser.parseToPaise("10,00,000")) // 10 Lakhs
        assertEquals(50L, AmountParser.parseToPaise("0.50")) // 50 paise
        assertEquals(5L, AmountParser.parseToPaise("0.05")) // 5 paise
        assertEquals(145050L, AmountParser.parseToPaise("Rs. 1,450.50"))
        assertEquals(9500000L, AmountParser.parseToPaise("INR 95,000.00"))
        assertEquals(124900L, AmountParser.parseToPaise("₹1,249.00"))
    }
}
