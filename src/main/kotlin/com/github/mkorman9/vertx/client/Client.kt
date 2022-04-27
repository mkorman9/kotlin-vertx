package com.github.mkorman9.vertx.client

import java.util.UUID
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Entity(name = "Client")
@Table(name = "clients")
data class Client(
    @Id
    @Column(name = "id", columnDefinition = "uuid")
    val id: UUID,

    @Column(name = "first_name")
    val firstName: String,

    @Column(name = "last_name")
    val lastName: String
)
