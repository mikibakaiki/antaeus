/*
    Implements endpoints related to invoices.
 */

package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.exceptions.FailedUpdatingStatusException
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

    fun updateInvoiceStatus(id: Int, status: InvoiceStatus): Invoice {
        logger.info("receiving $id and $status")
        val updatedInvoice = dal.updateInvoiceStatus(id, status) ?: throw FailedUpdatingStatusException(id, status)
        logger.info("updated invoice => $updatedInvoice")
        return updatedInvoice

    }

    fun fetchAllInvoicesByCustomerId(customerId: Int): List<Invoice> {
        return dal.fetchAllInvoicesByCustomerId(customerId);
    }

    fun fetchAllInvoicesByCustomerIdAndStatus(customerId: Int, status: InvoiceStatus): List<Invoice> {
        return dal.fetchAllInvoicesByCustomerIdAndInvoiceStatus(customerId, status)
    }
}
