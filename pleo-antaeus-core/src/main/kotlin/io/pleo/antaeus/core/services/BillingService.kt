package io.pleo.antaeus.core.services

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
    private var successfulList: MutableList<Invoice> = mutableListOf()
    private var failedList: MutableList<Invoice> = mutableListOf()
    fun processPendingInvoices(): List<Invoice> {
        // val currentDate = LocalDate.now()
        // val firstDayOfMonth = LocalDate.of(currentDate.year, currentDate.month, 1)
        val invoices = invoiceService.fetchAllPending()
        if (invoices.isEmpty()) {
            throw NoPendingInvoiceException()
        }
        for (invoice in invoices) {
            val updatedInvoice = this.chargeInvoice(invoice)
            if (updatedInvoice == null) {
                failedList.add(invoice)
            } else {
                logger.info("Updated invoice => $updatedInvoice")
                successfulList.add(updatedInvoice)
            }
        }
        return failedList
    }
    private fun chargeInvoice(inv: Invoice): Invoice? {
        val successfullyCharged = paymentProvider.charge(inv)
        logger.info("Invoice ${inv.id} charged? $successfullyCharged")
        if (!successfullyCharged) {
            logger.info("Failed at invoice ${inv.id}")
            // throw UnableToChargeInvoiceException(inv.id)
            return null

        }
        val updatedInvoice = invoiceService.updateInvoiceStatus(inv.id)
        if (updatedInvoice.status != InvoiceStatus.PAID) {
            // Something went wrong ?
            logger.info("Failed: ${updatedInvoice.status} is still pending for invoice ${inv.id}")
            return null
        }
        return updatedInvoice
    }
}
