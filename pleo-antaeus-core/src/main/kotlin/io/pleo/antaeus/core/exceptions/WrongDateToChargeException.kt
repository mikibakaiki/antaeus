package io.pleo.antaeus.core.exceptions

class WrongDateToChargeException : Exception("Failed to charge invoices: only allowed on the first of the month")
