package com.github.mkorman9.vertx.tools.aws

import com.amazonaws.AmazonWebServiceRequest
import com.amazonaws.ClientConfiguration
import com.amazonaws.Protocol
import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.client.builder.AwsClientBuilder
import com.amazonaws.handlers.AsyncHandler
import com.amazonaws.regions.Regions
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBAsync
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBAsyncClientBuilder
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper
import com.amazonaws.services.dynamodbv2.model.*
import com.amazonaws.waiters.WaiterHandler
import com.amazonaws.waiters.WaiterParameters
import com.github.mkorman9.vertx.utils.Config
import com.github.mkorman9.vertx.utils.get
import io.vertx.core.Future
import io.vertx.core.Promise

class DynamoDBClient private constructor(
    config: Config
) {
    companion object {
        fun create(config: Config): DynamoDBClient {
            return DynamoDBClient(config)
        }
    }

    val client: AmazonDynamoDBAsync
    val mapper: DynamoDBMapper

    init {
        val emulatorEnabled = config.get<Boolean>("aws.dynamodb.emulator.enabled") ?: false
        val emulatorAddress = config.get<String>("aws.dynamodb.emulator.address") ?: "localhost:4100"

        if (emulatorEnabled) {
            client = AmazonDynamoDBAsyncClientBuilder.standard()
                .withEndpointConfiguration(
                    AwsClientBuilder.EndpointConfiguration(
                        emulatorAddress,
                        Regions.US_EAST_1.name
                    )
                )
                .withCredentials(
                    AWSStaticCredentialsProvider(
                        BasicAWSCredentials("ACCESS_KEY", "SECRET_ACCESS_KEY")
                    )
                )
                .withClientConfiguration(
                    ClientConfiguration()
                        .withProtocol(Protocol.HTTP)
                )
                .build()
        } else {
            client = AmazonDynamoDBAsyncClientBuilder.defaultClient()
        }

        mapper = DynamoDBMapper(client)
    }

    fun close() {
        client.shutdown()
    }

    inline fun <reified T> createTable(
        billingMode: BillingMode = BillingMode.PAY_PER_REQUEST
    ): Future<Void> {
        val request = mapper.generateCreateTableRequest(T::class.java)

        val createTablePromise = Promise.promise<CreateTableResult>()

        client.createTableAsync(
            request
                .withBillingMode(billingMode),
            createAsyncHandler<CreateTableRequest, CreateTableResult>(createTablePromise)
        )

        val resultPromise = Promise.promise<Void>()

        createTablePromise.future()
            .recover { cause ->
                if (cause.message?.startsWith("Table already exists:") == true) {
                    Future.succeededFuture(null)
                } else {
                    Future.failedFuture(cause)
                }
            }
            .onSuccess {
                waitForTableActive(request.tableName)
                    .onSuccess { resultPromise.complete() }
                    .onFailure { cause -> resultPromise.fail(cause) }
            }
            .onFailure { cause ->
                resultPromise.fail(cause)
            }

        return resultPromise.future()
    }

    inline fun <reified T> getItem(key: Map<String, AttributeValue>): Future<T?> {
        val tableName = mapper.generateCreateTableRequest(T::class.java).tableName

        val promise = Promise.promise<GetItemResult>()

        client.getItemAsync(
            GetItemRequest()
                .withTableName(tableName)
                .withKey(key),
            createAsyncHandler<GetItemRequest, GetItemResult>(promise)
        )

        return promise.future()
            .map { result ->
                result.item
            }
            .map { attributes ->
                if (attributes != null) {
                    mapper.marshallIntoObject(T::class.java, attributes)
                } else {
                    null
                }
            }
    }

    inline fun <reified T> query(queryRequest: QueryRequest): Future<List<T>> {
        val tableName = mapper.generateCreateTableRequest(T::class.java).tableName

        val promise = Promise.promise<QueryResult>()

        client.queryAsync(
            queryRequest
                .withTableName(tableName),
            createAsyncHandler<QueryRequest, QueryResult>(promise)
        )

        return promise.future()
            .map { result ->
                result.items
            }
            .map { items ->
                mapper.marshallIntoObjects(T::class.java, items)
            }
    }

    inline fun <reified T> scan(scanRequest: ScanRequest): Future<List<T>> {
        val tableName = mapper.generateCreateTableRequest(T::class.java).tableName

        val promise = Promise.promise<ScanResult>()

        client.scanAsync(
            scanRequest
                .withTableName(tableName),
            createAsyncHandler<ScanRequest, ScanResult>(promise)
        )

        return promise.future()
            .map { result ->
                result.items
            }
            .map { items ->
                mapper.marshallIntoObjects(T::class.java, items)
            }
    }

    inline fun <reified T> putItem(item: T): Future<PutItemResult> {
        val tableName = mapper.generateCreateTableRequest(T::class.java).tableName
        val tableModel = mapper.getTableModel(T::class.java)
        val attributes = tableModel.convert(item)

        val promise = Promise.promise<PutItemResult>()

        client.putItemAsync(
            PutItemRequest()
                .withTableName(tableName)
                .withItem(attributes),
            createAsyncHandler<PutItemRequest, PutItemResult>(promise)
        )

        return promise.future()
    }

    inline fun <reified T, H, R> updateItem(item: T): Future<UpdateItemResult> {
        val tableName = mapper.generateCreateTableRequest(T::class.java).tableName
        val tableModel = mapper.getTableModel(T::class.java)

        val key = tableModel.convertKey<H, R>(item)
        val fields = tableModel.convert(item)
            .filterNot { (k, _) ->
                key.containsKey(k)
            }

        val promise = Promise.promise<UpdateItemResult>()

        client.updateItemAsync(
            UpdateItemRequest()
                .withTableName(tableName)
                .withKey(key)
                .withAttributeUpdates(fields
                    .mapValues { (_, value) ->
                        AttributeValueUpdate()
                            .withAction("PUT")
                            .withValue(value)
                    }
                ),
            createAsyncHandler<UpdateItemRequest, UpdateItemResult>(promise)
        )

        return promise.future()
    }

    inline fun <reified T> deleteItem(key: Map<String, AttributeValue>): Future<DeleteItemResult> {
        val tableName = mapper.generateCreateTableRequest(T::class.java).tableName

        val promise = Promise.promise<DeleteItemResult>()

        client.deleteItemAsync(
            DeleteItemRequest()
                .withTableName(tableName)
                .withKey(key),
            createAsyncHandler<DeleteItemRequest, DeleteItemResult>(promise)
        )

        return promise.future()
    }

    fun <REQUEST : AmazonWebServiceRequest, RESULT> createAsyncHandler(
        promise: Promise<RESULT>
    ): AsyncHandler<REQUEST, RESULT> {
        return object : AsyncHandler<REQUEST, RESULT> {
            override fun onError(exception: Exception?) {
                promise.fail(exception)
            }

            override fun onSuccess(request: REQUEST, result: RESULT) {
                promise.complete(result)
            }
        }
    }

    fun waitForTableActive(tableName: String): Future<Void> {
        val promise = Promise.promise<Void>()

        client.waiters().tableExists().runAsync(
            WaiterParameters(
                DescribeTableRequest()
                    .withTableName(tableName)
            ),
            object : WaiterHandler<AmazonWebServiceRequest>() {
                override fun onWaitSuccess(request: AmazonWebServiceRequest?) {
                    promise.complete()
                }

                override fun onWaitFailure(e: java.lang.Exception?) {
                    promise.fail(e)
                }
            })

        return promise.future()
    }
}
