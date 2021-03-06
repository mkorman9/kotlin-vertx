package com.github.mkorman9.vertx.tools.aws.dynamodb

import com.amazonaws.AmazonWebServiceRequest
import com.amazonaws.ClientConfiguration
import com.amazonaws.Protocol
import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.client.builder.AwsClientBuilder
import com.amazonaws.regions.Regions
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBAsync
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBAsyncClientBuilder
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper
import com.amazonaws.services.dynamodbv2.model.*
import com.amazonaws.waiters.WaiterHandler
import com.amazonaws.waiters.WaiterParameters
import com.github.mkorman9.vertx.tools.aws.createAsyncHandler
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

    private val client: AmazonDynamoDBAsync
    private val mapper: DynamoDBMapper

    private val tableNamesCache = mutableMapOf<Class<*>, String>()

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

    fun <T : Any> createTable(
        tableClass: Class<T>,
        billingMode: BillingMode = BillingMode.PAY_PER_REQUEST,
        readCapacity: Long = 3000,
        writeCapacity: Long = 1000,
    ): Future<Void> {
        val request = mapper.generateCreateTableRequest(tableClass)
            .withBillingMode(billingMode)

        if (billingMode == BillingMode.PROVISIONED) {
            request
                .withProvisionedThroughput(ProvisionedThroughput(readCapacity, writeCapacity))
        }

        tableNamesCache[tableClass] = request.tableName

        val promise = Promise.promise<CreateTableResult>()

        client.createTableAsync(
            request,
            createAsyncHandler(promise)
        )

        return promise.future()
            .recover { cause ->
                if (cause.message?.startsWith("Table already exists:") == true) {
                    Future.succeededFuture(null)
                } else {
                    Future.failedFuture(cause)
                }
            }
            .compose {
                waitForTableActive(request.tableName)
            }
    }

    fun <T : Any> getItem(
        tableClass: Class<T>,
        key: Map<String, AttributeValue>
    ): Future<T?> {
        val tableName = getTableName(tableClass)

        val promise = Promise.promise<GetItemResult>()

        client.getItemAsync(
            GetItemRequest()
                .withTableName(tableName)
                .withKey(key),
            createAsyncHandler(promise)
        )

        return promise.future()
            .map { result ->
                result.item
            }
            .map { attributes ->
                if (attributes != null) {
                    mapper.marshallIntoObject(tableClass, attributes)
                } else {
                    null
                }
            }
    }

    fun <T : Any> query(
        tableClass: Class<T>,
        queryRequest: QueryRequest
    ): Future<List<T>> {
        val tableName = getTableName(tableClass)

        val promise = Promise.promise<QueryResult>()

        client.queryAsync(
            queryRequest
                .withTableName(tableName),
            createAsyncHandler(promise)
        )

        return promise.future()
            .map { result ->
                result.items
            }
            .map { items ->
                mapper.marshallIntoObjects(tableClass, items)
            }
    }

    fun <T : Any> scan(
        tableClass: Class<T>,
        scanRequest: ScanRequest
    ): Future<List<T>> {
        val tableName = getTableName(tableClass)

        val promise = Promise.promise<ScanResult>()

        client.scanAsync(
            scanRequest
                .withTableName(tableName),
            createAsyncHandler(promise)
        )

        return promise.future()
            .map { result ->
                result.items
            }
            .map { items ->
                mapper.marshallIntoObjects(tableClass, items)
            }
    }

    fun <T : Any> putItem(
        item: T
    ): Future<PutItemResult> {
        val tableName = getTableName(item.javaClass)
        val tableModel = mapper.getTableModel(item.javaClass)
        val attributes = tableModel.convert(item)

        val promise = Promise.promise<PutItemResult>()

        client.putItemAsync(
            PutItemRequest()
                .withTableName(tableName)
                .withItem(attributes),
            createAsyncHandler(promise)
        )

        return promise.future()
    }

    fun <T : Any> updateItem(
        tableClass: Class<T>,
        key: Map<String, AttributeValue>,
        toUpdate: Map<String, AttributeValueUpdate>
    ): Future<UpdateItemResult> {
        val tableName = getTableName(tableClass)

        val promise = Promise.promise<UpdateItemResult>()

        client.updateItemAsync(
            UpdateItemRequest()
                .withTableName(tableName)
                .withKey(key)
                .withAttributeUpdates(toUpdate),
            createAsyncHandler(promise)
        )

        return promise.future()
    }

    fun <T : Any> deleteItem(
        tableClass: Class<T>,
        key: Map<String, AttributeValue>
    ): Future<DeleteItemResult> {
        val tableName = getTableName(tableClass)

        val promise = Promise.promise<DeleteItemResult>()

        client.deleteItemAsync(
            DeleteItemRequest()
                .withTableName(tableName)
                .withKey(key),
            createAsyncHandler(promise)
        )

        return promise.future()
    }

    private fun waitForTableActive(tableName: String): Future<Void> {
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
            }
        )

        return promise.future()
    }

    private fun getTableName(tableClass: Class<*>): String {
        val cachedName = tableNamesCache[tableClass]
        if (cachedName != null) {
            return cachedName
        }

        val tableName = mapper.generateCreateTableRequest(tableClass).tableName
        tableNamesCache[tableClass] = tableName
        return tableName
    }
}
