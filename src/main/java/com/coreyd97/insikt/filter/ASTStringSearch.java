package com.coreyd97.insikt.filter;

public
class ASTStringSearch extends SimpleNode {

  public String searchValue;

  public ASTStringSearch(int id) {
    super(id);
  }

  public ASTStringSearch(FilterParser p, int id) {
    super(p, id);
  }

  @Override
  public String getFilterString() {
    return String.format("\"%s\"", searchValue);
  }

  @Override
  public String toString() {
    return String.format("StringSearch[value=%s]", searchValue);
  }

  /**
   * Accept the visitor.
   **/
  public Object jjtAccept(FilterParserVisitor visitor, VisitorData data) {

    return
        visitor.visit(this, data);
  }
}