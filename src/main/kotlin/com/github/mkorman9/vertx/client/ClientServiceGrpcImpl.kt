package com.github.mkorman9.vertx.client

import com.github.mkorman9.vertx.protocol.Client
import com.github.mkorman9.vertx.protocol.CreditCard
import com.github.mkorman9.vertx.protocol.ClientRequest
import com.github.mkorman9.vertx.protocol.ClientServiceGrpcKt
import com.google.protobuf.Timestamp
import io.vertx.kotlin.coroutines.await
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.LocalDateTime
import java.time.ZoneOffset
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ClientServiceGrpcImpl @Inject constructor(
    private val clientRepository: ClientRepository
) : ClientServiceGrpcKt.ClientServiceCoroutineImplBase() {
    override fun getClients(request: ClientRequest): Flow<Client> {
        return flow {
            val clientsPage = clientRepository.findPaged(
                ClientsFilteringOptions(),
                ClientsPagingOptions(1, 10),
                ClientsSortingOptions("id", false)
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
                        .addAllCreditCards(it.creditCards.map {
                            CreditCard.newBuilder()
                                .setNumber(it.number)
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
