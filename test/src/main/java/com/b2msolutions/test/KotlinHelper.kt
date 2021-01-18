package com.b2msolutions.test

import org.mockito.ArgumentCaptor
import org.mockito.Mockito

abstract class KotlinHelper {
    companion object {
        fun setStaticField(item: Any, propName: String, value: Any?) {
            val field = item::class.java.getDeclaredField(propName)
            field.isAccessible = true
            field.set(item, value)
        }

        fun getStaticField(item: Any, propName: String): Any {
            val field = item::class.java.getDeclaredField(propName)
            field.isAccessible = true
            return field.get(item)
        }

        @Suppress("UNCHECKED_CAST")
        fun <T> uninitialized(): T = null as T

        fun <T> eq(value: T): T {
            return Mockito.eq(value) ?: value ?: uninitialized()
        }

        fun <T> any(type: Class<T>): T {
            return Mockito.any(type) ?: uninitialized()
        }

        fun <T> anyObject(): T {
            return Mockito.any<T>() ?: uninitialized()
        }

        fun <T> capture(argumentCaptor: ArgumentCaptor<T>): T {
            return argumentCaptor.capture() ?: uninitialized()
        }
    }
}