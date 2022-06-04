package com.github.mkorman9.vertx.client

import com.github.mkorman9.vertx.security.AuthorizationMiddleware
import com.github.mkorman9.vertx.utils.StatusDTO
import com.github.mkorman9.vertx.utils.asyncHandler
import com.github.mkorman9.vertx.utils.endWithJson
import com.github.mkorman9.vertx.utils.handleJsonBody
import com.google.inject.Inject
import com.google.inject.Singleton
import io.vertx.core.Vertx
import io.vertx.core.http.HttpServerRequest
import io.vertx.ext.web.Router
import io.vertx.kotlin.coroutines.await
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeParseException

@Singleton
class ClientApi @Inject constructor(
    private val vertx: Vertx,
    private val clientRepository: ClientRepository,
    private val authorizationMiddleware: AuthorizationMiddleware,
    private val clientEventsPublisher: ClientEventsPublisher
) {
    private val allowedSortByValues = hashSetOf(
        "id", "gender", "firstName", "lastName", "address", "phoneNumber", "email", "birthDate"
    )

    fun createRouter(): Router = Router.router(vertx).apply {
        get("/").asyncHandler { ctx ->
            val params = parseFindClientsQueryParams(ctx.request())

            val clients = clientRepository.findPaged(
                filtering = params.filtering,
                paging = params.paging,
                sorting = params.sorting
            ).await()

            ctx.response().endWithJson(clients.map { mapToClientResponse(it) })
        }

        get("/:id").asyncHandler { ctx ->
            val id = ctx.pathParam("id")

            val client = clientRepository.findById(id).await()
            if (client != null) {
                ctx.response().endWithJson(mapToClientResponse(client))
            } else {
                ctx.response().setStatusCode(404).endWithJson(StatusDTO(
                    status = "error",
                    message = "client not found"
                ))
            }
        }

        post("/")
            .handler { ctx -> authorizationMiddleware.authorize(ctx, allowedRoles = setOf("CLIENTS_EDITOR")) }
            .asyncHandler { ctx ->
                val account = authorizationMiddleware.getActiveAccount(ctx)

                ctx.handleJsonBody<ClientAddPayload> { payload ->
                    val client = clientRepository.add(payload).await()

                    clientEventsPublisher.publish(
                        ClientEvent(
                            operation = ClientEventOperation.ADDED,
                            clientId = client.id,
                            author = account.id
                        )
                    )

                    ctx.response().endWithJson(ClientAddResponse(id = client.id))
                }
            }

        put("/:id")
            .handler { ctx -> authorizationMiddleware.authorize(ctx, allowedRoles = setOf("CLIENTS_EDITOR")) }
            .asyncHandler { ctx ->
                val account = authorizationMiddleware.getActiveAccount(ctx)

                ctx.handleJsonBody<ClientUpdatePayload> { payload ->
                    val id = ctx.pathParam("id")

                    val client = clientRepository.update(id, payload).await()
                    if (client != null) {
                        clientEventsPublisher.publish(
                            ClientEvent(
                                operation = ClientEventOperation.UPDATED,
                                clientId = client.id,
                                author = account.id
                            )
                        )

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
            }

        delete("/:id")
            .handler { ctx -> authorizationMiddleware.authorize(ctx, allowedRoles = setOf("CLIENTS_EDITOR")) }
            .asyncHandler { ctx ->
                val id = ctx.pathParam("id")

                val account = authorizationMiddleware.getActiveAccount(ctx)

                val deleted = clientRepository.delete(id).await()
                if (deleted) {
                    clientEventsPublisher.publish(
                        ClientEvent(
                            operation = ClientEventOperation.DELETED,
                            clientId = id,
                            author = account.id
                        )
                    )

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
    }

    private fun mapToClientResponse(client: Client): ClientResponse {
        return ClientResponse(
            id = client.id,
            gender = client.gender,
            firstName = client.firstName,
            lastName = client.lastName,
            address = client.address,
            phoneNumber = client.phoneNumber,
            email = client.email,
            birthDate = if (client.birthDate == null) null else
                LocalDateTime.ofEpochSecond(client.birthDate!!, 0, ZoneOffset.UTC),
            creditCards = client.creditCards.map { CreditCardResponse(number = it) }
        )
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
        if (!allowedSortByValues.contains(sortBy)) {
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
