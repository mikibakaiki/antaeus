> :warning: This repository was archived automatically since no ownership was defined :warning:
>
> For details on how to claim stewardship of this repository see:
>
> [How to configure a service in OpsLevel](https://www.notion.so/pleo/How-to-configure-a-service-in-OpsLevel-f6483fcb4fdd4dcc9fc32b7dfe14c262)
>
> To learn more about the automatic process for stewardship which archived this repository see:
>
> [Automatic process for stewardship](https://www.notion.so/pleo/Automatic-process-for-stewardship-43d9def9bc9a4010aba27144ef31e0f2)

## Antaeus

Antaeus (/ænˈtiːəs/), in Greek mythology, a giant of Libya, the son of the sea god Poseidon and the Earth goddess Gaia. He compelled all strangers who were passing through the country to wrestle with him. Whenever Antaeus touched the Earth (his mother), his strength was renewed, so that even if thrown to the ground, he was invincible. Heracles, in combat with him, discovered the source of his strength and, lifting him up from Earth, crushed him to death.

Welcome to our challenge.

## The challenge

As most "Software as a Service" (SaaS) companies, Pleo needs to charge a subscription fee every month. Our database contains a few invoices for the different markets in which we operate. Your task is to build the logic that will schedule payment of those invoices on the first of the month. While this may seem simple, there is space for some decisions to be taken and you will be expected to justify them.

## Instructions

Fork this repo with your solution. Ideally, we'd like to see your progression through commits, and don't forget to update the README.md to explain your thought process.

Please let us know how long the challenge takes you. We're not looking for how speedy or lengthy you are. It's just really to give us a clearer idea of what you've produced in the time you decided to take. Feel free to go as big or as small as you want.

## Insights

### Inside my head

First thing was to just implement a method to get all `PENDING` invoices from the DB and create an endpoint to test it out, `GET /invoices/pending`.

Paying an invoice, in this context, is mainly changing the state of the invoice from `PENDING` to `PAID`. I needed a method to achieve this. I started by implementing a method that changed from `PENDING` to `PAID`. Whilst doing it, i thought that i could be more abstract, and change the method to perform a status change, independent of the status to change to. This would be helpful in the future. (1)

Since the `charge()` method was already done (sort of), i had all the building blocks to perform the specified task: charge every invoice, using the `GET /billing` (yes, i know, a GET is not the correct verb, i ended up changing this to POST :smile: ). This endpoint returns a list of the invoices that could not be charged. It handles all exceptions caught by adding the invoice in question to this `failedList` of invoices.

I then coded the main endpoint, `POST /billing/:id`, which acts as you'd expect: charge the invoice with the specified id. This one has the focus now.

Something was bothering me: In the description of the mock `PaymentProvider` there were some exceptions mentioned, which made sense in a real-world scenario. So i adjusted the logic of the `PaymentProvider.charge()` method to still throw a random `boolean`, but if it was `false`, i.e., the payment was not processed, i would throw a random exception from these 3:

- `CustomerNotFoundException()`
- `NetworkException()`
- `InsufficientBalanceException()`

This emulates better a real-world scenario. I had a 4th exception, `CurrencyMismatchException()` but i noticed that the invoices were being created according to the customers specified currency, so i left it out.

So if payment went well, _nothing else could go wrong right?_ Hmm, _not so fast_.

I thought that there could be some problem updating the status in the DB. So, i decided to add an extra method, `cancelCharge(): Boolean` which just returns `true` for simplicity. It will be called when the payment was successful, but there was a problem changing the state of the invoice in the DB. Thus, the balance will be restored to the customer's account.

Regarding exceptions, i thought that there were three main ones that would be returned from the `billingService`:

- `InvoiceNotFoundException()` - can't bill an invoice that does not exist, can you? :smile:
- `UnableToChargeInvoiceException()` - This was returned for most exceptions caught during the payment process, specifying the underlying reason.
- `FailedUpdatingStatusException()` - This was returned when the payment was successful, but the change in the DB was not.

I also had some questions on how to implement the "first of the month" part. At first, i just wrote some code to check if the request was being made on the first of the month, and act on it.

But, rethinking it, it wouldn't make too much sense: you have to keep making the request until you get it right, i.e., it is the correct day of the month. _Wouldn't it be easier to just have a scheduled task that would do this request on the first day of the month, every month?_ Yep! :smile:

That's where `cron` comes in.
All i had to do now was create a little script that would:

1. Call `GET /invoices/pending`
2. If there were no invoices, `exit`
3. Else, for each of these invoices:
   2.1. Call `POST /billing/invoice_id`
   2.2. Log the result to give visibility.

Wrote a `crontab` file to run this automatically, inside a Docker container _et voi la!_

:warning: the `crontab` file is programmed to run every minute (`* * * * *`) for testing purposes. To actually run it on the first of the month, at 00h00, we'd change it to `0 0 1 * *`.

PS: The process wasn't so linear :sweat_smile:
PPS:

- I have another script which performs the request to `POST /billing`, which is the bulk charging endpoint.
- In the docker-compose file, i also leave 2 options commented out: run the `scheduler_by_id.py` or the `scheduler_batch.py`. I'm currently running the `cron` task. [See more](#running-solution)

### Overall Time

Apr 2nd : 3h
Apr 3rd : 4h
Apr 4th: 1h + 3h
Apr 5th: 2h
Apr 6th: 2h
TOTAL: ~15h

To run the docker-compose.yml, `PERSISTENT='--persistent' docker-compose up --build`

## Developing

Requirements:

- \>= Java 11 environment

Open the project using your favorite text editor. If you are using IntelliJ, you can open the `build.gradle.kts` file and it is gonna setup the project in the IDE for you.

### Building

```
./gradlew build
```

### Running

There are 2 options for running Anteus. You either need libsqlite3 or docker. Docker is easier but requires some docker knowledge. We do recommend docker though.

_Running Natively_

Native java with sqlite (requires libsqlite3):

If you use homebrew on MacOS `brew install sqlite`.

```
./gradlew run
```

_Running through docker_

Install docker for your platform

```
docker build -t antaeus
docker run antaeus
```

### Running solution

### App Structure

The code given is structured as follows. Feel free however to modify the structure to fit your needs.

```
├── buildSrc
|  | gradle build scripts and project wide dependency declarations
|  └ src/main/kotlin/utils.kt
|      Dependencies
|
├── pleo-antaeus-app
|       main() & initialization
|
├── pleo-antaeus-core
|       This is probably where you will introduce most of your new code.
|       Pay attention to the PaymentProvider and BillingService class.
|
├── pleo-antaeus-data
|       Module interfacing with the database. Contains the database
|       models, mappings and access layer.
|
├── pleo-antaeus-models
|       Definition of the Internal and API models used throughout the
|       application.
|
└── pleo-antaeus-rest
        Entry point for HTTP REST API. This is where the routes are defined.
```

### Main Libraries and dependencies

- [Exposed](https://github.com/JetBrains/Exposed) - DSL for type-safe SQL
- [Javalin](https://javalin.io/) - Simple web framework (for REST)
- [kotlin-logging](https://github.com/MicroUtils/kotlin-logging) - Simple logging framework for Kotlin
- [JUnit 5](https://junit.org/junit5/) - Testing framework
- [Mockk](https://mockk.io/) - Mocking library
- [Sqlite3](https://sqlite.org/index.html) - Database storage engine

Happy hacking 😁!
