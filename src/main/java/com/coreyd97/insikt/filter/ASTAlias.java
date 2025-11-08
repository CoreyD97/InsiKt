package com.coreyd97.insikt.filter;

public
class ASTAlias extends SimpleNode {

  public String identifier;
  public ASTExpression filter;

  public ASTAlias(int id) {
    super(id);
  }

  public ASTAlias(FilterParser p, int id) {
    super(p, id);
  }

  @Override
  public String getFilterString() {
    return "#" + identifier;
  }

  @Override
  public String toString() {
    return String.format("ASTAlias[id=%s]", identifier);
  }

  /**
   * Accept the visitor.
   **/
  public Object jjtAccept(FilterParserVisitor visitor, VisitorData data) {

    return
        visitor.visit(this, data);
  }
}