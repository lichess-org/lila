package lila.gameSearch

import chess.Mode
import play.api.data.*
import play.api.data.Forms.*
import play.api.i18n.Lang

import lila.common.Form.*
import lila.search.Range
import lila.user.UserForm.historicalUsernameField
import java.time.LocalDate

final private[gameSearch] class GameSearchForm:

  def search(using lang: Lang) = Form(
    mapping(
      "players" -> mapping(
        "a"      -> optional(historicalUsernameField),
        "b"      -> optional(historicalUsernameField),
        "winner" -> optional(historicalUsernameField),
        "loser"  -> optional(historicalUsernameField),
        "white"  -> optional(historicalUsernameField),
        "black"  -> optional(historicalUsernameField)
      )(SearchPlayer.apply)(unapply),
      "winnerColor" -> optional(numberIn(Query.winnerColors)),
      "perf"        -> optional(numberIn(lila.rating.PerfType.nonPuzzle.map(_.id.value))),
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
      )(SearchClock.apply)(unapply),
      "dateMin"  -> GameSearchForm.dateField,
      "dateMax"  -> GameSearchForm.dateField,
      "status"   -> optional(numberIn(Query.statuses)),
      "analysed" -> optional(number),
      "sort" -> optional(
        mapping(
          "field" -> stringIn(Sorting.fields),
          "order" -> stringIn(Sorting.orders)
        )(SearchSort.apply)(unapply)
      )
    )(SearchData.apply)(unapply)
  ) fill SearchData()

private[gameSearch] object GameSearchForm:
  val dateField = optional(ISODateOrTimestamp.mapping)

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
    dateMin: Option[LocalDate] = None,
    dateMax: Option[LocalDate] = None,
    status: Option[Int] = None,
    analysed: Option[Int] = None,
    sort: Option[SearchSort] = None
):

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

private[gameSearch] case class SearchPlayer(
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
  def cleanLoser  = oneOf(loser)
  def cleanWhite  = oneOf(white)
  def cleanBlack  = oneOf(black)

  private def oneOf(s: Option[UserStr]) = s.map(_.id).filter(List(cleanA, cleanB).flatten.contains)

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
