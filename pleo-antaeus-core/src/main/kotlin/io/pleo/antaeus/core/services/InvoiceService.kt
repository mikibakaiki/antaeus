/*
    Implements endpoints related to invoices.
 */

package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.exceptions.InvoiceNotFoundException
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

class InvoiceService(private val dal: AntaeusDal) {
    fun fetchAll(): List<Invoice> {
        return dal.fetchInvoices()
    }

    fun fetch(id: Int): Invoice {
        return dal.fetchInvoice(id) ?: throw InvoiceNotFoundException(id)
    }

    fun fetchAllPending(): List<Invoice> {
        return dal.fetchPendingInvoices()
    }

    fun updateInvoiceStatus(id: Int, status: InvoiceStatus = InvoiceStatus.PAID): Invoice {
        logger.info("receiving $id and $status")
        val updatedInvoice = dal.updateInvoiceStatus(id, status) ?: throw InvoiceNotFoundException(id)
        logger.info("updated invoice => $updatedInvoice")
        return updatedInvoice

    }
}
