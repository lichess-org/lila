package lila
package search

import Game.fields

import org.elasticsearch.index.query._, FilterBuilders._, QueryBuilders._

case class Query(
    usernames: List[String] = Nil,
    variant: Option[Int] = None,
    turns: Range[Int] = Range.none,
    elos: Range[Int] = Range.none,
    ai: Option[Boolean] = None,
    rated: Option[Boolean] = None,
    opening: Option[String] = None,
    date: Range[String] = Range.none,
    duration: Range[Int] = Range.none,
    sorting: Sorting = Sorting.default) {

  def request = Request(
    query = matchAllQuery,
    filter = filters.toNel map { fs ⇒
      andFilter(fs.list: _*)
    },
    sortings = sorting.fieldSort.toList
  )

  def filters = List(
    usernames map { u => termFilter(fields.uids, u.toLowerCase) },
    turns filters fields.turns,
    elos filters fields.elos,
    duration filters fields.duration,
    toFilter(variant, fields.variant),
    toFilter(ai, fields.ai),
    toFilter(rated, fields.rated),
    toFilter(opening, fields.opening),
    date filters fields.date
  ).flatten

  def toFilter(query: Option[_], name: String) =
    query.toList map {
      case s: String ⇒ termFilter(name, s.toLowerCase)
      case x         ⇒ termFilter(name, x)
    }
}

object Query {

  def test = Query(
    usernames = List("thibault"),
    duration = Range(none, 60.some),
    sorting = Sorting("turns", "desc")
  )
  def test2 = Query(
    opening = "A04".some,
    sorting = Sorting("turns", "desc")
  )
  def test3 = Query(
    usernames = List("controlaltdelete"),
    variant = 1.some,
    turns = Range(20.some, 100.some),
    elos = Range(1100.some, 2000.some),
    opening = "A00".some,
    ai = false.some,
    date = Range("2011-01-01".some, none),
    sorting = Sorting("date", "desc")
  )
}
