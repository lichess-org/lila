package lila.gameSearch

import chess.{ Mode }
import com.github.nscala_time.time.Imports._
import org.joda.time.DateTime
import play.api.data._
import play.api.data.Forms._

import lila.common.Form._
import lila.search.Range

private[gameSearch] final class DataForm {

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
    "ratingMin" -> optional(numberIn(Query.averageRatings)),
    "ratingMax" -> optional(numberIn(Query.averageRatings)),
    "hasAi" -> optional(numberIn(Query.hasAis)),
    "aiLevelMin" -> optional(numberIn(Query.aiLevels)),
    "aiLevelMax" -> optional(numberIn(Query.aiLevels)),
    "durationMin" -> optional(numberIn(Query.durations)),
    "durationMax" -> optional(numberIn(Query.durations)),
    "dateMin" -> optional(stringIn(Query.dates)),
    "dateMax" -> optional(stringIn(Query.dates)),
    "status" -> optional(numberIn(Query.statuses)),
    "analysed" -> optional(numberIn(Query.analyseds :: Nil)),
    "sort" -> mapping(
      "field" -> stringIn(Sorting.fields),
      "order" -> stringIn(Sorting.orders)
    )(SearchSort.apply)(SearchSort.unapply)
  )(SearchData.apply)(SearchData.unapply)) fill SearchData()
}

private[gameSearch] case class SearchData(
    players: SearchPlayer = SearchPlayer(),
    variant: Option[Int] = None,
    mode: Option[Int] = None,
    opening: Option[String] = None,
    turnsMin: Option[Int] = None,
    turnsMax: Option[Int] = None,
    ratingMin: Option[Int] = None,
    ratingMax: Option[Int] = None,
    hasAi: Option[Int] = None,
    aiLevelMin: Option[Int] = None,
    aiLevelMax: Option[Int] = None,
    durationMin: Option[Int] = None,
    durationMax: Option[Int] = None,
    dateMin: Option[String] = None,
    dateMax: Option[String] = None,
    status: Option[Int] = None,
    analysed: Option[Int] = None,
    sort: SearchSort = SearchSort()) {

  def query(indexType: String) = Query(
    indexType = indexType,
    user1 = players.cleanA,
    user2 = players.cleanB,
    winner = players.cleanWinner,
    variant = variant,
    rated = mode flatMap Mode.apply map (_.rated),
    opening = opening map (_.trim.toLowerCase),
    turns = Range(turnsMin, turnsMax),
    averageRating = Range(ratingMin, ratingMax),
    hasAi = hasAi map (_ == 1),
    aiLevel = Range(aiLevelMin, aiLevelMax),
    duration = Range(durationMin, durationMax),
    date = Range(dateMin flatMap toDate, dateMax flatMap toDate),
    status = status,
    analysed = analysed map (_ == 1),
    sorting = Sorting(sort.field, sort.order))

  def nonEmptyQuery(indexType: String) = {
    val q = query(indexType)
    q.nonEmpty option q
  }

  private val DateDelta = """^(\d+)(\w)$""".r
  private def toDate(delta: String): Option[DateTime] = delta match {
    case DateDelta(n, "h") => parseIntOption(n) map (DateTime.now - _.hours)
    case DateDelta(n, "d") => parseIntOption(n) map (DateTime.now - _.days)
    case DateDelta(n, "w") => parseIntOption(n) map (DateTime.now - _.weeks)
    case DateDelta(n, "m") => parseIntOption(n) map (DateTime.now - _.months)
    case DateDelta(n, "y") => parseIntOption(n) map (DateTime.now - _.years)
    case _                 => None
  }
}

private[gameSearch] case class SearchPlayer(
    a: Option[String] = None,
    b: Option[String] = None,
    winner: Option[String] = None) {

  def cleanA = clean(a)
  def cleanB = clean(b)
  def cleanWinner = clean(winner) |> { w =>
    w filter List(cleanA, cleanB).flatten.contains
  }

  private def clean(s: Option[String]) =
    s map (_.trim.toLowerCase) filter (_.nonEmpty)
}

private[gameSearch] case class SearchSort(
  field: String = Sorting.default.field,
  order: String = Sorting.default.order)
