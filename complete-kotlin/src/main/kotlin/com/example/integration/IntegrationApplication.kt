package com.example.integration

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ImportResource

@SpringBootApplication
@ImportResource("/integration/integration.xml")
class IntegrationApplication

fun main(args: Array<String>) {
    val ctx = runApplication<IntegrationApplication>(*args)
    println("Hit Enter to terminate")
    System.`in`.read()
    ctx.close()
}