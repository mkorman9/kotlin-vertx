package com.github.mkorman9.vertx.tools.redis

import com.github.mkorman9.vertx.utils.Config
import com.github.mkorman9.vertx.utils.get
import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.redis.client.Redis
import io.vertx.redis.client.RedisAPI
import io.vertx.redis.client.RedisOptions

class RedisInitializer {
    companion object {
        fun initialize(vertx: Vertx, config: Config): Future<RedisAPI> {
            val uri = config.get<String>("redis.uri") ?: throw RuntimeException("redis.uri is missing from config")
            val password = config.get<String>("redis.password")

            val options = RedisOptions()
            options.setConnectionString(uri)

            if (password != null) {
                options.password = password
            }

            return Redis.createClient(vertx, options)
                .connect()
                .map { connection -> RedisAPI.api(connection) }
        }
    }
}