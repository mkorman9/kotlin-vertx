package com.github.mkorman9.vertx.client

import com.github.mkorman9.vertx.*
import com.github.mkorman9.vertx.utils.StatusDTO
import com.github.mkorman9.vertx.utils.endWithJson
import com.github.mkorman9.vertx.utils.handleJsonBody
import io.vertx.core.http.HttpServerRequest
import io.vertx.ext.web.Router
import java.time.LocalDateTime
import java.time.format.DateTimeParseException
import java.util.*

fun createClientRouter(context: AppContext): Router {
    val clientsRepository = ClientRepository(context.sessionFactory)

    return Router.router(context.vertx).apply {
        get("/").handler { ctx ->
            val params = parseFindClientsQueryParams(ctx.request())

            clientsRepository.findPaged(
                filtering = params.filtering,
                paging = params.paging,
                sorting = params.sorting
            )
                .onSuccess { clientsPage -> ctx.response().endWithJson(clientsPage) }
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
                    }.toMutableList()
                ))
                    .onSuccess { ctx.response().endWithJson(ClientAddResponse(id = id.toString())) }
                    .onFailure { failure -> ctx.fail(500, failure) }
            }
        }

        put("/:id").handler { ctx ->
            ctx.handleJsonBody<ClientUpdatePayload>(context.validator) { payload ->
                val id = ctx.pathParam("id")

                clientsRepository.update(id, payload)
                    .onSuccess { client ->
                        if (client != null) {
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

        delete("/:id").handler { ctx ->
            val id = ctx.pathParam("id")

            clientsRepository.delete(id)
                .onSuccess { deleted ->
                    if (deleted) {
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

private fun parseFindClientsQueryParams(request: HttpServerRequest): FindClientsParams {
    var genderFilter = request.getParam("filter[gender]")
    if (!hashSetOf("-", "M", "F").contains(genderFilter)) {
        genderFilter = null
    }

    val firstNameFilter = request.getParam("filter[firstName]")
    val lastNameFilter = request.getParam("filter[lastName]")
    val addressFilter = request.getParam("filter[address]")
    val phoneNumberFilter = request.getParam("filter[phoneNumber]")
    val emailFilter = request.getParam("filter[email]")
    val bornAfterFilter = try {
        val v = request.getParam("filter[bornAfter]")
        if (v == null) {
            null
        } else {
            LocalDateTime.parse(v)
        }
    } catch (e: DateTimeParseException) {
        null
    }
    val bornBeforeFilter = try {
        val v = request.getParam("filter[bornBefore]")
        if (v == null) {
            null
        } else {
            LocalDateTime.parse(v)
        }
    } catch (e: DateTimeParseException) {
        null
    }

    var page = request.getParam("page", "0").toInt()
    if (page > 0) {
        page = 0
    }

    var pageSize = request.getParam("pageSize", "10").toInt()
    if (pageSize < 0) {
        pageSize = 10
    }
    if (pageSize > 100) {
        pageSize = 100
    }

    var sortBy = request.getParam("sortBy", "id")
    if (!hashSetOf("id", "gender", "firstName", "lastName", "address", "phoneNumber", "email", "birthDate").contains(sortBy)) {
        sortBy = "id"
    }

    val sortReverse = request.params().contains("sortReverse")

    return FindClientsParams(
        filtering = ClientsFilteringOptions(
            gender = genderFilter,
            firstName = firstNameFilter,
            lastName = lastNameFilter,
            address = addressFilter,
            phoneNumber = phoneNumberFilter,
            email = emailFilter,
            bornAfter = bornAfterFilter,
            bornBefore = bornBeforeFilter
        ),
        paging = ClientsPagingOptions(
            pageNumber = page,
            pageSize = pageSize
        ),
        sorting = ClientsSortingOptions(
            sortBy = sortBy,
            sortReverse = sortReverse
        )
    )
}
