package lila.gameSearch

import chess.{ Mode }
import org.joda.time.DateTime
import play.api.data._
import play.api.data.Forms._
import play.api.data.validation.Constraints

import lila.common.Form._
import lila.search.Range

private[gameSearch] final class DataForm {

  val search = Form(mapping(
    "players" -> mapping(
      "a" -> optional(nonEmptyText),
      "b" -> optional(nonEmptyText),
      "winner" -> optional(nonEmptyText),
      "white" -> optional(nonEmptyText),
      "black" -> optional(nonEmptyText)
    )(SearchPlayer.apply)(SearchPlayer.unapply),
    "winnerColor" -> optional(numberIn(Query.winnerColors)),
    "perf" -> optional(numberIn(Query.perfs)),
    "source" -> optional(numberIn(Query.sources)),
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
    "dateMin" -> DataForm.dateField,
    "dateMax" -> DataForm.dateField,
    "status" -> optional(numberIn(Query.statuses)),
    "analysed" -> optional(number),
    "sort" -> optional(mapping(
      "field" -> stringIn(Sorting.fields),
      "order" -> stringIn(Sorting.orders)
    )(SearchSort.apply)(SearchSort.unapply))
  )(SearchData.apply)(SearchData.unapply)) fill SearchData()
}

private[gameSearch] object DataForm {

  val DateDelta = """^(\d+)(\w)$""".r
  private val dateConstraint = Constraints.pattern(
    regex = DateDelta,
    error = "Invalid date.")
  val dateField = optional(nonEmptyText.verifying(dateConstraint))
}

private[gameSearch] case class SearchData(
    players: SearchPlayer = SearchPlayer(),
    winnerColor: Option[Int] = None,
    perf: Option[Int] = None,
    source: Option[Int] = None,
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
    sort: Option[SearchSort] = None) {

  def sortOrDefault = sort | SearchSort()

  def query = Query(
    user1 = players.cleanA,
    user2 = players.cleanB,
    winner = players.cleanWinner,
    winnerColor = winnerColor,
    perf = perf,
    source = source,
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
    whiteUser = players.cleanWhite,
    blackUser = players.cleanBlack,
    sorting = Sorting(sortOrDefault.field, sortOrDefault.order))

  def nonEmptyQuery = Some(query).filter(_.nonEmpty)

  import DataForm.DateDelta

  private def toDate(delta: String): Option[DateTime] = delta match {
    case DateDelta(n, "h") => parseIntOption(n) map DateTime.now.minusHours
    case DateDelta(n, "d") => parseIntOption(n) map DateTime.now.minusDays
    case DateDelta(n, "w") => parseIntOption(n) map DateTime.now.minusWeeks
    case DateDelta(n, "m") => parseIntOption(n) map DateTime.now.minusMonths
    case DateDelta(n, "y") => parseIntOption(n) map DateTime.now.minusYears
    case _                 => None
  }
  private val dateConstraint = Constraints.pattern(
    regex = DateDelta,
    error = "Invalid date.")
}

private[gameSearch] case class SearchPlayer(
    a: Option[String] = None,
    b: Option[String] = None,
    winner: Option[String] = None,
    white: Option[String] = None,
    black: Option[String] = None) {

  lazy val cleanA = clean(a)
  lazy val cleanB = clean(b)
  def cleanWinner = oneOf(winner)
  def cleanWhite = oneOf(white)
  def cleanBlack = oneOf(black)

  private def oneOf(s: Option[String]) = clean(s).filter(List(cleanA, cleanB).flatten.contains)
  private def clean(s: Option[String]) = s map (_.trim.toLowerCase) filter (_.nonEmpty)
}

private[gameSearch] case class SearchSort(
  field: String = Sorting.default.f,
  order: String = Sorting.default.order)
