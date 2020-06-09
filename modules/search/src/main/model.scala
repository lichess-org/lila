package lila.search

case class Index(name: String) extends AnyVal

case class Id(value: String) extends AnyVal

case class StringQuery(value: String) extends AnyVal
case class From(value: Int)           extends AnyVal
case class Size(value: Int)           extends AnyVal

case class SearchResponse(ids: List[String]) extends AnyVal

object SearchResponse {
  def apply(txt: String): SearchResponse = SearchResponse(txt.split(',').toList)
}

case class CountResponse(count: Long) extends AnyVal

object CountResponse {
  def apply(txt: String): CountResponse = CountResponse(~txt.toLongOption)
}
