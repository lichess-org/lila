package lila
package search

import play.api.data._
import play.api.data.Forms._
import scalaz.effects._
import org.joda.time.DateTime
import org.scala_tools.time.Imports._

import chess.{ Mode }

final class DataForm {

  import lila.common.Form._

  val search = Form(mapping(
    "players" -> mapping(
      "a" -> optional(nonEmptyText),
      "b" -> optional(nonEmptyText),
      "winner" -> optional(nonEmptyText)
    )(SearchPlayer.apply)(SearchPlayer.unapply),
    "variant" -> optional(numberIn(Query.variants)),
    "mode" -> optional(numberIn(Query.modes)),
    "opening" -> optional(stringIn(Query.openings)),
    "turnsMin" -> optional(numberIn(Query.turns)),
    "turnsMax" -> optional(numberIn(Query.turns)),
    "eloMin" -> optional(numberIn(Query.averageElos)),
    "eloMax" -> optional(numberIn(Query.averageElos)),
    "hasAi" -> optional(numberIn(Query.hasAis)),
    "aiLevelMin" -> optional(numberIn(Query.aiLevels)),
    "aiLevelMax" -> optional(numberIn(Query.aiLevels)),
    "durationMin" -> optional(numberIn(Query.durations)),
    "durationMax" -> optional(numberIn(Query.durations)),
    "dateMin" -> optional(stringIn(Query.dates)),
    "dateMax" -> optional(stringIn(Query.dates)),
    "status" -> optional(numberIn(Query.statuses)),
    "sort" -> mapping(
      "field" -> stringIn(Sorting.fields),
      "order" -> stringIn(Sorting.orders)
    )(SearchSort.apply)(SearchSort.unapply)
  )(SearchData.apply)(SearchData.unapply)) fill SearchData()
}

case class SearchData(
    players: SearchPlayer = SearchPlayer(),
    variant: Option[Int] = None,
    mode: Option[Int] = None,
    opening: Option[String] = None,
    turnsMin: Option[Int] = None,
    turnsMax: Option[Int] = None,
    eloMin: Option[Int] = None,
    eloMax: Option[Int] = None,
    hasAi: Option[Int] = None,
    aiLevelMin: Option[Int] = None,
    aiLevelMax: Option[Int] = None,
    durationMin: Option[Int] = None,
    durationMax: Option[Int] = None,
    dateMin: Option[String] = None,
    dateMax: Option[String] = None,
    status: Option[Int] = None,
    sort: SearchSort = SearchSort()) {

  lazy val query = Query(
    user1 = players.cleanA,
    user2 = players.cleanB,
    winner = players.cleanWinner,
    variant = variant,
    rated = mode flatMap Mode.apply map (_.rated),
    opening = opening map clean,
    turns = Range(turnsMin, turnsMax),
    averageElo = Range(eloMin, eloMax),
    hasAi = hasAi map (_ == 1),
    aiLevel = Range(aiLevelMin, aiLevelMax),
    duration = Range(durationMin, durationMax),
    date = Range(dateMin flatMap toDate, dateMax flatMap toDate),
    status = status,
    sorting = Sorting(sort.field, sort.order)
  )

  private def clean(s: String) = s.trim.toLowerCase

  private val DateDelta = """^(\d+)(\w)$""".r
  private def toDate(delta: String): Option[DateTime] = delta match {
    case DateDelta(n, "h") ⇒ parseIntOption(n) map (DateTime.now - _.hours)
    case DateDelta(n, "d") ⇒ parseIntOption(n) map (DateTime.now - _.days)
    case DateDelta(n, "w") ⇒ parseIntOption(n) map (DateTime.now - _.weeks)
    case DateDelta(n, "m") ⇒ parseIntOption(n) map (DateTime.now - _.months)
    case DateDelta(n, "y") ⇒ parseIntOption(n) map (DateTime.now - _.years)
    case _                 ⇒ None
  }
}

case class SearchPlayer(
    a: Option[String] = None,
    b: Option[String] = None,
    winner: Option[String] = None) {

  def cleanA = clean(a)
  def cleanB = clean(b)
  def cleanWinner = clean(winner) |> { w ⇒
    w filter List(a, b).flatten.contains
  }

  private def clean(s: Option[String]) =
    s map (_.trim.toLowerCase) filter (_.nonEmpty)
}

case class SearchSort(
  field: String = Sorting.default.field,
  order: String = Sorting.default.order)
