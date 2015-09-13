package lila.search

case class Index(name: String)

case class Id(value: String)

case class StringQuery(value: String)
case class From(value: Int)
case class Size(value: Int)

case class SearchResponse(ids: List[String])

object SearchResponse {
  def apply(txt: String): SearchResponse = SearchResponse(txt.split(',').toList)
}

case class CountResponse(count: Int)

object CountResponse {
  def apply(txt: String): CountResponse = CountResponse(~parseIntOption(txt))
}
