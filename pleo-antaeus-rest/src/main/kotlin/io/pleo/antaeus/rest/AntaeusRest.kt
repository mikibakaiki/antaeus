/*
    Configures the rest app along with basic exception handling and URL endpoints.
 */

package io.pleo.antaeus.rest

import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder.*
import io.pleo.antaeus.core.exceptions.*
import io.pleo.antaeus.core.services.BillingService
import io.pleo.antaeus.core.services.CustomerService
import io.pleo.antaeus.core.services.InvoiceService
import io.pleo.antaeus.models.InvoiceStatus
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}
private val thisFile: () -> Unit = {}

class AntaeusRest(
    private val invoiceService: InvoiceService,
    private val customerService: CustomerService,
    private val billingService: BillingService
) : Runnable {

    override fun run() {
        app.start(7000)
    }

    // Set up Javalin rest app
    private val app = Javalin
        .create()
        .apply {
            // InvoiceNotFoundException: return 404 HTTP status code
            exception(EntityNotFoundException::class.java) { _, ctx ->
                ctx.status(404)
            }
            // Unexpected exception: return HTTP 500
            exception(Exception::class.java) { e, _ ->
                logger.error(e) { "Internal server error" }
            }
            // On 404: return message
            error(404) { ctx -> ctx.json("not found") }
        }

    init {
        // Set up URL endpoints for the rest app
        app.routes {
            get("/") {
                it.result("Welcome to Antaeus! see AntaeusRest class for routes")
            }
            path("rest") {
                // Route to check whether the app is running
                // URL: /rest/health
                get("health") {
                    it.json("ok")
                }

                // V1
                path("v1") {
                    path("invoices") {
                        // URL: /rest/v1/invoices
                        get {
                            it.json(invoiceService.fetchAll())
                        }

                        /**
                         * Get all the pending invoices
                         */
                        // URL: /rest/v1/invoices/pending
                        path ("pending") {
                            get {
                                it.json(invoiceService.fetchAllPending())
                            }
                        }

                        // URL: /rest/v1/invoices/{:id}
                        get(":id") {
                            val id = it.pathParam("id").toInt()
                            it.json(invoiceService.fetch(id))
                        }


                    }
                    path("customers") {
                        // URL: /rest/v1/customers
                        get {
                            it.json(customerService.fetchAll())
                        }

                        // URL: /rest/v1/customers/{:id}
                        get(":id") {
                            it.json(customerService.fetch(it.pathParam("id").toInt()))
                        }


                        /**
                         * Get all the invoices for a given customer
                         */
                        // URL: /rest/v1/customers/{:id}/invoices
                        get(":id/invoices"){
                            val id = it.pathParam("id").toInt()
                            it.json(invoiceService.fetchAllInvoicesByCustomerId(id))
                        }

                        /**
                         * Get all the invoices for a given customer with a given status.
                         */
                        // URL: /rest/v1/customers/{:id}/invoices/{:status}
                        get(":id/invoices/:status"){
                            val id = it.pathParam("id").toInt()
                            val status = it.pathParam("status").toUpperCase()
                            if (InvoiceStatus.values().map { invStatus -> invStatus.toString()}.contains(status)) {
                                it.json(invoiceService.fetchAllInvoicesByCustomerIdAndStatus(id, InvoiceStatus.valueOf(status)))
                            } else {
                                it.status(500)
                                it.result("The status '$status' does not exist. Please use either 'pending' or 'paid'")
                            }
                        }
                    }

                    path("billing") {
                        /**
                         * Charge every PENDING invoice.
                         * It will return a list with the invoices that were not charged due to errors.
                         */
                        // URL: /rest/v1/billing
                        post {
                            try {
                                val pendingInvoices = billingService.processAllPendingInvoices()
                                if (pendingInvoices.isEmpty()) {
                                    it.result("Successfully Charged all pending invoices")
                                } else {
                                    val str = "Here's a list of all invoices that failed"

                                    it.result("$str\n\n$pendingInvoices")
                                }
                            } catch (e: UnableToChargeInvoiceException) {
                                it.result("There was a problem: ${e.message}")
                                it.status(500)
                            } catch (e: NoPendingInvoiceException) {
                                it.result("There was a problem: ${e.message}")
                                it.status(500)
                            }
                        }

                        /**
                         * Charge an invoice with a given id
                         */
                        // URL: /rest/v1/billing/{:id}
                        post(":id") {
                            val id = it.pathParam("id").toInt()
                            try {
                                val res = billingService.processPendingInvoice(id)
                                it.json(res)
                            } catch (e: InvoiceNotFoundException) {
                                it.json("There was a problem: ${e.message}")
                                it.status(500)
                            } catch (e: UnableToChargeInvoiceException) {
                                it.json("There was a problem: ${e.message}")
                                it.status(500)
                            } catch (e: FailedUpdatingStatusException) {
                                it.json("There was a problem: ${e.message}")
                                it.status(500)
                            }
                        }
                    }
                }
            }
        }
    }
}
