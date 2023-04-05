package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.exceptions.*
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
    fun processAllPendingInvoices(): List<Invoice> {
        var failedList: MutableList<Invoice> = mutableListOf()
        val invoices = invoiceService.fetchAllPending()
        if (invoices.isEmpty()) {
            throw NoPendingInvoiceException()
        }
        for (invoice in invoices) {
            try {
                val updatedInvoice = chargeInvoice(invoice)
                logger.info("$updatedInvoice was charged and updated")
            } catch (e: UnableToChargeInvoiceException) {
                failedList.add(invoice)
            } catch (e: FailedUpdatingStatusException) {
                failedList.add(invoice)
            }
        }
        return failedList
    }

    fun processPendingInvoice(id:Int): String {
        try {
            val invoice = invoiceService.fetch(id)
            if (invoice.status == InvoiceStatus.PENDING) {
                val updatedInvoice = chargeInvoice(invoice)
                return "$updatedInvoice was paid!"
            } else {
                return "$invoice was already paid!"
            }
        } catch (e: InvoiceNotFoundException) {
            logger.info("Error: $e")
            throw e
        } catch (e: UnableToChargeInvoiceException) {
            throw e
        } catch (e: FailedUpdatingStatusException) {
            throw e
        }
    }
    private fun chargeInvoice(inv: Invoice): Invoice {
        try {
            paymentProvider.charge(inv)
            logger.info("Invoice ${inv.id} was charged")
            return invoiceService.updateInvoiceStatus(inv.id, InvoiceStatus.PAID)
        } catch (e: CustomerNotFoundException) {
            logger.info("Error: $e")
            throw UnableToChargeInvoiceException(inv, e.message)
        } catch (e: NetworkException) {
            logger.info("Error: $e")
            throw UnableToChargeInvoiceException(inv, e.message)
        } catch (e: InsufficientBalanceException) {
            logger.info("Error: $e")
            throw UnableToChargeInvoiceException(inv, e.message)
        } catch (e: FailedUpdatingStatusException) {
            paymentProvider.cancelCharge(inv)
            throw e
        }
    }
}
