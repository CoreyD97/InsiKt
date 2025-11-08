package com.coreyd97.insikt.filter

enum class ComparisonOperator(@JvmField val label: String) {
    EQUAL("=="),
    NOT_EQUAL("!="),
    GREATER_THAN(">"),
    LESS_THAN("<"),
    GREATER_THAN_EQUAL(">="),
    LESS_THAN_EQUAL("<="),
    CONTAINS("CONTAINS"),
    IN("IN"),
    MATCHES("MATCHES");

    override fun toString(): String {
        return label
    }
}
