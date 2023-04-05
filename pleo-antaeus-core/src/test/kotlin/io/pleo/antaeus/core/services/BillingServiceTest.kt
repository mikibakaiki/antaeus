package io.pleo.antaeus.core.services


import io.mockk.every
import io.mockk.mockk
import io.pleo.antaeus.core.exceptions.*
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Money
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal

class BillingServiceTest {
    private val invoice1 = Invoice(1, 1, Money(BigDecimal(10.0), Currency.DKK), InvoiceStatus.PENDING)
    private val invoice2 = Invoice(2, 2, Money(BigDecimal(10.0), Currency.EUR), InvoiceStatus.PAID)
    private val invoice4 = Invoice(4, 3, Money(BigDecimal(10.0), Currency.DKK), InvoiceStatus.PENDING)
    private val invoice5 = Invoice(5, 4, Money(BigDecimal(10.0), Currency.USD), InvoiceStatus.PENDING)
    private val invoice6 = Invoice(6, 5, Money(BigDecimal(10.0), Currency.USD), InvoiceStatus.PENDING)
    private val invoice7 = Invoice(7, 5, Money(BigDecimal(10.0), Currency.USD), InvoiceStatus.PENDING)

    private val invoice1Changed = Invoice(1, 1, Money(BigDecimal(10.0), Currency.DKK), InvoiceStatus.PAID)
    private val invoice4Changed = Invoice(4, 3, Money(BigDecimal(10.0), Currency.DKK), InvoiceStatus.PAID)
    private val invoice5Changed = Invoice(5, 4, Money(BigDecimal(10.0), Currency.USD), InvoiceStatus.PAID)
    private val invoice6Changed = Invoice(6, 5, Money(BigDecimal(10.0), Currency.USD), InvoiceStatus.PAID)
    private val invoice7Changed = Invoice(7, 5, Money(BigDecimal(10.0), Currency.USD), InvoiceStatus.PAID)

    private val invoiceService = mockk<InvoiceService> {
        every { fetch(404) } throws InvoiceNotFoundException(404)
        every { fetch(invoice1.id) } returns invoice1
        every { fetch(invoice2.id) } returns invoice2
        every { fetch(invoice4.id) } returns invoice4
        every { fetch(invoice5.id) } returns invoice5
        every { fetch(invoice6.id) } returns invoice6
        every { fetch(invoice7.id) } returns invoice7
        every { updateInvoiceStatus(invoice1.id, InvoiceStatus.PAID)} returns invoice1Changed
        every { updateInvoiceStatus(invoice4.id, InvoiceStatus.PAID)} returns invoice4Changed
        every { updateInvoiceStatus(invoice5.id, InvoiceStatus.PAID)} returns invoice5Changed
        every { updateInvoiceStatus(invoice6.id, InvoiceStatus.PAID)} returns invoice6Changed
        every { updateInvoiceStatus(invoice7.id, InvoiceStatus.PAID)} throws FailedUpdatingStatusException(invoice7.id, InvoiceStatus.PAID) andThen { invoice7Changed}
        every { fetchAllPending() } returns listOf(invoice1, invoice4, invoice5, invoice6, invoice7) andThen { listOf(invoice4, invoice5, invoice6, invoice7) } andThen { listOf() }
    }

    private val paymentProvider = mockk<PaymentProvider>{
        every { charge(invoice1) } returns true
        every { charge(invoice4) } throws CustomerNotFoundException(invoice4.customerId) andThen { true }
        every { charge(invoice5) } throws NetworkException() andThen { true }
        every { charge(invoice6) } throws InsufficientBalanceException(invoice6) andThen { true }
        every { charge(invoice7) } returns true
        every { cancelCharge(invoice7) } returns true

    }

    private val billingService = BillingService(paymentProvider = paymentProvider, invoiceService = invoiceService)

    @Test
    fun `will throw if invoice is not found`() {
        assertThrows<InvoiceNotFoundException> {
            invoiceService.fetch(404)
        }
    }

    @Test
    fun `processPendingInvoice should return string when invoice is already paid`() {
        assert(billingService.processPendingInvoice(invoice2.id) == "$invoice2 was already paid!")
    }

    @Test
    fun `processPendingInvoice should return string when invoice was successfully paid`() {
        assert(billingService.processPendingInvoice(invoice1.id) == "$invoice1Changed was paid!")
    }

    @Test
    fun `processPendingInvoice should throw UnableToChargeInvoiceException on multiple scenarios`() {
        // the charging Service throws CustomerNotFound
        assertThrows<UnableToChargeInvoiceException> { billingService.processPendingInvoice(invoice4.id)  }

        // the Charging Service throws NetworkException
        assertThrows<UnableToChargeInvoiceException> { billingService.processPendingInvoice(invoice5.id)  }

        // the Charging Service throws InsufficientBalanceException
        assertThrows<UnableToChargeInvoiceException> { billingService.processPendingInvoice(invoice6.id)  }
    }

    @Test
    fun `processPendingInvoice should cancel the charge and throw FailedUpdatingStatusException`() {
        // the Charging Service returned true, but failed to update the invoice status in the DB
       assertThrows<FailedUpdatingStatusException> { billingService.processPendingInvoice(invoice7.id) }
    }

    @Test
    fun `processAllPendingInvoices should return list with invoices for which the payment failed`() {

        var failed = billingService.processAllPendingInvoices()

        // invoice1 was successfully paid
        assert(failed.size == 4)
        // the Charging Service throws exceptions for invoice4, invoice5 and invoice6
        assert(failed.contains(invoice4))
        assert(failed.contains(invoice5))
        assert(failed.contains(invoice6))

        // failed to update the status in the db
        assert(failed.contains(invoice7))

        // The second try

        // The Charging Service should return true for all invoices
        // There should be no problem updating the db status
        // The previous 4 PENDING invoices were all paid
        failed = billingService.processAllPendingInvoices()
        assert(failed.isEmpty())

        // The third try

        // there are no more PENDING invoices, throw NoPendingInvoiceException exception
        assertThrows<NoPendingInvoiceException> { billingService.processAllPendingInvoices() }
    }


}