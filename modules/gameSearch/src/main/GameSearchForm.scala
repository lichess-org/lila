package lila.gameSearch

import chess.Rated
import play.api.data.*
import play.api.data.Forms.*
import smithy4s.Timestamp

import java.time.LocalDate

import lila.common.Form.*
import lila.core.i18n.Translate
import lila.search.spec.{ DateRange, IntRange, Query, Sorting as SpecSorting }

final private[gameSearch] class GameSearchForm:

  def search(using Translate) = Form(
    mapping(
      "players" -> mapping(
        "a" -> optional(username.historicalField),
        "b" -> optional(username.historicalField),
        "winner" -> optional(username.historicalField),
        "loser" -> optional(username.historicalField),
        "white" -> optional(username.historicalField),
        "black" -> optional(username.historicalField)
      )(SearchPlayer.apply)(unapply),
      "winnerColor" -> optional(numberIn(FormHelpers.winnerColors)),
      "perf" -> optional(numberIn(perfKeys.map(_.id.value))),
      "source" -> optional(numberIn(FormHelpers.sources)),
      "mode" -> optional(numberIn(FormHelpers.modes)),
      "turnsMin" -> optional(numberIn(FormHelpers.turns)),
      "turnsMax" -> optional(numberIn(FormHelpers.turns)),
      "ratingMin" -> optional(numberIn(FormHelpers.averageRatings)),
      "ratingMax" -> optional(numberIn(FormHelpers.averageRatings)),
      "hasAi" -> optional(numberIn(FormHelpers.hasAis)),
      "aiLevelMin" -> optional(numberIn(FormHelpers.aiLevels)),
      "aiLevelMax" -> optional(numberIn(FormHelpers.aiLevels)),
      "durationMin" -> optional(numberIn(FormHelpers.durations)),
      "durationMax" -> optional(numberIn(FormHelpers.durations)),
      "clockInit" -> optional(numberIn(FormHelpers.clockInits)),
      "clockInc" -> optional(numberIn(FormHelpers.clockIncs)),
      "dateMin" -> GameSearchForm.dateField,
      "dateMax" -> GameSearchForm.dateField,
      "status" -> optional(numberIn(FormHelpers.statuses)),
      "analysed" -> optional(number),
      "sort" -> optional(
        mapping(
          "field" -> stringIn(Sorting.fields),
          "order" -> stringIn(Sorting.orders)
        )(SearchSort.apply)(unapply)
      )
    )(SearchData.apply)(unapply)
  ).fill(SearchData())

private[gameSearch] object GameSearchForm:
  val dateField = optional(ISODateOrTimestamp.mapping)

case class SearchData(
    players: SearchPlayer = SearchPlayer(),
    winnerColor: Option[Int] = None,
    perf: Option[Int] = None,
    source: Option[Int] = None,
    mode: Option[Int] = None,
    turnsMin: Option[Int] = None,
    turnsMax: Option[Int] = None,
    ratingMin: Option[Int] = None,
    ratingMax: Option[Int] = None,
    hasAi: Option[Int] = None,
    aiLevelMin: Option[Int] = None,
    aiLevelMax: Option[Int] = None,
    durationMin: Option[Int] = None,
    durationMax: Option[Int] = None,
    clockInit: Option[Int] = None,
    clockInc: Option[Int] = None,
    dateMin: Option[LocalDate] = None,
    dateMax: Option[LocalDate] = None,
    status: Option[Int] = None,
    analysed: Option[Int] = None,
    sort: Option[SearchSort] = None
):

  def sortOrDefault = sort | SearchSort()

  def query: Query.Game = Query.game(
    user1 = players.cleanA.map(_.value),
    user2 = players.cleanB.map(_.value),
    winner = players.cleanWinner.map(_.value),
    loser = players.cleanLoser.map(_.value),
    winnerColor = winnerColor,
    perf =
      if perf.exists(_ == 5) then List(1, 2, 3, 4, 6)
      else perf.toList, // 1,2,3,4,6 are the perf types for standard games
    source = source,
    rated = mode.flatMap(Rated.apply).map(_.yes),
    status = status,
    turns = IntRange(turnsMin, turnsMax),
    averageRating = IntRange(ratingMin, ratingMax),
    hasAi = hasAi.map(_ == 1),
    aiLevel = IntRange(aiLevelMin, aiLevelMax),
    date = DateRange(dateMin.map(transform), dateMax.map(transform)),
    duration = IntRange(durationMin, durationMax),
    analysed = analysed.map(_ == 1),
    whiteUser = players.cleanWhite.map(_.value),
    blackUser = players.cleanBlack.map(_.value),
    sorting = SpecSorting(sortOrDefault.field, sortOrDefault.order),
    clockInit = clockInit,
    clockInc = clockInc
  )

  def transform(l: LocalDate): Timestamp = Timestamp(l.getYear, l.getMonthValue, l.getDayOfMonth)

  def nonEmptyQuery: Option[Query.Game] = query.some.filter(_.nonEmpty)

case class SearchPlayer(
    a: Option[UserStr] = None,
    b: Option[UserStr] = None,
    winner: Option[UserStr] = None,
    loser: Option[UserStr] = None,
    white: Option[UserStr] = None,
    black: Option[UserStr] = None
):

  lazy val cleanA = a.map(_.id)
  lazy val cleanB = b.map(_.id)
  def cleanWinner = oneOf(winner)
  def cleanLoser = oneOf(loser)
  def cleanWhite = oneOf(white)
  def cleanBlack = oneOf(black)

  private def oneOf(s: Option[UserStr]) = s.map(_.id).filter(List(cleanA, cleanB).flatten.contains)

case class SearchSort(
    field: String = Sorting.default.f,
    order: String = Sorting.default.order
)
