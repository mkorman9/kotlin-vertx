package com.github.mkorman9.vertx.client

import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*

data class ClientDocument(
    val id: String = "",
    val gender: String = "-",
    val firstName: String = "",
    val lastName: String = "",
    val address: String? = null,
    val phoneNumber: String? = null,
    val email: String? = null,
    val birthDate: Long? = null,
    val deleted: Boolean = false,
    val creditCards: List<String> = listOf()
) {
    companion object {
        fun fromClient(client: Client): ClientDocument {
            return ClientDocument(
                id = client.id.toString(),
                gender = client.gender,
                firstName = client.firstName,
                lastName = client.lastName,
                address = client.address,
                phoneNumber = client.phoneNumber,
                email = client.email,
                birthDate = client.birthDate?.toEpochSecond(ZoneOffset.UTC),
                deleted = client.deleted,
                creditCards = client.creditCards.toMutableList(),
            )
        }
    }

    fun toClient(): Client {
        return Client(
            id = UUID.fromString(id),
            gender = gender,
            firstName = firstName,
            lastName = lastName,
            address = address,
            phoneNumber = phoneNumber,
            email = email,
            birthDate =
                if (birthDate != null)
                    LocalDateTime.ofEpochSecond(birthDate, 0, ZoneOffset.UTC)
                else
                    null,
            deleted = deleted,
            creditCards = creditCards.toMutableList(),
        )
    }
}
