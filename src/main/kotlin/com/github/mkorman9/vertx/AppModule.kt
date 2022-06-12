package com.github.mkorman9.vertx

import com.github.mkorman9.vertx.utils.Config
import com.github.mkorman9.vertx.utils.gcp.GCPPubSubClient
import dev.misfitlabs.kotlinguice4.KotlinModule
import io.vertx.core.Vertx
import org.hibernate.reactive.mutiny.Mutiny.SessionFactory

class AppModule(
    private val vertx: Vertx,
    private val config: Config,
    private val sessionFactory: SessionFactory,
    private val gcpPubSubClient: GCPPubSubClient
) : KotlinModule() {
    companion object {
        const val PACKAGE_NAME = "com.github.mkorman9.vertx"
    }

    override fun configure() {
        bind<Vertx>().toInstance(vertx)
        bind<Config>().toInstance(config)
        bind<SessionFactory>().toInstance(sessionFactory)
        bind<GCPPubSubClient>().toInstance(gcpPubSubClient)
    }
}
