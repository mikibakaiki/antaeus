package io.pleo.antaeus.core.services

import io.mockk.every
import io.mockk.mockk
import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import kotlin.random.Random

class CustomerServiceTest {
    private val customer1 = Customer(1, Currency.DKK)
    private val customer2 = Customer(2, Currency.EUR)

    private val dal = mockk<AntaeusDal> {
        every { fetchCustomer(404) } returns null
        every { fetchCustomer(1) } returns customer1
        every { fetchCustomers() } returns listOf(customer1, customer2)
    }

    private val customerService = CustomerService(dal = dal)

    @Test
    fun `will throw if customer is not found`() {
        assertThrows<CustomerNotFoundException> {
            customerService.fetch(404)
        }
    }
    @Test
    fun `should fetch a single customer`() {
        val customer = customerService.fetch(1)
        assert(customer == customer1)
    }

    @Test
    fun `should fetch all customers`() {
        val customers = customerService.fetchAll()
        assert(customers.size == 2)
        assert(customers[0] == customer1)
        assert(customers[1] == customer2)
    }
}
