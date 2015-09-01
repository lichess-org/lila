package lila.search

trait SearchReadApi[A, Q <: Query] {

  def search(query: Q, from: From, size: Size): Fu[List[A]]

  def count(query: Q): Fu[Int]
}
