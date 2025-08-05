package lila.fide

case class FideSearch(order: FideSearch.Order)

object FideSearch:
  enum Order:
    case Standard, Rapid, Blitz, Age
  val default = FideSearch(Order.Standard)
  val orders = Order.values
