package lila
package search

import Game.fields
import chess.{ Variant, Mode, Status, EcoDb }

import org.elasticsearch.index.query._, FilterBuilders._, QueryBuilders._

case class Query(
    usernames: List[String] = Nil,
    variant: Option[Int] = None,
    status: Option[Int] = None,
    turns: Range[Int] = Range.none,
    averageElo: Range[Int] = Range.none,
    hasAi: Option[Boolean] = None,
    aiLevel: Range[Int] = Range.none,
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
    usernames map { u ⇒ termFilter(fields.uids, u.toLowerCase) },
    turns filters fields.turns,
    averageElo filters fields.averageElo,
    duration filters fields.duration,
    date filters fields.date,
    averageElo filters fields.date,
    hasAi.toList map { a ⇒
      a.fold(
        rangeFilter(fields.ai) gt 0,
        termFilter(fields.ai, null)
      )
    },
    aiLevel filters fields.ai,
    toFilter(variant, fields.variant),
    toFilter(rated, fields.rated),
    toFilter(opening, fields.opening),
    toFilter(status, fields.status)
  ).flatten

  def toFilter(query: Option[_], name: String) =
    query.toList map {
      case s: String ⇒ termFilter(name, s.toLowerCase)
      case x         ⇒ termFilter(name, x)
    }
}

object Query {

  val durations = List(0, 1, 2, 3, 5, 10, 15, 20, 30) map { d ⇒
    d -> (d + " minutes")
  }

  val variants = Variant.all map { v ⇒ v.id -> v.name }

  val modes = Mode.all map { mode ⇒ mode.id -> mode.name }

  val openings = EcoDb.db map {
    case (code, name, _) ⇒ code -> (code + " " + name)
  }

  val turns = {
    (1 to 5) ++ (10 to 45 by 5) ++ (50 to 90 by 10) ++ (100 to 300 by 25)
  }.toList map { t ⇒ t -> (t + " turns") }

  val averageElos = (800 to 2300 by 100).toList map { e ⇒ e -> (e + " ELO") }

  val hasAis = List(
    0 -> "Human opponent",
    1 -> "Computer opponent")

  val aiLevels = (1 to 8) map { l ⇒ l -> ("Stockfish level " + l) }

  val dates = List("0d" -> "Today") ++ {
    (1 to 6) map { d ⇒ (d + "d") -> (d + " days ago") }
  } ++ {
    (1 to 3) map { w ⇒ (w + "w") -> (w + " weeks ago") }
  } ++ {
    (1 to 6) map { m ⇒ (m + "m") -> (m + " months ago") }
  } ++ {
    (1 to 3) map { y ⇒ (y + "y") -> (y + " years ago") }
  }

  val statuses = Status.finishedNotCheated map { s ⇒ s.id -> s.name }

  def test = Query(
    usernames = List("thibault"),
    duration = Range(60.some, 150.some),
    sorting = Sorting(fields.averageElo, "desc")
  )
  def test2 = Query(
    opening = "A04".some,
    sorting = Sorting(fields.turns, "desc")
  )
  def test3 = Query(
    usernames = List("controlaltdelete"),
    variant = 1.some,
    turns = Range(20.some, 100.some),
    averageElo = Range(1100.some, 2000.some),
    opening = "A00".some,
    hasAi = true.some,
    aiLevel = Range.none,
    date = Range("2011-01-01".some, none),
    sorting = Sorting(fields.date, "desc")
  )
}
