package com.coreyd97.insikt

import com.coreyd97.insikt.util.PREF_LOG_LEVEL
import com.coreyd97.montoyautilities.Preference
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
class LoggingController {
    private val levelDelegate = Preference(PREF_LOG_LEVEL, Level.INFO, serializer = LevelSerializer())
    private var _logLevel by levelDelegate

    init {
//        levelDelegate.addListener { _, new ->
//            setLogLevel(new)
//        }
//        if (Insikt.montoya.extension().filename() == null) { //Loaded from classpath. Log to console!
            val context = LogManager.getContext(false) as LoggerContext
            val appenderBuilder = ConsoleAppender.Builder()
            appenderBuilder.name = "ConsoleAppender"
            val consoleAppender = appenderBuilder.build()
            consoleAppender.start()
            context.rootLogger.addAppender(consoleAppender)
//        }
        setLogLevel(_logLevel)
    }

    fun setLogLevel(logLevel: Level) {
        this._logLevel = logLevel
        val context = LogManager.getContext(false) as LoggerContext
        context.configuration.rootLogger.level = logLevel
        context.updateLoggers()
    }
}
