package com.github.mkorman9.vertx

import dev.misfitlabs.kotlinguice4.KotlinModule
import io.mockk.mockk
import io.vertx.config.ConfigRetriever
import io.vertx.config.ConfigRetrieverOptions
import io.vertx.config.ConfigStoreOptions
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import org.hibernate.reactive.mutiny.Mutiny.SessionFactory
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator
import javax.validation.Validation
import javax.validation.Validator

class TestModuleBase(
    private val vertx: Vertx
) : KotlinModule() {
    override fun configure() {
        bind<SessionFactory>().toInstance(mockk())
        bind<Validator>().toInstance(createBeanValidator())
        bind<ConfigRetriever>().toInstance(createConfigRetriever())
    }

    private fun createBeanValidator(): Validator {
        return Validation.byDefaultProvider()
            .configure()
            .messageInterpolator(ParameterMessageInterpolator())
            .buildValidatorFactory()
            .validator
    }

    private fun createConfigRetriever(): ConfigRetriever {
        val config = JsonObject().apply {
            put("server",
                JsonObject()
                    .put("host", "127.0.0.1")
                    .put("port", 8080)
            )
        }

        val store = ConfigStoreOptions()
            .setType("json")
            .setConfig(config)
        return ConfigRetriever.create(vertx, ConfigRetrieverOptions().addStore(store))
    }
}
