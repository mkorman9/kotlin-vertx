package com.github.mkorman9.vertx.client

import com.github.mkorman9.vertx.AppContext
import com.github.mkorman9.vertx.security.AuthorizationMiddleware
import com.github.mkorman9.vertx.utils.StatusDTO
import com.github.mkorman9.vertx.utils.endWithJson
import com.github.mkorman9.vertx.utils.handleJsonBody
import dev.misfitlabs.kotlinguice4.getInstance
import io.vertx.core.http.HttpServerRequest
import io.vertx.ext.web.Router
import java.time.LocalDateTime
import java.time.format.DateTimeParseException

class ClientApi(context: AppContext) {
    private val clientRepository = context.injector.getInstance<ClientRepository>()
    private val authorizationMiddleware = context.injector.getInstance<AuthorizationMiddleware>()

    val router: Router = Router.router(context.vertx).apply {
        get("/").handler { ctx ->
            val params = parseFindClientsQueryParams(ctx.request())

            clientRepository.findPaged(
                filtering = params.filtering,
                paging = params.paging,
                sorting = params.sorting
            )
                .onSuccess { clientsPage -> ctx.response().endWithJson(clientsPage) }
                .onFailure { failure -> ctx.fail(500, failure) }
        }

        get("/:id").handler { ctx ->
            val id = ctx.pathParam("id")

            clientRepository.findById(id)
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

        post("/")
            .handler { ctx -> authorizationMiddleware.authorize(ctx, allowedRoles = setOf("CLIENTS_EDITOR")) }
            .handler { ctx ->
                ctx.handleJsonBody<ClientAddPayload> { payload ->
                    clientRepository.add(payload)
                        .onSuccess { client -> ctx.response().endWithJson(ClientAddResponse(id = client.id.toString())) }
                        .onFailure { failure -> ctx.fail(500, failure) }
                }
            }

        put("/:id")
            .handler { ctx -> authorizationMiddleware.authorize(ctx, allowedRoles = setOf("CLIENTS_EDITOR")) }
            .handler { ctx ->
                ctx.handleJsonBody<ClientUpdatePayload> { payload ->
                    val id = ctx.pathParam("id")

                    clientRepository.update(id, payload)
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

        delete("/:id")
            .handler { ctx -> authorizationMiddleware.authorize(ctx, allowedRoles = setOf("CLIENTS_EDITOR")) }
            .handler { ctx ->
                val id = ctx.pathParam("id")

                clientRepository.delete(id)
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
        val creditCardFilter = request.getParam("filter[creditCard]")

        var page = request.getParam("page", "1").toInt()
        if (page < 1) {
            page = 1
        }

        var pageSize = request.getParam("pageSize", "10").toInt()
        if (pageSize < 1) {
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
                bornBefore = bornBeforeFilter,
                creditCard = creditCardFilter
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
}
