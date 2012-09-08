package lila
package search

import Game.fields
import chess.{ Variant, Mode, Status, EcoDb }

import org.elasticsearch.index.query._, FilterBuilders._, QueryBuilders._
import org.joda.time.DateTime
import org.scala_tools.time.Imports._

case class Query(
    user1: Option[String] = None,
    user2: Option[String] = None,
    winner: Option[String] = None,
    variant: Option[Int] = None,
    status: Option[Int] = None,
    turns: Range[Int] = Range.none,
    averageElo: Range[Int] = Range.none,
    hasAi: Option[Boolean] = None,
    aiLevel: Range[Int] = Range.none,
    rated: Option[Boolean] = None,
    opening: Option[String] = None,
    date: Range[DateTime] = Range.none,
    duration: Range[Int] = Range.none,
    sorting: Sorting = Sorting.default) {

  def nonEmpty = 
    user1.nonEmpty ||
    user2.nonEmpty ||
    winner.nonEmpty ||
    variant.nonEmpty ||
    status.nonEmpty ||
    turns.nonEmpty ||
    averageElo.nonEmpty ||
    hasAi.nonEmpty ||
    aiLevel.nonEmpty ||
    rated.nonEmpty ||
    opening.nonEmpty ||
    date.nonEmpty ||
    duration.nonEmpty 

  def searchRequest(from: Int = 0, size: Int = 10) = SearchRequest(
    query = matchAllQuery,
    filter = filters,
    sortings = List(sorting.fieldSort),
    from = from,
    size = size)

  def countRequest = CountRequest(matchAllQuery, filters)

  def usernames = List(user1, user2).flatten

  private def filters = List(
    usernames map { termFilter(fields.uids, _) },
    toFilters(winner, fields.winner),
    turns filters fields.turns,
    averageElo filters fields.averageElo,
    duration map (60 *) filters fields.duration,
    date map Game.dateFormatter.print filters fields.date,
    hasAiFilters,
    (hasAi | true).fold(aiLevel filters fields.ai, Nil),
    toFilters(variant, fields.variant),
    toFilters(rated, fields.rated),
    toFilters(opening, fields.opening),
    toFilters(status, fields.status)
  ).flatten.toNel map { fs ⇒
      andFilter(fs.list: _*)
    }

  private def hasAiFilters = hasAi.toList map { a ⇒
    a.fold(existsFilter(fields.ai), missingFilter(fields.ai))
  }

  private def toFilters(query: Option[_], name: String) = query.toList map {
    case s: String ⇒ termFilter(name, s.toLowerCase)
    case x         ⇒ termFilter(name, x)
  }
}

object Query {

  val durations = List(1, 2, 3, 5, 10, 15, 20, 30) map { d ⇒
    d -> (d + " minutes")
  }

  val variants = Variant.all map { v ⇒ v.id -> v.name }

  val modes = Mode.all map { mode ⇒ mode.id -> mode.name }

  val openings = EcoDb.db map {
    case (code, name, _) ⇒ code -> (code + " " + name.take(50))
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

  val statuses = 
    Status.finishedNotCheated filterNot (_.is(_.Timeout)) map { s ⇒ s.id -> s.name } 
}
