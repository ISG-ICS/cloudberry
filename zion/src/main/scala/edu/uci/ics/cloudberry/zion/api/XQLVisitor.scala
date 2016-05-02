package edu.uci.ics.cloudberry.zion.api

trait XQLVisitor {
  def visit(query: DBQuery): Unit
}

trait XQLVisitable {
  def accept(visitor: XQLVisitor)
}
