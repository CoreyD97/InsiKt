package com.coreyd97.insikt

import burp.api.montoya.MontoyaApi
import com.coreyd97.insikt.util.PREF_LOG_LEVEL
import com.coreyd97.montoyautilities.Preference
import com.google.inject.Inject
import com.google.inject.Singleton
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.LoggerContext
import org.apache.logging.log4j.core.appender.ConsoleAppender

class LevelSerializer : KSerializer<Level> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Level", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: Level) {
        encoder.encodeString(value.name())
    }
    override fun deserialize(decoder: Decoder): Level {
        val levelName = decoder.decodeString()
        return Level.toLevel(levelName, Level.INFO)
    }
}

@Singleton
class LoggingController @Inject constructor(
    val montoyaApi: MontoyaApi
) {
    private val levelDelegate = Preference(PREF_LOG_LEVEL, Level.INFO, serializer = LevelSerializer()) { _, new ->
        setLogLevel(new)
    }
    private var _logLevel by levelDelegate

    init {
//        levelDelegate.addListener { _, new ->
//            setLogLevel(new)
//        }
        if (montoyaApi.extension().filename().contains("InsiKt.jar")) { //Loaded from classpath. Log to console!
            val context = LogManager.getContext(false) as LoggerContext
            val appenderBuilder = ConsoleAppender.Builder()
            appenderBuilder.name = "ConsoleAppender"
            val consoleAppender = appenderBuilder.build()
            consoleAppender.start()
            context.rootLogger.addAppender(consoleAppender)
        }
        setLogLevel(_logLevel)
    }

    private fun setLogLevel(logLevel: Level) {
        val context = LogManager.getContext(false) as LoggerContext
        val config = context.configuration
        config.rootLogger.level = logLevel
        context.updateLoggers()
    }
}
