package com.github.mkorman9.vertx.client

import com.github.mkorman9.vertx.common.Services
import com.github.mkorman9.vertx.security.AuthorizationMiddleware
import com.github.mkorman9.vertx.utils.VerticleContext
import com.github.mkorman9.vertx.utils.web.*
import io.vertx.ext.web.Router
import io.vertx.kotlin.coroutines.await

class ClientApi (services: Services, context: VerticleContext) {
    private val clientRepository: ClientRepository = services.clientRepository
    private val authorizationMiddleware: AuthorizationMiddleware = services.authorizationMiddleware
    private val clientEventsPublisher: ClientEventsPublisher = services.clientEventsPublisher

    private val websocketApi = ClientEventsWebsocketApi(context)

    val router: Router = Router.router(context.vertx).apply {
        get("/")
            .coroutineHandler(context.scope) { ctx ->
                val queryParams = QueryParamValues.parse(ctx, FIND_PAGED_CLIENTS_QUERY_PARAMS)
                val filtering = ClientFilteringOptions(
                    gender = queryParams.get("filter[gender]"),
                    firstName = queryParams.get("filter[firstName]"),
                    lastName = queryParams.get("filter[lastName]"),
                    address = queryParams.get("filter[address]"),
                    phoneNumber = queryParams.get("filter[phoneNumber]"),
                    email = queryParams.get("filter[email]"),
                    bornAfter = queryParams.get("filter[bornAfter]"),
                    bornBefore = queryParams.get("filter[bornBefore]"),
                    creditCard = queryParams.get("filter[creditCard]")
                )
                val paging = ClientPagingOptions(
                    pageNumber = queryParams.mustGet("page"),
                    pageSize = queryParams.mustGet("pageSize"),
                )
                val sorting = ClientSortingOptions(
                    sortBy = queryParams.mustGet("sortBy"),
                    sortReverse = queryParams.mustGet("sortReverse"),
                )

                val clientsPage = clientRepository.findPaged(
                    filtering = filtering,
                    paging = paging,
                    sorting = sorting
                ).await()

                ctx.response().endWithJson(clientsPage)
            }

        get("/cursor/get")
            .coroutineHandler(context.scope) { ctx ->
                val queryParams = QueryParamValues.parse(ctx, FIND_CLIENTS_BY_CURSOR_QUERY_PARAMS)
                val filtering = ClientFilteringOptions(
                    gender = queryParams.get("filter[gender]"),
                    firstName = queryParams.get("filter[firstName]"),
                    lastName = queryParams.get("filter[lastName]"),
                    address = queryParams.get("filter[address]"),
                    phoneNumber = queryParams.get("filter[phoneNumber]"),
                    email = queryParams.get("filter[email]"),
                    bornAfter = queryParams.get("filter[bornAfter]"),
                    bornBefore = queryParams.get("filter[bornBefore]"),
                    creditCard = queryParams.get("filter[creditCard]")
                )
                val cursorOptions = ClientCursorOptions(
                    cursor = queryParams.mustGet("cursor"),
                    limit = queryParams.mustGet("limit"),
                )

                val clientsCursor = clientRepository.findByCursor(
                    filtering = filtering,
                    cursorOptions = cursorOptions
                ).await()

                ctx.response().endWithJson(clientsCursor)
            }

        get("/events")
            .coroutineHandler(context.scope) { ctx ->
                try {
                    val websocket = ctx.request().toWebSocket().await()
                    websocketApi.handle(websocket)
                } catch (e: Exception) {
                    // ignore
                }
            }

        get("/:id")
            .coroutineHandler(context.scope) { ctx ->
                val id = ctx.pathParam("id")

                val client = clientRepository.findById(id).await()
                if (client != null) {
                    ctx.response().endWithJson(client)
                } else {
                    ctx.response().setStatusCode(404).endWithJson(
                        StatusDTO(
                            status = "error",
                            message = "client not found"
                        )
                    )
                }
            }

        post("/")
            .handler { ctx -> authorizationMiddleware.authorize(ctx, allowedRoles = setOf("CLIENTS_EDITOR")) }
            .coroutineHandler(context.scope) { ctx ->
                val account = authorizationMiddleware.getActiveSession(ctx).account

                ctx.handleJsonBody(ClientAddPayload::class.java) { payload ->
                    val client = clientRepository.add(payload).await()

                    clientEventsPublisher.publish(
                        context.vertx,
                        ClientEvent(
                            operation = ClientEventOperation.ADDED,
                            clientId = client.id.toString(),
                            author = account.id.toString()
                        )
                    )

                    ctx.response().endWithJson(ClientAddResponse(id = client.id.toString()))
                }
            }

        put("/:id")
            .handler { ctx -> authorizationMiddleware.authorize(ctx, allowedRoles = setOf("CLIENTS_EDITOR")) }
            .coroutineHandler(context.scope) { ctx ->
                val account = authorizationMiddleware.getActiveSession(ctx).account

                ctx.handleJsonBody(ClientUpdatePayload::class.java) { payload ->
                    val id = ctx.pathParam("id")

                    val client = clientRepository.update(id, payload).await()
                    if (client != null) {
                        clientEventsPublisher.publish(
                            context.vertx,
                            ClientEvent(
                                operation = ClientEventOperation.UPDATED,
                                clientId = client.id.toString(),
                                author = account.id.toString()
                            )
                        )

                        ctx.response().endWithJson(
                            StatusDTO(
                                status = "ok"
                            )
                        )
                    } else {
                        ctx.response().setStatusCode(404).endWithJson(
                            StatusDTO(
                                status = "error",
                                message = "client not found"
                            )
                        )
                    }
                }
            }

        delete("/:id")
            .handler { ctx -> authorizationMiddleware.authorize(ctx, allowedRoles = setOf("CLIENTS_EDITOR")) }
            .coroutineHandler(context.scope) { ctx ->
                val id = ctx.pathParam("id")

                val account = authorizationMiddleware.getActiveSession(ctx).account

                val deleted = clientRepository.delete(id).await()
                if (deleted) {
                    clientEventsPublisher.publish(
                        context.vertx,
                        ClientEvent(
                            operation = ClientEventOperation.DELETED,
                            clientId = id,
                            author = account.id.toString()
                        )
                    )

                    ctx.response().endWithJson(
                        StatusDTO(
                            status = "ok"
                        )
                    )
                } else {
                    ctx.response().setStatusCode(404).endWithJson(
                        StatusDTO(
                            status = "error",
                            message = "client not found"
                        )
                    )
                }
            }
    }
}
