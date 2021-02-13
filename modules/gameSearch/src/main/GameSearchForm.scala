package lila.gameSearch

import chess.Mode
import org.joda.time.DateTime
import play.api.data._
import play.api.data.Forms._

import lila.common.Form._
import lila.search.Range

final private[gameSearch] class GameSearchForm {

  val search = Form(
    mapping(
      "players" -> mapping(
        "a"      -> optional(nonEmptyText),
        "b"      -> optional(nonEmptyText),
        "winner" -> optional(nonEmptyText),
        "loser"  -> optional(nonEmptyText),
        "white"  -> optional(nonEmptyText),
        "black"  -> optional(nonEmptyText)
      )(SearchPlayer.apply)(SearchPlayer.unapply),
      "winnerColor" -> optional(numberIn(Query.winnerColors)),
      "perf"        -> optional(numberIn(lila.rating.PerfType.nonPuzzle.map(_.id))),
      "source"      -> optional(numberIn(Query.sources)),
      "mode"        -> optional(numberIn(Query.modes)),
      "turnsMin"    -> optional(numberIn(Query.turns)),
      "turnsMax"    -> optional(numberIn(Query.turns)),
      "ratingMin"   -> optional(numberIn(Query.averageRatings)),
      "ratingMax"   -> optional(numberIn(Query.averageRatings)),
      "hasAi"       -> optional(numberIn(Query.hasAis)),
      "aiLevelMin"  -> optional(numberIn(Query.aiLevels)),
      "aiLevelMax"  -> optional(numberIn(Query.aiLevels)),
      "durationMin" -> optional(numberIn(Query.durations)),
      "durationMax" -> optional(numberIn(Query.durations)),
      "clock" -> mapping(
        "initMin" -> optional(numberIn(Query.clockInits)),
        "initMax" -> optional(numberIn(Query.clockInits)),
        "incMin"  -> optional(numberIn(Query.clockIncs)),
        "incMax"  -> optional(numberIn(Query.clockIncs))
      )(SearchClock.apply)(SearchClock.unapply),
      "dateMin"  -> GameSearchForm.dateField,
      "dateMax"  -> GameSearchForm.dateField,
      "status"   -> optional(numberIn(Query.statuses)),
      "analysed" -> optional(number),
      "sort" -> optional(
        mapping(
          "field" -> stringIn(Sorting.fields),
          "order" -> stringIn(Sorting.orders)
        )(SearchSort.apply)(SearchSort.unapply)
      )
    )(SearchData.apply)(SearchData.unapply)
  ) fill SearchData()
}

private[gameSearch] object GameSearchForm {
  val dateField = optional(ISODateOrTimestamp.isoDateOrTimestamp)
}

private[gameSearch] case class SearchData(
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
    clock: SearchClock = SearchClock(),
    dateMin: Option[DateTime] = None,
    dateMax: Option[DateTime] = None,
    status: Option[Int] = None,
    analysed: Option[Int] = None,
    sort: Option[SearchSort] = None
) {

  def sortOrDefault = sort | SearchSort()

  def query =
    Query(
      user1 = players.cleanA,
      user2 = players.cleanB,
      winner = players.cleanWinner,
      loser = players.cleanLoser,
      winnerColor = winnerColor,
      perf = perf,
      source = source,
      rated = mode flatMap Mode.apply map (_.rated),
      turns = Range(turnsMin, turnsMax),
      averageRating = Range(ratingMin, ratingMax),
      hasAi = hasAi map (_ == 1),
      aiLevel = Range(aiLevelMin, aiLevelMax),
      duration = Range(durationMin, durationMax),
      clock = Clocking(clock.initMin, clock.initMax, clock.incMin, clock.incMax),
      date = Range(dateMin, dateMax),
      status = status,
      analysed = analysed map (_ == 1),
      whiteUser = players.cleanWhite,
      blackUser = players.cleanBlack,
      sorting = Sorting(sortOrDefault.field, sortOrDefault.order)
    )

  def nonEmptyQuery = Some(query).filter(_.nonEmpty)
}

private[gameSearch] case class SearchPlayer(
    a: Option[String] = None,
    b: Option[String] = None,
    winner: Option[String] = None,
    loser: Option[String] = None,
    white: Option[String] = None,
    black: Option[String] = None
) {

  lazy val cleanA = clean(a)
  lazy val cleanB = clean(b)
  def cleanWinner = oneOf(winner)
  def cleanLoser  = oneOf(loser)
  def cleanWhite  = oneOf(white)
  def cleanBlack  = oneOf(black)

  private def oneOf(s: Option[String]) = clean(s).filter(List(cleanA, cleanB).flatten.contains)
  private def clean(s: Option[String]) = s map (_.trim.toLowerCase) filter (_.nonEmpty)
}

private[gameSearch] case class SearchSort(
    field: String = Sorting.default.f,
    order: String = Sorting.default.order
)

private[gameSearch] case class SearchClock(
    initMin: Option[Int] = None,
    initMax: Option[Int] = None,
    incMin: Option[Int] = None,
    incMax: Option[Int] = None
)
