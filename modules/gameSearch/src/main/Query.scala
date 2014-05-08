package lila.gameSearch

import chess.{ Variant, Mode, Status, Openings }
import com.github.nscala_time.time.Imports._
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.QueryDefinition
import org.joda.time.DateTime

import lila.rating.RatingRange
import lila.search.{ ElasticSearch, Range }

case class Query(
    indexType: String,
    user1: Option[String] = None,
    user2: Option[String] = None,
    winner: Option[String] = None,
    variant: Option[Int] = None,
    status: Option[Int] = None,
    turns: Range[Int] = Range.none,
    averageRating: Range[Int] = Range.none,
    hasAi: Option[Boolean] = None,
    aiLevel: Range[Int] = Range.none,
    rated: Option[Boolean] = None,
    opening: Option[String] = None,
    date: Range[DateTime] = Range.none,
    duration: Range[Int] = Range.none,
    sorting: Sorting = Sorting.default,
    analysed: Option[Boolean] = None) extends lila.search.Query {

  import Fields._

  def nonEmpty =
    user1.nonEmpty ||
      user2.nonEmpty ||
      winner.nonEmpty ||
      variant.nonEmpty ||
      status.nonEmpty ||
      turns.nonEmpty ||
      averageRating.nonEmpty ||
      hasAi.nonEmpty ||
      aiLevel.nonEmpty ||
      rated.nonEmpty ||
      opening.nonEmpty ||
      date.nonEmpty ||
      duration.nonEmpty

  def searchDef(from: Int = 0, size: Int = 10) =
    search in indexType query makeQuery sort sorting.definition start from size size

  def countDef = count from indexType query makeQuery

  private lazy val makeQuery = filteredQuery query matchall filter {
    List(
      usernames map { termFilter(Fields.uids, _) },
      toFilters(winner, Fields.winner),
      turns filters Fields.turns,
      averageRating filters Fields.averageRating,
      duration map (60 *) filters Fields.duration,
      date map ElasticSearch.Date.formatter.print filters Fields.date,
      hasAiFilters,
      (hasAi | true).fold(aiLevel filters Fields.ai, Nil),
      toFilters(variant, Fields.variant),
      toFilters(rated, Fields.rated),
      toFilters(opening, Fields.opening),
      toFilters(status, Fields.status),
      toFilters(analysed, Fields.analysed)
    ).flatten match {
        case Nil => matchAllFilter
        case filters => must(filters: _*)
      }
  }

  def usernames = List(user1, user2).flatten

  private def hasAiFilters = hasAi.toList map { a =>
    a.fold(existsFilter(Fields.ai), missingFilter(Fields.ai))
  }

  private def toFilters(query: Option[_], name: String) = query.toList map {
    case s: String => termFilter(name, s.toLowerCase)
    case x         => termFilter(name, x)
  }
}

object Query {

  import lila.common.Form._

  val durations = options(List(1, 2, 3, 5, 10, 15, 20, 30), "%d minute{s}")

  val variants = Variant.all map { v => v.id -> v.name }

  val modes = Mode.all map { mode => mode.id -> mode.name }

  val openings = Openings.generals map {
    case (code, name) => code -> s"$code ${name.take(50)}"
  }

  val turns = options(
    (1 to 5) ++ (10 to 45 by 5) ++ (50 to 90 by 10) ++ (100 to 300 by 25),
    "%d move{s}")

  val averageRatings = (RatingRange.min to RatingRange.max by 100).toList map { e => e -> (e + " Rating") }

  val hasAis = List(0 -> "Human opponent", 1 -> "Computer opponent")

  val aiLevels = (1 to 8) map { l => l -> ("Stockfish level " + l) }

  val analyseds = 1 -> "Analysis available"

  val dates = List("0d" -> "Now") ++
    options(List(1, 2, 6), "h", "%d hour{s} ago") ++
    options(1 to 6, "d", "%d day{s} ago") ++
    options(1 to 3, "w", "%d week{s} ago") ++
    options(1 to 6, "m", "%d month{s} ago") ++
    options(1 to 4, "y", "%d year{s} ago")

  val statuses =
    Status.finishedNotCheated filterNot (_.is(_.Timeout)) map { s =>
      s.id -> s.is(_.Outoftime).fold("Clock Flag", s.name)
    }
}
