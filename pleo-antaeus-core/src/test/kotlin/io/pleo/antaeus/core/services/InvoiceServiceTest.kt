package io.pleo.antaeus.core.services

import io.mockk.every
import io.mockk.mockk
import io.pleo.antaeus.core.exceptions.FailedUpdatingStatusException
import io.pleo.antaeus.core.exceptions.InvoiceNotFoundException
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal

class InvoiceServiceTest {
    private val invoice1 = Invoice(1, 1, Money(BigDecimal(10.0), Currency.DKK), InvoiceStatus.PENDING)
    private val invoice2 = Invoice(2, 2, Money(BigDecimal(10.0), Currency.EUR), InvoiceStatus.PAID)
    private val invoice3 = Invoice(3, 1, Money(BigDecimal(10.0), Currency.DKK), InvoiceStatus.PAID)
    private val invoice1Changed = Invoice(1, 1, Money(BigDecimal(10.0), Currency.DKK), InvoiceStatus.PAID)



    private val dal = mockk<AntaeusDal> {
        every { fetchInvoice(404) } returns null
        every { fetchInvoice(1) } returns invoice1
        every { fetchInvoices() } returns listOf(invoice1, invoice2, invoice3)
        every { fetchPendingInvoices() } returns listOf(invoice1)
        every { updateInvoiceStatus(1, InvoiceStatus.PAID)} returns invoice1Changed
        every { updateInvoiceStatus(404, InvoiceStatus.PAID)} returns null
        every { fetchAllInvoicesByCustomerId(1) } returns listOf(invoice1, invoice3)
        every { fetchAllInvoicesByCustomerId(2) } returns listOf(invoice2)
        every { fetchAllInvoicesByCustomerIdAndInvoiceStatus(1, InvoiceStatus.PENDING)} returns listOf(invoice1)
        every { fetchAllInvoicesByCustomerIdAndInvoiceStatus(1, InvoiceStatus.PAID)} returns listOf(invoice3)
        every { fetchAllInvoicesByCustomerIdAndInvoiceStatus(2, InvoiceStatus.PENDING)} returns listOf()
        every { fetchAllInvoicesByCustomerIdAndInvoiceStatus(2, InvoiceStatus.PAID)} returns listOf(invoice2)

    }

    private val invoiceService = InvoiceService(dal = dal)

    @Test
    fun `will throw if invoice is not found`() {
        assertThrows<InvoiceNotFoundException> {
            invoiceService.fetch(404)
        }
    }

    @Test
    fun `should fetch all invoices`() {
        var invoices = invoiceService.fetchAll()
        assert(invoices.size == 3)
        assert(invoices[0] == invoice1)
        assert(invoices[1] == invoice2)
        assert(invoices[2] == invoice3)
    }

    @Test
    fun `should fetch all pending invoices`() {
        var invoices = invoiceService.fetchAllPending()
        assert(invoices.size == 1)
        assert(invoices[0] == invoice1)
    }

    @Test
    fun `should update invoice status`() {
        var invoice = invoiceService.updateInvoiceStatus(1, InvoiceStatus.PAID)
        assert(invoice == invoice1Changed)
    }

    @Test
    fun `will throw FailedUpdatingStatusException`() {
        assertThrows<FailedUpdatingStatusException> {
            invoiceService.updateInvoiceStatus(404, InvoiceStatus.PAID)
        }
    }

    @Test
    fun `should fetch all invoices by customer id`() {
        var invoices = invoiceService.fetchAllInvoicesByCustomerId(1)
        assert(invoices.size == 2)
        assert(invoices[0] == invoice1)
        assert(invoices[1] == invoice3)

        invoices = invoiceService.fetchAllInvoicesByCustomerId(2)
        assert(invoices.size == 1)
        assert(invoices[0] == invoice2)
    }

    @Test
    fun `should fetch all invoices by customer id and status`() {
        var invoices = invoiceService.fetchAllInvoicesByCustomerIdAndStatus(1, InvoiceStatus.PENDING)
        assert(invoices.size == 1)
        assert(invoices[0] == invoice1)

        invoices = invoiceService.fetchAllInvoicesByCustomerIdAndStatus(1, InvoiceStatus.PAID)
        assert(invoices.size == 1)
        assert(invoices[0] == invoice3)

        invoices = invoiceService.fetchAllInvoicesByCustomerIdAndStatus(2, InvoiceStatus.PENDING)
        assert(invoices.isEmpty())

        invoices = invoiceService.fetchAllInvoicesByCustomerIdAndStatus(2, InvoiceStatus.PAID)
        assert(invoices.size == 1)
        assert(invoices[0] == invoice2)
    }
}
