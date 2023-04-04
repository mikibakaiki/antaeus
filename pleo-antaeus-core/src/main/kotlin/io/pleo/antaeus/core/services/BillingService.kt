package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.exceptions.FailedUpdatingStatusException
import io.pleo.antaeus.core.exceptions.NoPendingInvoiceException
import io.pleo.antaeus.core.exceptions.UnableToChargeInvoiceException
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import mu.KotlinLogging

class BillingService(
    private val paymentProvider: PaymentProvider,
    private val invoiceService: InvoiceService
) {
    private val logger = KotlinLogging.logger {}
    //private var successfulList: MutableList<Invoice> = mutableListOf()
    private var failedList: MutableList<Invoice> = mutableListOf()
    fun processAllPendingInvoices(): List<Invoice> {
        // val currentDate = LocalDate.now()
        // val firstDayOfMonth = LocalDate.of(currentDate.year, currentDate.month, 1)
        val invoices = invoiceService.fetchAllPending()
        if (invoices.isEmpty()) {
            throw NoPendingInvoiceException()
        }
        for (invoice in invoices) {
            try {
                chargeInvoice(invoice)
            } catch (e: UnableToChargeInvoiceException) {
                failedList.add(invoice)
            } catch (e: FailedUpdatingStatusException) {
                failedList.add(invoice)
            }
        }

        return failedList
    }
    private fun chargeInvoice(inv: Invoice): Invoice {
        val successfullyCharged = paymentProvider.charge(inv)
        logger.info("Invoice ${inv.id} charged? $successfullyCharged")
        if (!successfullyCharged) {
            logger.info("Failed at invoice ${inv.id}")
            throw UnableToChargeInvoiceException(inv)
        }
        try {
            return invoiceService.updateInvoiceStatus(inv.id, InvoiceStatus.PAID)
        } catch (e: FailedUpdatingStatusException) {
            paymentProvider.cancelCharge(inv)
            throw e
        }
    }
}
