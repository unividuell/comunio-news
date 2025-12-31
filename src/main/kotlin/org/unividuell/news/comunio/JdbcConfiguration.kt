package org.unividuell.news.comunio

import org.h2.value.ValueJson
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.convert.TypeDescriptor
import org.springframework.core.convert.converter.Converter
import org.springframework.core.convert.converter.GenericConverter
import org.springframework.data.convert.ReadingConverter
import org.springframework.data.convert.WritingConverter
import org.springframework.data.jdbc.core.convert.JdbcCustomConversions
import org.springframework.transaction.annotation.EnableTransactionManagement
import tools.jackson.databind.ObjectMapper
import java.util.*

@Configuration
@EnableTransactionManagement
class JdbcConfiguration {
    @Bean
    fun applicationJdbcCustomConverters(
        moduleConverters: List<List<Converter<*, *>>>,
        moduleGenericConverters: List<List<GenericConverter>>,
    ): JdbcCustomConversions {
        val converters = moduleConverters.flatten() + moduleGenericConverters.flatten()
        return JdbcCustomConversions(converters)
    }
}

// kudos: https://github.com/coding-jj/json-db-converter
@WritingConverter
class H2JsonWritingConverter<S>(
    private val sourceClazz: Class<S>,
    private val applicationContext: ApplicationContext
) : GenericConverter {

    // Don't initialize ObjectMapper on @Configuration Class, so use ApplicationContext to retrieve ObjectMapper
    private val objectMapper: ObjectMapper by lazy { applicationContext.getBean(ObjectMapper::class.java) }

    override fun getConvertibleTypes(): MutableSet<GenericConverter.ConvertiblePair>? =
        Collections.singleton(GenericConverter.ConvertiblePair(sourceClazz, ValueJson::class.java))

    override fun convert(source: Any?, sourceType: TypeDescriptor, targetType: TypeDescriptor): Any? {
        val sourceObject = sourceClazz.cast(source)
        val json = objectMapper.writeValueAsString(sourceObject)
        return ValueJson.fromJson(json)
    }
}

// kudos: https://github.com/coding-jj/json-db-converter
@ReadingConverter
class H2JsonReadingConverter<T>(
    private val targetClazz: Class<T>,
    private val applicationContext: ApplicationContext
) : GenericConverter {

    // Don't initialize ObjectMapper on @Configuration Class, so use ApplicationContext to retrieve ObjectMapper
    private val objectMapper: ObjectMapper by lazy { applicationContext.getBean(ObjectMapper::class.java) }

    override fun getConvertibleTypes(): MutableSet<GenericConverter.ConvertiblePair>? =
        Collections.singleton(GenericConverter.ConvertiblePair(ByteArray::class.java, targetClazz))

    override fun convert(source: Any?, sourceType: TypeDescriptor, targetType: TypeDescriptor): Any? {
        val byteArray = ByteArray::class.java.cast(source)
        val valueJson = ValueJson.fromJson(byteArray)
        return objectMapper.readValue(valueJson.string, targetClazz)
    }
}