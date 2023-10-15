package lila.gameSearch

import chess.Status

import lila.common.Json.given
import lila.rating.RatingRange
import lila.search.Range
import java.time.LocalDate

case class Query(
    user1: Option[UserId] = None,
    user2: Option[UserId] = None,
    winner: Option[UserId] = None,
    loser: Option[UserId] = None,
    winnerColor: Option[Int] = None,
    perf: Option[Int] = None,
    source: Option[Int] = None,
    status: Option[Int] = None,
    turns: Range[Int] = Range.none,
    averageRating: Range[Int] = Range.none,
    hasAi: Option[Boolean] = None,
    aiLevel: Range[Int] = Range.none,
    rated: Option[Boolean] = None,
    date: Range[LocalDate] = Range.none,
    duration: Range[Int] = Range.none,
    clock: Clocking = Clocking(),
    sorting: Sorting = Sorting.default,
    analysed: Option[Boolean] = None,
    whiteUser: Option[UserId] = None,
    blackUser: Option[UserId] = None
):

  def userIds: Set[UserId] = Set(user1, user2, winner, loser, whiteUser, blackUser).flatten

  def nonEmpty =
    user1.nonEmpty ||
      user2.nonEmpty ||
      winner.nonEmpty ||
      loser.nonEmpty ||
      winnerColor.nonEmpty ||
      perf.nonEmpty ||
      source.nonEmpty ||
      status.nonEmpty ||
      turns.nonEmpty ||
      averageRating.nonEmpty ||
      hasAi.nonEmpty ||
      aiLevel.nonEmpty ||
      rated.nonEmpty ||
      date.nonEmpty ||
      duration.nonEmpty ||
      clock.nonEmpty ||
      analysed.nonEmpty

object Query:

  import lila.common.Form.*
  import play.api.libs.json.*
  import play.api.i18n.Lang
  import lila.i18n.{ I18nKeys as trans }

  import Range.given
  private given Writes[Sorting]  = Json.writes
  private given Writes[Clocking] = Json.writes
  given Writes[Query]            = Json.writes

  def durations(using lang: Lang): List[(Int, String)] =
    ((30, trans.nbSeconds.pluralSameTxt(30)) ::
      options(
        List(60, 60 * 2, 60 * 3, 60 * 5, 60 * 10, 60 * 15, 60 * 20, 60 * 30),
        i => trans.nbMinutes.pluralSameTxt(i / 60)
      ).toList) :+
      (60 * 60 * 1 -> trans.nbHours.pluralSameTxt(1)) :+
      (60 * 60 * 2 -> trans.nbHours.pluralSameTxt(2)) :+
      (60 * 60 * 3 -> trans.nbHours.pluralSameTxt(3))

  def clockInits(using lang: Lang) = List(
    (30, trans.nbSeconds.pluralSameTxt(30)),
    (45, trans.nbSeconds.pluralSameTxt(45))
  ) ::: options(
    List(
      60 * 1,
      60 * 2,
      60 * 3,
      60 * 5,
      60 * 10,
      60 * 15,
      60 * 20,
      60 * 30,
      60 * 45,
      60 * 60,
      60 * 90,
      60 * 120,
      60 * 150,
      60 * 180
    ),
    i => trans.nbMinutes.pluralSameTxt(i / 60)
  ).toList

  def clockIncs(using lang: Lang) =
    options(
      List(0, 1, 2, 3, 5, 10, 15, 20, 30, 45, 60, 90, 120, 150, 180),
      i => trans.nbSeconds.pluralSameTxt(i)
    ).toList

  def winnerColors(using lang: Lang) = List(1 -> trans.white.txt(), 2 -> trans.black.txt())

  val sources = lila.game.Source.searchable map { v =>
    v.id -> v.name.capitalize
  }

  def modes(using lang: Lang) = List(0 -> trans.casual.txt(), 1 -> trans.rated.txt())

  val turns = options(
    (1 to 5) ++ (10 to 45 by 5) ++ (50 to 90 by 10) ++ (100 to 300 by 25),
    _.toString
  )

  val averageRatings = (RatingRange.min.value to RatingRange.max.value by 100).toList map { e =>
    e -> e.toString
  }

  def hasAis(using lang: Lang) = List(0 -> trans.human.txt(), 1 -> trans.computer.txt())

  val aiLevels = (1 to 8) map { l =>
    l -> s"level $l"
  }

  val dates = List("0d" -> "Now") ++
    options(List(1, 2, 6), "h", "%d hour{s} ago") ++
    options(1 to 6, "d", "%d day{s} ago") ++
    options(1 to 3, "w", "%d week{s} ago") ++
    options(1 to 6, "m", "%d month{s} ago") ++
    options(1 to 5, "y", "%d year{s} ago")

  val statuses = Status.finishedNotCheated.flatMap {
    case s if s.is(_.Timeout)       => none
    case s if s.is(_.NoStart)       => none
    case s if s.is(_.UnknownFinish) => none
    case s if s.is(_.Outoftime)     => Some(s.id -> "Clock Flag")
    case s if s.is(_.VariantEnd)    => Some(s.id -> "Variant End")
    case s                          => Some(s.id -> s.toString)
  }
