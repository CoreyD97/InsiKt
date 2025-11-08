package com.coreyd97.insikt.filter;

import com.coreyd97.insikt.filter.ComparisonOperator;
import com.coreyd97.insikt.filter.FilterRule;
import com.coreyd97.insikt.logging.logentry.LogEntryField;
import com.coreyd97.insikt.logging.LogProcessor;
import com.coreyd97.insikt.util.MoreHelpKt;
import java.util.Date;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.commons.text.StringEscapeUtils;

public
class ASTComparison extends SimpleNode {

  Object left, right;
  ComparisonOperator comparisonOperator;

  public ASTComparison(int id) {
    super(id);
  }

  public ASTComparison(FilterParser p, int id) {
    super(p, id);
  }

  /**
   * Accept the visitor.
   **/
  public Object jjtAccept(FilterParserVisitor visitor, VisitorData data) {

    return visitor.visit(this, data);
  }

  public ComparisonOperator getComparisonOperator() {
    return comparisonOperator;
  }

  public Object getLeft() {
    return left;
  }

  public Object getRight() {
    return right;
  }

  @Override
  public String toString() {
    Class<?> leftClass =
        left instanceof LogEntryField ? ((LogEntryField) left).getType() : left.getClass();
    Class<?> rightClass =
        right instanceof LogEntryField ? ((LogEntryField) right).getType() : right.getClass();
    return String.format("ASTComparison[left=%s (%s), op=%s, right=%s (%s)]", left, leftClass,
        comparisonOperator, right, rightClass);
  }

  @Override
  public String getFilterString() {
    return String.format("%s %s %s", convertObjectToString(left), comparisonOperator.label,
        convertObjectToString(right));
  }

  private String convertObjectToString(Object obj) {
    if (obj instanceof Pattern) {
      if (comparisonOperator == ComparisonOperator.MATCHES) {
        return "\"" + obj + "\"";
      } else {
        return "/" + obj + "/";
      }
    } else if (obj instanceof String) {
      return "\"" + StringEscapeUtils.escapeJava((String) obj) + "\"";
    } else if (obj instanceof Set) {
      StringBuilder sb = new StringBuilder();
      sb.append("[");
      sb.append(((Set) obj).stream().map(item -> {
        if (item instanceof String) {
          return "\"" + item + "\"";
        } else {
          return String.valueOf(item);
        }
      }).collect(Collectors.joining(", ")));
      sb.append("]");
      return sb.toString();
    } else if (obj instanceof Date) {
      return "\"" + MoreHelpKt.getLoggerDateFormat().format(obj) + "\"";
    } else if (obj instanceof FilterRule){
      return ((FilterRule) obj).getName();
    }
    return String.valueOf(obj);
  }

}