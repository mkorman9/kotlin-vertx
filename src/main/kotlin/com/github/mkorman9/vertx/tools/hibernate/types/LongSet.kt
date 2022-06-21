package com.github.mkorman9.vertx.tools.hibernate.types

import org.hibernate.engine.spi.SharedSessionContractImplementor
import org.hibernate.usertype.UserType
import java.io.Serializable
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Types
import java.util.*

class LongSet : UserType {
    override fun sqlTypes(): IntArray {
        return intArrayOf(Types.OTHER)
    }

    override fun returnedClass(): Class<*> {
        return MutableSet::class.java
    }

    override fun equals(x: Any, y: Any): Boolean {
        return Objects.deepEquals(x, y)
    }

    override fun hashCode(value: Any): Int {
        @Suppress("UNCHECKED_CAST")
        return (value as MutableSet<Long>).toTypedArray().contentHashCode()
    }

    override fun nullSafeGet(
        rs: ResultSet,
        names: Array<String>,
        session: SharedSessionContractImplementor,
        owner: Any?
    ): Any {
        @Suppress("UNCHECKED_CAST")
        return (rs.getObject(names[0]) as Array<Long>).toMutableSet()
    }

    override fun nullSafeSet(
        st: PreparedStatement,
        value: Any?,
        index: Int,
        session: SharedSessionContractImplementor
    ) {
        if (value == null) {
            st.setNull(index, Types.OTHER)
        } else {
            @Suppress("UNCHECKED_CAST")
            st.setObject(index, (value as MutableSet<Long>).toTypedArray())
        }
    }

    override fun deepCopy(value: Any): Any {
        @Suppress("UNCHECKED_CAST")
        return (value as MutableSet<Long>).toMutableSet()
    }

    override fun isMutable(): Boolean {
        return false
    }

    override fun disassemble(value: Any): Serializable {
        @Suppress("UNCHECKED_CAST")
        return (value as Set<Long>).toTypedArray()
    }

    override fun assemble(cached: Serializable, owner: Any?): Any {
        @Suppress("UNCHECKED_CAST")
        return (cached as Array<Long>).toMutableSet()
    }

    override fun replace(original: Any, target: Any?, owner: Any?): Any {
        return deepCopy(original)
    }
}
