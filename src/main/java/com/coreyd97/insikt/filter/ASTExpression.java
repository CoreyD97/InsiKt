package com.coreyd97.insikt.filter;

import com.coreyd97.insikt.filter.LogicalOperator;

public
class ASTExpression extends SimpleNode {

  boolean inverse = false;
  LogicalOperator op;

  public ASTExpression(int id) {
    super(id);
  }

  public ASTExpression(FilterParser p, int id) {
    super(p, id);
  }

  @Override
  public String getFilterString() {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < this.children.length; i++) {
      String childString = this.children[i].getFilterString();
      if (this.children[i] instanceof ASTExpression
          && !((ASTExpression) this.children[i]).inverse) {
        sb.append("(" + childString + ")");
      } else {
        sb.append(childString);
      }

      if (i != this.children.length - 1) {
        sb.append(" " + op.getLabel() + " ");
      }
    }

    if (inverse) {
      return "!(" + sb + ")";
    } else {
      return sb.toString();
    }
  }

  public LogicalOperator getLogicalOperator() {
    return op;
  }

  public void addCondition(ASTExpression comparison) {
    jjtAddChild(comparison, this.jjtGetNumChildren());
  }

  public void addCondition(ASTComparison comparison) {
    jjtAddChild(comparison, this.jjtGetNumChildren());
  }

  @Override
  public String toString() {
    return String.format("ASTExpression[inverse=%s, op=%s]", inverse, op);
  }

  /**
   * Accept the visitor.
   **/
  public Object jjtAccept(FilterParserVisitor visitor, VisitorData data) {
    return visitor.visit(this, data);
  }
}