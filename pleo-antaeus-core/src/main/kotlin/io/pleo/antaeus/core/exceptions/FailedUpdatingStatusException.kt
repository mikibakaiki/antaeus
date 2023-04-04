package io.pleo.antaeus.core.exceptions

import io.pleo.antaeus.models.InvoiceStatus

class FailedUpdatingStatusException(id: Int, newStatus: InvoiceStatus) : Exception("Failed to update Invoice with id = $id to $newStatus")