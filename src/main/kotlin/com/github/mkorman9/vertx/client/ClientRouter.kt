package com.github.mkorman9.vertx.client

import com.github.mkorman9.vertx.*
import com.github.mkorman9.vertx.utils.StatusDTO
import com.github.mkorman9.vertx.utils.endWithJson
import com.github.mkorman9.vertx.utils.handleJsonBody
import io.vertx.ext.web.Router
import java.util.*

fun createClientRouter(context: AppContext): Router {
    val clientsRepository = ClientRepository(context.sessionFactory)

    return Router.router(context.vertx).apply {
        get("/").handler { ctx ->
            clientsRepository.findAll()
                .onSuccess { clientsList -> ctx.response().endWithJson(clientsList) }
                .onFailure { failure -> ctx.fail(500, failure) }
        }

        get("/:id").handler { ctx ->
            val id = ctx.pathParam("id")
            clientsRepository.findById(id)
                .onSuccess { client ->
                    if (client != null) {
                        ctx.response().endWithJson(client)
                    } else {
                        ctx.response().setStatusCode(404).endWithJson(StatusDTO(
                            status = "error",
                            message = "client not found"
                        ))
                    }
                }
                .onFailure { failure -> ctx.fail(500, failure) }
        }

        post("/").handler { ctx ->
            ctx.handleJsonBody<ClientAddPayload>(context.validator) { payload ->
                val id = UUID.randomUUID()

                clientsRepository.add(Client(
                    id = id,
                    gender = payload.gender ?: "-",
                    firstName = payload.firstName,
                    lastName = payload.lastName,
                    address = payload.address,
                    phoneNumber = payload.phoneNumber,
                    email = payload.email,
                    birthDate = payload.birthDate,
                    creditCards = (payload.creditCards ?: listOf()).map {
                        CreditCard(
                            clientId = id,
                            number = it.number
                        )
                    }
                ))
                    .onSuccess { ctx.response().endWithJson(ClientAddResponse(id = id.toString())) }
                    .onFailure { failure -> ctx.fail(500, failure) }
            }
        }

        put("/:id").handler { ctx ->
            ctx.handleJsonBody<ClientUpdatePayload>(context.validator) { payload ->
                val id = ctx.pathParam("id")

                clientsRepository.update(id, payload)
                    .onSuccess { updated ->
                        if (updated) {
                            ctx.response().endWithJson(StatusDTO(
                                status = "ok"
                            ))
                        } else {
                            ctx.response().setStatusCode(404).endWithJson(StatusDTO(
                                status = "error",
                                message = "client not found"
                            ))
                        }
                    }
                    .onFailure { failure -> ctx.fail(500, failure) }
            }
        }
    }
}
