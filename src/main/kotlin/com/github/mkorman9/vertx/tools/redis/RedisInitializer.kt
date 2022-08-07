package com.github.mkorman9.vertx.tools.redis

import com.github.mkorman9.vertx.utils.Config
import com.github.mkorman9.vertx.utils.get
import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.redis.client.Redis
import io.vertx.redis.client.RedisAPI
import io.vertx.redis.client.RedisOptions

object RedisInitializer {
    fun initialize(vertx: Vertx, config: Config): Future<RedisAPI> {
        val uri = config.get<String>("REDIS_URI") ?: throw RuntimeException("REDIS_URI is missing from config")
        val password = config.get<String>("REDIS_PASSWORD")
        val poolSize = config.get<Int>("REDIS_POOL_SIZE") ?: 6
        val poolWaiting = config.get<Int>("REDIS_POOL_WAITING") ?: 24
        val poolCleanerInterval = config.get<Int>("REDIS_POOL_CLEANER_INTERVAL") ?: 30_000
        val poolRecycleTimeout = config.get<Int>("REDIS_POOL_RECYCLE_TIMEOUT") ?: 180_000

        val options = RedisOptions()
        options.setConnectionString(uri)
        options.maxPoolSize = poolSize
        options.maxPoolWaiting = poolWaiting
        options.poolCleanerInterval = poolCleanerInterval
        options.poolRecycleTimeout = poolRecycleTimeout

        if (password != null) {
            options.password = password
        }

        return Redis.createClient(vertx, options)
            .connect()
            .map { connection -> RedisAPI.api(connection) }
    }
}
