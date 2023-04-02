package lila.search

import alleycats.Zero

opaque type Index = String
object Index extends OpaqueString[Index]

opaque type Id = String
object Id extends OpaqueString[Id]

opaque type From = Int
object From extends OpaqueInt[From]

opaque type Size = Int
object Size extends OpaqueInt[Size]

case class SearchResponse(ids: List[String]) extends AnyVal

object SearchResponse:
  def apply(txt: String): SearchResponse = SearchResponse(txt.split(',').toList)
  given Zero[SearchResponse]             = Zero(SearchResponse(Nil))

opaque type CountResponse = Int
object CountResponse extends OpaqueInt[CountResponse]:
  def apply(txt: String): CountResponse = CountResponse(~txt.toIntOption)
  given Zero[CountResponse]             = Zero(0)

object Date:
  val format    = "yyyy-MM-dd HH:mm:ss"
  val formatter = java.time.format.DateTimeFormatter ofPattern format
