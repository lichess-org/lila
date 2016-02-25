package lila.gameSearch

import chess.{ Mode, Status }
import chess.opening._
import org.joda.time.DateTime

import lila.rating.RatingRange
import lila.search.Range

case class Query(
    user1: Option[String] = None,
    user2: Option[String] = None,
    winner: Option[String] = None,
    winnerColor: Option[Int] = None,
    perf: Option[Int] = None,
    source: Option[Int] = None,
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
    analysed: Option[Boolean] = None,
    whiteUser: Option[String] = None,
    blackUser: Option[String] = None) {

  def nonEmpty =
    user1.nonEmpty ||
      user2.nonEmpty ||
      winner.nonEmpty ||
      winnerColor.nonEmpty ||
      perf.nonEmpty ||
      source.nonEmpty ||
      status.nonEmpty ||
      turns.nonEmpty ||
      averageRating.nonEmpty ||
      hasAi.nonEmpty ||
      aiLevel.nonEmpty ||
      rated.nonEmpty ||
      opening.nonEmpty ||
      date.nonEmpty ||
      duration.nonEmpty ||
      analysed.nonEmpty
}

object Query {

  import lila.common.Form._
  import play.api.libs.json._

  import Range.rangeJsonWriter
  private implicit val sortingJsonWriter = play.api.libs.json.Json.writes[Sorting]
  implicit val jsonWriter = play.api.libs.json.Json.writes[Query]

  val durations =
    options(List(1, 2, 3, 5, 10, 15, 20, 30), "%d minute{s}").toList :+
      (60 * 1, "One hour") :+
      (60 * 3, "Three hours") :+
      (60 * 24, "One day") :+
      (60 * 24 * 3, "Three days") :+
      (60 * 24 * 7, "One week") :+
      (60 * 24 * 7 * 2, "Two weeks") :+
      (60 * 24 * 30, "One month") :+
      (60 * 24 * 30 * 3, "Three months") :+
      (60 * 24 * 30 * 6, "6 months") :+
      (60 * 24 * 365, "One year")

  val winnerColors = List(1 -> "White", 2 -> "Black", 3 -> "None")

  val perfs = lila.rating.PerfType.nonPuzzle map { v => v.id -> v.name }

  val sources = lila.game.Source.searchable map { v => v.id -> v.name.capitalize }

  val modes = Mode.all map { mode => mode.id -> mode.name.capitalize }

  val openings = EcopeningDB.all map { o =>
    o.eco -> s"${o.eco} ${o.name.take(50)}"
  }

  val turns = options(
    (1 to 5) ++ (10 to 45 by 5) ++ (50 to 90 by 10) ++ (100 to 300 by 25),
    "%d move{s}")

  val averageRatings = (RatingRange.min to RatingRange.max by 100).toList map { e => e -> (e + " Rating") }

  val hasAis = List(0 -> "Human opponent", 1 -> "Computer opponent")

  val aiLevels = (1 to 8) map { l => l -> ("level " + l) }

  val dates = List("0d" -> "Now") ++
    options(List(1, 2, 6), "h", "%d hour{s} ago") ++
    options(1 to 6, "d", "%d day{s} ago") ++
    options(1 to 3, "w", "%d week{s} ago") ++
    options(1 to 6, "m", "%d month{s} ago") ++
    options(1 to 5, "y", "%d year{s} ago")

  val statuses = Status.finishedNotCheated.map {
    case s if s.is(_.Timeout)    => none
    case s if s.is(_.NoStart)    => none
    case s if s.is(_.Outoftime)  => Some(s.id -> "Clock Flag")
    case s if s.is(_.VariantEnd) => Some(s.id -> "Variant End")
    case s                       => Some(s.id -> s.toString)
  }.flatten
}
