package com.github.mkorman9.vertx.client

import com.github.mkorman9.vertx.protocol.Client
import com.github.mkorman9.vertx.protocol.ClientRequest
import com.github.mkorman9.vertx.protocol.ClientServiceGrpcKt
import com.github.mkorman9.vertx.protocol.CreditCard
import com.google.inject.Injector
import com.google.protobuf.Timestamp
import dev.misfitlabs.kotlinguice4.getInstance
import io.vertx.kotlin.coroutines.await
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.LocalDateTime
import java.time.ZoneOffset

class ClientServiceGrpcImpl (
    injector: Injector
) : ClientServiceGrpcKt.ClientServiceCoroutineImplBase() {
    private val clientRepository: ClientRepository = injector.getInstance()

    override fun getClients(request: ClientRequest): Flow<Client> {
        return flow {
            val clientsPage = clientRepository.findByCursor(
                ClientFilteringOptions(),
                ClientCursorOptions(cursor = null, limit = 10)
            ).await()

            clientsPage.data
                .map {
                    Client.newBuilder()
                        .setId(it.id.toString())
                        .setGender(it.gender)
                        .setFirstName(it.firstName)
                        .setLastName(it.lastName)
                        .setAddress(it.address)
                        .setPhoneNumber(it.phoneNumber)
                        .setEmail(it.email)
                        .setBirthDate(toTimestamp(it.birthDate))
                        .addAllCreditCards(it.creditCards.map { cc ->
                            CreditCard.newBuilder()
                                .setNumber(cc.number)
                                .build()
                        })
                        .build()
                }
                .forEach {
                    emit(it)
                }
        }
    }

    private fun toTimestamp(dateTime: LocalDateTime?): Timestamp? {
        if (dateTime == null) {
            return null
        }

        val instant = dateTime.toInstant(ZoneOffset.UTC)
        return Timestamp.newBuilder()
            .setSeconds(instant.epochSecond)
            .setNanos(instant.nano)
            .build()
    }
}
