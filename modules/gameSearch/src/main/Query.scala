package lila.gameSearch

import shogi.{ Mode, Status }
import org.joda.time.DateTime

import lila.common.Json.jodaWrites
import lila.rating.RatingRange
import lila.search.Range

case class Query(
    user1: Option[String] = None,
    user2: Option[String] = None,
    winner: Option[String] = None,
    loser: Option[String] = None,
    winnerColor: Option[Int] = None,
    perf: Option[Int] = None,
    source: Option[Int] = None,
    status: Option[Int] = None,
    plies: Range[Int] = Range.none,
    averageRating: Range[Int] = Range.none,
    hasAi: Option[Boolean] = None,
    aiLevel: Range[Int] = Range.none,
    rated: Option[Boolean] = None,
    date: Range[DateTime] = Range.none,
    duration: Range[Int] = Range.none,
    clock: Clocking = Clocking(),
    sorting: Sorting = Sorting.default,
    analysed: Option[Boolean] = None,
    senteUser: Option[String] = None,
    goteUser: Option[String] = None
) {

  def nonEmpty =
    user1.nonEmpty ||
      user2.nonEmpty ||
      winner.nonEmpty ||
      loser.nonEmpty ||
      winnerColor.nonEmpty ||
      perf.nonEmpty ||
      source.nonEmpty ||
      status.nonEmpty ||
      plies.nonEmpty ||
      averageRating.nonEmpty ||
      hasAi.nonEmpty ||
      aiLevel.nonEmpty ||
      rated.nonEmpty ||
      date.nonEmpty ||
      duration.nonEmpty ||
      clock.nonEmpty ||
      analysed.nonEmpty
}

object Query {

  import lila.common.Form._
  import play.api.libs.json._

  import Range.rangeJsonWriter
  implicit private val sortingJsonWriter  = Json.writes[Sorting]
  implicit private val clockingJsonWriter = Json.writes[Clocking]
  implicit val jsonWriter                 = Json.writes[Query]

  val durations: List[(Int, String)] =
    ((30, "30 seconds") ::
      options(
        List(60, 60 * 2, 60 * 3, 60 * 5, 60 * 10, 60 * 15, 60 * 20, 60 * 30),
        _ / 60,
        "%d minute{s}"
      ).toList) :+
      (60 * 60 * 1 -> "One hour") :+
      (60 * 60 * 2 -> "Two hours") :+
      (60 * 60 * 3 -> "Three hours")

  val clockInits = List(
    (0, "0 seconds"),
    (30, "30 seconds"),
    (45, "45 seconds")
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
    _ / 60,
    "%d minute{s}"
  ).toList

  val clockIncs =
    options(List(0, 1, 2, 3, 5, 10, 15, 20, 30, 45, 60, 90, 120, 150, 180), "%d second{s}").toList

  val clockByos =
    options(List(0, 1, 2, 3, 5, 10, 15, 20, 30, 45, 60, 90, 120, 150, 180), "%d second{s}").toList

  val winnerColors = List(1 -> "Sente", 2 -> "Gote", 3 -> "None")

  val sources = lila.game.Source.searchable map { v =>
    v.id -> v.name.capitalize
  }

  val modes = Mode.all map { mode =>
    mode.id -> mode.name.capitalize
  }

  val plies = options(
    (1 to 5) ++ (10 to 45 by 5) ++ (50 to 90 by 10) ++ (100 to 300 by 25),
    "%d move{s}"
  )

  val averageRatings = (RatingRange.min to RatingRange.max by 100).toList map { e =>
    e -> s"$e Rating"
  }

  val hasAis = List(0 -> "Human opponent", 1 -> "Computer opponent")

  val aiLevels = (1 to 8) map { l =>
    l -> ("level " + l)
  }

  val dates = List("0d" -> "Now") ++
    options(List(1, 2, 6), "h", "%d hour{s} ago") ++
    options(1 to 6, "d", "%d day{s} ago") ++
    options(1 to 3, "w", "%d week{s} ago") ++
    options(1 to 6, "m", "%d month{s} ago") ++
    options(1 to 5, "y", "%d year{s} ago")

  val statuses = Status.finishedNotCheated.map {
    case s if s.is(_.Timeout)           => none
    case s if s.is(_.NoStart)           => none
    case s if s.is(_.UnknownFinish)     => none
    case s if s.is(_.Repetition)        => none
    case s if s.is(_.Draw)              => Some(s.id -> "Draw/Repetition")
    case s if s.is(_.Impasse27)         => Some(s.id -> "Impasse")
    case s if s.is(_.TryRule)           => Some(s.id -> "Try rule")
    case s if s.is(_.Outoftime)         => Some(s.id -> "Clock Flag")
    case s if s.is(_.PerpetualCheck)    => Some(s.id -> "Perpetual check")
    case s if s.is(_.RoyalsLost)        => Some(s.id -> "Royals lost")
    case s if s.is(_.BareKing)          => Some(s.id -> "Bare king")
    case s if s.is(_.SpecialVariantEnd) => Some(s.id -> "Special variant end")
    case s                              => Some(s.id -> s.toString)
  }.flatten
}
