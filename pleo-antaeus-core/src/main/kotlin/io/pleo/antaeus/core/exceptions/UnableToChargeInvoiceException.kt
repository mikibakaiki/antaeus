package io.pleo.antaeus.core.exceptions

import io.pleo.antaeus.models.Invoice

class UnableToChargeInvoiceException(invoice: Invoice, reason: String?) : Exception("Unable to charge $invoice\n Reason: ${reason?:"Unknown"}")