/*
    Implements endpoints related to customers.
 */

package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.Customer
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus

class CustomerService(private val dal: AntaeusDal) {
    fun fetchAll(): List<Customer> {
        return dal.fetchCustomers()
    }

    fun fetch(id: Int): Customer {
        return dal.fetchCustomer(id) ?: throw CustomerNotFoundException(id)
    }

    fun fetchAllInvoicesByCustomerId(id:Int): List<Invoice> {
        return dal.fetchAllInvoicesByCustomerId(id);
    }

    fun fetchAllInvoicesByCustomerIdAndStatus(id: Int, status: InvoiceStatus): List<Invoice> {
        return dal.fetchAllInvoicesByCustomerIdAndInvoiceStatus(id, status)
    }

}
