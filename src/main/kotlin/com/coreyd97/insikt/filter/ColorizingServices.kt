package com.coreyd97.insikt.filter

import com.coreyd97.insikt.logging.logentry.LogEntry
import com.coreyd97.insikt.util.PREF_COLOR_FILTERS
import com.coreyd97.insikt.util.PREF_TAG_FILTERS
import com.coreyd97.montoyautilities.Preference
import com.google.inject.Inject
import com.google.inject.Singleton
import org.apache.logging.log4j.LogManager
import java.awt.Color

interface ColorRuleRepository {
    fun test(filter: ColorizingRule, logEntry: LogEntry): Boolean

    fun add(filter: ColorizingRule)
    fun remove(filter: ColorizingRule)
    fun librarySnippetUpdated(rule: FilterRule)
    fun preferenceDelegate(): Preference<MutableList<ColorizingRule>>
    fun addListener(listener: ColorizingRuleListener)
    fun removeListener(listener: ColorizingRuleListener)
}

abstract class ColorizingService(val filterLibrary: FilterLibrary): ColorRuleRepository {
    val listeners = mutableListOf<ColorizingRuleListener>()
    val preference = preferenceDelegate()
    private val data by preference

    val entries get() = data.toList()

    init {
        filterLibrary.addListener(object : FilterLibraryListenerAdapter() {
            override fun onFilterModified(filterRule: FilterRule, index: Int) {
                librarySnippetUpdated(filterRule)
            }
        })
    }

    override fun test(filter: ColorizingRule, logEntry: LogEntry): Boolean {
        if(!filter.isValid) return false
        return FilterEvaluationVisitor(filterLibrary).visit(filter.expression!!.astExpression, logEntry)
    }

    override fun add(filter: ColorizingRule) {
        data.add(filter)
    }

    override fun remove(filter: ColorizingRule) {
        data.remove(filter)
    }

    override fun addListener(listener: ColorizingRuleListener) {
        listeners.add(listener)
    }

    override fun removeListener(listener: ColorizingRuleListener) {
        listeners.remove(listener)
    }
}

@Singleton
class TableColorService @Inject constructor(
    filterLibrary: FilterLibrary
): ColorizingService(filterLibrary) {
    val log = LogManager.getLogger(TableColorService::class.java)

    override fun preferenceDelegate(): Preference<MutableList<ColorizingRule>> {
        return Preference(PREF_COLOR_FILTERS, mutableListOf()) //todo load from DEFAULT_COLOR_FILTERS_JSON
    }

    fun testAndUpdateEntry(rule: ColorizingRule, logEntry: LogEntry, retest: Boolean): Boolean {
        if(!rule.isValid) {
            logEntry.matchingColorFilters.remove(rule)
        }

        // If we don't already know if the color filter matches (e.g. haven't checked it
        // before)
        if (!logEntry.matchingColorFilters.contains(rule)) {
            if (test(rule, logEntry)) {
                logEntry.matchingColorFilters.add(rule)
                return true
            } else {
                return false
            }
        } else if (retest) { // Or if we are forcing a retest (e.g. filter was updated)
            if (!test(rule, logEntry)) {
                logEntry.matchingColorFilters.remove(rule)
            }
            return true
        } else {
            return false
        }
    }

    fun addColorFilter(title: String = "",
                       expression: String = "",
                       foreground: Color? = Color.BLACK,
                       background: Color? = Color.WHITE) {
        val rule = ColorizingRule.fromString(title, expression)
        rule.foregroundColor = foreground
        rule.backgroundColor = background
        this.addColorFilter(rule)
    }

    fun addColorFilter(
        title: String = "",
        filter: FilterExpression,
        foreground: Color? = Color.BLACK,
        background: Color? = Color.WHITE
    ) {
        addColorFilter(title, filter.astExpression.toString(), foreground, background)
    }

    fun addColorFilter(tableColorRule: ColorizingRule) {
        add(tableColorRule)

        for (colorFilterListener in this.listeners) {
            try {
                colorFilterListener.onAdd(tableColorRule)
            } catch (e: Exception) {
                log.error(e)
            }
        }
    }

    fun removeColorFilter(tableColorRule: ColorizingRule) {
        remove(tableColorRule)
        for (listener in this.listeners) {
            try {
                listener.onRemove(tableColorRule)
            } catch (e: Exception) {
                log.error(e)
            }
        }
    }

    //Called when a filter is modified.
    fun updateColorFilter(tableColorRule: ColorizingRule) {
        for (listener in this.listeners) {
            try {
                listener.onExpressionChange(tableColorRule)
            } catch (e: Exception) {
                log.error(e)
            }
        }
    }

    fun attributeChanged(tableColorRule: ColorizingRule) {
        for (listener in this.listeners) {
            try{
                listener.onAttributeChange(tableColorRule)
            }catch (e: Exception){
                log.error(e)
            }
        }
    }


    fun updateColorFilters(colorRules: List<ColorizingRule>) {
        //This is a full overwrite of the existing rules.
        //We must see which tags have been removed, changed, or added and handle each accordingly.
        val removed = entries.filter { existing -> colorRules.none { it.uuid == existing.uuid } }
        val added = colorRules.filter { new -> this.entries.none { it.uuid == new.uuid } }
        val expressionModified = colorRules
            .filterNot { removed.contains(it) || added.contains(it) }
            .filter { it.shouldRetest }
        val attributeModified =
            colorRules.filterNot { removed.contains(it) || added.contains(it) || expressionModified.contains(it) }
                .filter {
                    val original = this.entries.find { original -> it.uuid == original.uuid } ?: return@filter true
                    original.foregroundColor != it.foregroundColor
                            || original.backgroundColor != it.backgroundColor
                            || original.name != it.name
                            || original.priority != it.priority
                            || original.enabled != it.enabled
                }

        removed.forEach { rule ->
            val original = this.entries.find { it.uuid == rule.uuid } ?: return@forEach
            removeColorFilter(original)
        }

        expressionModified.forEach { rule ->
            val original = this.entries.find { it.uuid == rule.uuid } ?: return@forEach
            original.expression = rule.expression
            updateColorFilter(original)
        }

        attributeModified.forEach { rule ->
            val original = this.entries.find { it.uuid == rule.uuid } ?: return@forEach
            original.foregroundColor = rule.foregroundColor
            original.backgroundColor = rule.backgroundColor
            original.name = rule.name
            original.priority = rule.priority
            original.enabled = rule.enabled
            //TODO replace with
            attributeChanged(original)
        }

        added.forEach { rule ->
            addColorFilter(rule)
        }
    }

    override fun librarySnippetUpdated(rule: FilterRule) {
        val snippet = rule.name
        for (tableColorRule in entries) {
            if (tableColorRule.expression?.snippetDependencies?.contains(snippet) ?: false) {
                updateColorFilter(tableColorRule)
            }
        }
    }

}

@Singleton
class TagService @Inject constructor(
    filterLibrary: FilterLibrary
): ColorizingService(filterLibrary) {

    override fun preferenceDelegate(): Preference<MutableList<ColorizingRule>> {
        return Preference(PREF_TAG_FILTERS, mutableListOf()) //todo load defaults
    }

    override fun librarySnippetUpdated(rule: FilterRule) {
        val snippet = rule.name
        for (tag in entries) {
            if (tag.expression!!.snippetDependencies.contains(snippet)) {
                tagExpressionModified(tag)
            }
        }
    }

    fun testAndUpdateEntry(rule: ColorizingRule, logEntry: LogEntry, retest: Boolean): Boolean {
        if(!rule.isValid){
            return logEntry.matchingTags.remove(rule)
        }

        // If we don't already know if the color filter matches (e.g. haven't checked it
        // before)
        if (!logEntry.matchingTags.contains(rule)) {
            if (test(rule, logEntry)) {
                logEntry.matchingTags.add(rule)
                return true
            } else {
                return false
            }
        } else if (retest) { // Or if we are forcing a retest (e.g. filter was updated)
            if (!test(rule, logEntry)) {
                logEntry.matchingTags.remove(rule)
            }
            return true
        } else {
            return false
        }
    }

    fun addTag(tag: ColorizingRule) {
        add(tag)

        for (listener in this.listeners) {
            try {
                listener.onAdd(tag)
            } catch (error: Exception) {
//                log.error(error)
            }
        }
    }

    fun removeTag(tag: ColorizingRule) {
        remove(tag)

        for (listener in this.listeners) {
            try {
                listener.onRemove(tag)
            } catch (error: Exception) {
//                log.error(error)
            }
        }
    }

    //Called when a filter is modified.
    internal fun tagExpressionModified(tag: ColorizingRule?) {
        for (listener in this.listeners) {
            try {
                listener.onExpressionChange(tag!!)
            } catch (e: Exception) {
//                log.error(e)
            }
        }
    }

    private fun tagAttributesModified(tag: ColorizingRule?) {
        for (listener in this.listeners) {
            try {
                listener.onAttributeChange(tag!!)
            } catch (e: Exception) {
//                log.error(e)
            }
        }
    }


    fun updateTags(tags: List<ColorizingRule>) {
        //This is a full overwrite of the existing tags.
        //We must see which tags have been removed, changed, or added and handle each accordingly.
        val removedTags = this.entries.filter { existingTag -> tags.none { it.uuid == existingTag.uuid } }
        val addedTags = tags.filter { newTag -> this.entries.none { it.uuid == newTag.uuid } }
        val expressionModified = tags
            .filterNot { removedTags.contains(it) || addedTags.contains(it) }
            .filter { it.shouldRetest }
        val attributeModified =
            tags.filterNot { removedTags.contains(it) || addedTags.contains(it) || expressionModified.contains(it) }
                .filter {
                    val original = this.entries.find { original -> it.uuid == original.uuid } ?: return@filter true
                    original.foregroundColor != it.foregroundColor
                            || original.backgroundColor != it.backgroundColor
                            || original.name != it.name
                            || original.priority != it.priority
                            || original.enabled != it.enabled
                }

        removedTags.forEach { tag ->
            val original = this.entries.find { it.uuid == tag.uuid } ?: return@forEach
            removeTag(original)
        }

        expressionModified.forEach { tag ->
            val original = this.entries.find { it.uuid == tag.uuid } ?: return@forEach
            original.expression = tag.expression
            tagExpressionModified(original)
        }

        attributeModified.forEach { tag ->
            val original = this.entries.find { it.uuid == tag.uuid } ?: return@forEach
            original.foregroundColor = tag.foregroundColor
            original.backgroundColor = tag.backgroundColor
            original.name = tag.name
            original.priority = tag.priority
            original.enabled = tag.enabled
            tagAttributesModified(original)
        }

        addedTags.forEach { tag ->
            addTag(tag)
        }
    }
}