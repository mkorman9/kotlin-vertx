package com.github.mkorman9.vertx.client

import com.github.mkorman9.vertx.utils.web.QueryParamsParsingRules
import java.time.LocalDateTime
import java.time.format.DateTimeParseException

private val allowedGenderFilterValues = hashSetOf(
    "-", "M", "F"
)
private val allowedSortByValues = hashSetOf(
    "id", "gender", "firstName", "lastName", "address", "phoneNumber", "email", "birthDate"
)

val FIND_PAGED_CLIENTS_QUERY_PARAMS: QueryParamsParsingRules = mapOf(
    "page" to { value ->
        var page = try {
            value?.toInt() ?: 1
        } catch (e: NumberFormatException) {
            1
        }

        if (page < 1) {
            page = 1
        }

        page
    },
    "pageSize" to { value ->
        var pageSize = try {
            value?.toInt() ?: 10
        } catch (e: NumberFormatException) {
            10
        }

        if (pageSize < 1) {
            pageSize = 10
        }
        if (pageSize > 100) {
            pageSize = 100
        }

        pageSize
    },
    "sortBy" to { value ->
        if (allowedSortByValues.contains(value)) {
            value
        } else {
            "id"
        }
    },
    "sortReverse" to { value ->
        value != null
    },
    "filter[gender]" to { value ->
        if (allowedGenderFilterValues.contains(value)) {
            value
        } else {
            null
        }
    },
    "filter[firstName]" to { value ->
        value
    },
    "filter[lastName]" to { value ->
        value
    },
    "filter[address]" to { value ->
        value
    },
    "filter[phoneNumber]" to { value ->
        value
    },
    "filter[email]" to { value ->
        value
    },
    "filter[bornAfter]" to { value ->
        var dateTime: LocalDateTime? = null

        if (value != null) {
            dateTime = try {
                LocalDateTime.parse(value)
            } catch (e: DateTimeParseException) {
                null
            }
        }

        dateTime
    },
    "filter[bornBefore]" to { value ->
        var dateTime: LocalDateTime? = null

        if (value != null) {
            dateTime = try {
                LocalDateTime.parse(value)
            } catch (e: DateTimeParseException) {
                null
            }
        }

        dateTime
    },
    "filter[creditCard]" to { value ->
        value
    },
)

val FIND_CLIENTS_BY_CURSOR_QUERY_PARAMS: QueryParamsParsingRules = mapOf(
    "cursor" to { value ->
        value
    },
    "limit" to { value ->
        var limit = try {
            value?.toInt() ?: 10
        } catch (e: NumberFormatException) {
            10
        }

        if (limit < 1) {
            limit = 10
        }
        if (limit > 100) {
            limit = 100
        }

        limit
    },
    "filter[gender]" to { value ->
        if (allowedGenderFilterValues.contains(value)) {
            value
        } else {
            null
        }
    },
    "filter[firstName]" to { value ->
        value
    },
    "filter[lastName]" to { value ->
        value
    },
    "filter[address]" to { value ->
        value
    },
    "filter[phoneNumber]" to { value ->
        value
    },
    "filter[email]" to { value ->
        value
    },
    "filter[bornAfter]" to { value ->
        var dateTime: LocalDateTime? = null

        if (value != null) {
            dateTime = try {
                LocalDateTime.parse(value)
            } catch (e: DateTimeParseException) {
                null
            }
        }

        dateTime
    },
    "filter[bornBefore]" to { value ->
        var dateTime: LocalDateTime? = null

        if (value != null) {
            dateTime = try {
                LocalDateTime.parse(value)
            } catch (e: DateTimeParseException) {
                null
            }
        }

        dateTime
    },
    "filter[creditCard]" to { value ->
        value
    },
)
