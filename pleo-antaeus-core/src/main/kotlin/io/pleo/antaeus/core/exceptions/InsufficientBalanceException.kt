package io.pleo.antaeus.core.exceptions

import io.pleo.antaeus.models.Invoice

class InsufficientBalanceException(inv: Invoice) : Exception("Insufficient balance to charge $inv")
