/*
    Configures the rest app along with basic exception handling and URL endpoints.
 */

package io.pleo.antaeus.rest

import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder.*
import io.pleo.antaeus.core.exceptions.EntityNotFoundException
import io.pleo.antaeus.core.exceptions.NoPendingInvoiceException
import io.pleo.antaeus.core.exceptions.UnableToChargeInvoiceException
import io.pleo.antaeus.core.services.BillingService
import io.pleo.antaeus.core.services.CustomerService
import io.pleo.antaeus.core.services.InvoiceService
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
                    }

                    path("billing") {
                        get {
                            try {
                                val pendingInvoices = billingService.processAllPendingInvoices()
                                if (pendingInvoices.isEmpty()) {
                                    it.html("<h1>Successfully Charged all pending invoices</h1>")
                                } else {
                                    val html = "<h2>Here's a list of all invoices that failed</h2>"

                                    it.result("$html\n\n$pendingInvoices")
                                }

                            } catch (e: UnableToChargeInvoiceException) {
                                it.html("<h1>There was a problem: ${e.message}</h1>")
                            } catch (e: NoPendingInvoiceException) {
                                it.html("<h1>HURRAY! ${e.message}</h1>")
                            }
                        }

                        // URL: /rest/v1/billing/:id
                        get(":id") {
                            val id = it.pathParam("id").toInt()
                            //it.json()
                        }
                    }
                }
            }
        }
    }
}
