package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.exceptions.FailedUpdatingStatusException
import io.pleo.antaeus.core.exceptions.NoPendingInvoiceException
import io.pleo.antaeus.core.exceptions.UnableToChargeInvoiceException
import io.pleo.antaeus.core.exceptions.WrongDateToChargeException
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import mu.KotlinLogging
import java.time.LocalDate

class BillingService(
    private val paymentProvider: PaymentProvider,
    private val invoiceService: InvoiceService
) {
    private val logger = KotlinLogging.logger {}
    private var failedList: MutableList<Invoice> = mutableListOf()
    fun processAllPendingInvoices(): List<Invoice> {
        // This code ensures that the charging only occurs at the first of the month
        // However, I will leave it commented for testing purposes: it makes it easier to test the functionality.

        // val currentDate = LocalDate.now()
        // if (currentDate.dayOfMonth != 1) {
        //     throw WrongDateToChargeException()
        // }

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
