package io.pleo.antaeus.core.exceptions

import io.pleo.antaeus.models.Invoice

class UnableToChargeInvoiceException(invoice: Invoice) : Exception("Unable to charge Invoice $invoice")