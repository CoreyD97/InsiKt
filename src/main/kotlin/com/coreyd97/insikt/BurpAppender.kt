package com.coreyd97.insikt

import burp.api.montoya.MontoyaApi
import com.google.inject.Inject
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.core.Appender
import org.apache.logging.log4j.core.Core
import org.apache.logging.log4j.core.Filter
import org.apache.logging.log4j.core.LogEvent
import org.apache.logging.log4j.core.appender.AbstractAppender
import org.apache.logging.log4j.core.config.plugins.Plugin
import org.apache.logging.log4j.core.config.plugins.PluginAttribute
import org.apache.logging.log4j.core.config.plugins.PluginElement
import org.apache.logging.log4j.core.config.plugins.PluginFactory
import org.apache.logging.log4j.core.layout.PatternLayout

@Plugin(name = "BurpAppender", category = Core.CATEGORY_NAME, elementType = Appender.ELEMENT_TYPE)
class BurpAppender(name: String, filter: Filter?) :
    AbstractAppender(name, filter, PatternLayout.createDefaultLayout(), false, null) {
    override fun append(event: LogEvent) {
        val message = String(this.layout.toByteArray(event))

        if(montoyaApi == null || montoyaApi.extension().filename().contains("InsiKt.jar")){
            if(event.level.isMoreSpecificThan(Level.ERROR)) {
                System.err.println("${event.level} $message")
            }else{
                println("${event.level} $message")
            }
            if(montoyaApi == null) return
        }

        if (event.level.isMoreSpecificThan(Level.INFO)) {
            montoyaApi.logging().logToError(message)
        } else {
            montoyaApi.logging().logToOutput(message)
        }
    }

    companion object {
        private lateinit var montoyaApi: MontoyaApi

        @Inject
        @JvmStatic
        fun setMontoyaApi(montoyaApi: MontoyaApi) {
            this.montoyaApi = montoyaApi
        }

        @PluginFactory
        @JvmStatic
        fun createAppender(
            @PluginAttribute("name") name: String,
            @PluginElement("Filter") filter: Filter?
        ): BurpAppender {
            return BurpAppender(name, filter)
        }
    }
}
