# Restaurant Ops Backend

Restaurant Ops Backend is a backend-first preparation project for a restaurant operation platform.

The goal of this project is to practice realistic backend development using Kotlin, Spring Boot, PostgreSQL, database migrations, transactions, idempotency, and integration tests.

The main feature is an idempotent restaurant order checkout flow. The checkout flow will safely create a payment, update the order status, update the table status, optionally update customer visit history, and reflect the result in sales analytics.

This project is not intended to be a complete restaurant SaaS product. The focus is backend design and implementation, especially transaction-safe checkout behavior.

## Main Goal

Implement an idempotent checkout API that safely checks out a restaurant order using Kotlin, Spring Boot, PostgreSQL transactions, row-level locking, and integration tests.

## Backend Focus

This project prioritizes:

* Kotlin and Spring Boot backend development
* REST API design
* PostgreSQL schema design
* Flyway database migrations
* Transaction-safe business logic
* Idempotency key handling
* Duplicate checkout prevention
* Row-level locking
* Integration testing with a real PostgreSQL database
* Clear documentation and report writing

## Main Deliverable

The main deliverable is an idempotent checkout API that:

* checks out a restaurant order
* creates a payment
* updates the order status
* updates the table status
* optionally updates customer visit history
* updates sales analytics
* prevents duplicate checkout and duplicate payment creation

## What This Project Is Not

This project is not focused on:

* production-level frontend design
* real payment integration
* real POS hardware integration
* real LINE Messaging API integration
* Kubernetes deployment
* microservices
* advanced reservation features
* advanced campaign or marketing features

A minimal frontend may be added later only to demonstrate that the backend works.

## Tech Stack

Planned backend stack:

* Kotlin
* Spring Boot
* PostgreSQL
* Flyway
* Docker Compose
* JUnit 5
* Testcontainers

## One-Sentence Summary

Restaurant Ops Backend is a backend-first Kotlin and Spring Boot project for practicing transaction-safe, idempotent restaurant checkout with PostgreSQL and integration tests.
