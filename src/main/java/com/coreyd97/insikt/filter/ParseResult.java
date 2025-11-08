package com.coreyd97.insikt.filter;

import java.util.List;

public record ParseResult(FilterExpression expression, List<String> errors) {
    public boolean valid(){
        return expression != null && errors.isEmpty();
    }
}