
import io.pleo.antaeus.core.exceptions.CurrencyMismatchException
import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.core.exceptions.InsufficientBalanceException
import io.pleo.antaeus.core.exceptions.NetworkException
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Money
import java.math.BigDecimal
import kotlin.random.Random

// This will create all schemas and setup initial data
internal fun setupInitialData(dal: AntaeusDal) {
    val customers = (1..100).mapNotNull {
        dal.createCustomer(
            currency = Currency.values()[Random.nextInt(0, Currency.values().size)]
        )
    }

    customers.forEach { customer ->
        (1..10).forEach {
            dal.createInvoice(
                amount = Money(
                    value = BigDecimal(Random.nextDouble(10.0, 500.0)),
                    currency = customer.currency
                ),
                customer = customer,
                status = if (it == 1) InvoiceStatus.PENDING else InvoiceStatus.PAID
            )
        }
    }
}

// This is the mocked instance of the payment provider
internal fun getPaymentProvider(): PaymentProvider {
    return object : PaymentProvider {
        override fun charge(invoice: Invoice): Boolean {
            // I added this randomizer of exceptions, so it would better emulate the real-world scenario
            // The logic is:
            //      if the random boolean to be returned is false:
            //          pick randomly an exception and throw it - something went wrong with the payment
            //      else return true - all was fine with the payment

            val exceptions = listOf(CustomerNotFoundException(invoice.customerId), CurrencyMismatchException(invoice.id, invoice.customerId), NetworkException(), InsufficientBalanceException(invoice))
            val result = Random.nextBoolean()
            if (!result) {
                val randomIndex = Random.nextInt(exceptions.size)
                throw exceptions[randomIndex]
            }
            return result
        }

        override fun cancelCharge(invoice: Invoice): Boolean {
            return true
        }
    }
}
