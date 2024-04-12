package lila.core

import lila.core.userId.UserId
import lila.core.rating.data.IntRating
import lila.core.rating.Glicko
import lila.core.rating.data.IntRatingDiff
import lila.core.userId.UserIdOf
import scalalib.Render

object perf:

  opaque type PerfKey = String
  object PerfKey:
    val bullet: PerfKey         = "bullet"
    val blitz: PerfKey          = "blitz"
    val rapid: PerfKey          = "rapid"
    val classical: PerfKey      = "classical"
    val correspondence: PerfKey = "correspondence"
    val standard: PerfKey       = "standard"
    val ultraBullet: PerfKey    = "ultraBullet"
    val chess960: PerfKey       = "chess960"
    val kingOfTheHill: PerfKey  = "kingOfTheHill"
    val threeCheck: PerfKey     = "threeCheck"
    val antichess: PerfKey      = "antichess"
    val atomic: PerfKey         = "atomic"
    val horde: PerfKey          = "horde"
    val racingKings: PerfKey    = "racingKings"
    val crazyhouse: PerfKey     = "crazyhouse"
    val puzzle: PerfKey         = "puzzle"
    val all: Set[PerfKey] = Set(
      bullet,
      blitz,
      rapid,
      classical,
      correspondence,
      standard,
      ultraBullet,
      chess960,
      kingOfTheHill,
      threeCheck,
      antichess,
      atomic,
      horde,
      racingKings,
      crazyhouse,
      puzzle
    )
    def apply(key: String): Option[PerfKey]    = Option.when(all.contains(key))(key)
    def read(key: PerfKeyStr): Option[PerfKey] = apply(key.value)
    extension (key: PerfKey) def value: String = key
    given Render[PerfKey]                      = _.value

  opaque type PerfKeyStr = String
  object PerfKeyStr extends OpaqueString[PerfKeyStr]

  opaque type PerfId = Int
  object PerfId extends OpaqueInt[PerfId]

  trait PerfStatApi:
    def highestRating(user: UserId, perfKey: PerfKey): Fu[Option[IntRating]]

  case class Perf(
      glicko: Glicko,
      nb: Int,
      recent: List[IntRating],
      latest: Option[Instant]
  ):
    export glicko.{ intRating, intDeviation, provisional }
    export latest.{ isEmpty, nonEmpty }

    def keyed(key: PerfKey) = KeyedPerf(key, this)

    def progress: IntRatingDiff = {
      for
        head <- recent.headOption
        last <- recent.lastOption
      yield IntRatingDiff(head.value - last.value)
    } | IntRatingDiff(0)

  case class KeyedPerf(key: PerfKey, perf: Perf)

  case class PuzPerf(score: Int, runs: Int):
    def nonEmpty = runs > 0
    def option   = nonEmpty.option(this)

  case class UserPerfs(
      id: UserId,
      bullet: Perf,
      blitz: Perf,
      rapid: Perf,
      classical: Perf,
      correspondence: Perf,
      standard: Perf,
      chess960: Perf,
      kingOfTheHill: Perf,
      threeCheck: Perf,
      antichess: Perf,
      atomic: Perf,
      horde: Perf,
      racingKings: Perf,
      crazyhouse: Perf,
      ultraBullet: Perf,
      puzzle: Perf,
      storm: PuzPerf,
      racer: PuzPerf,
      streak: PuzPerf
  ):
    def apply(key: PerfKey): Perf = key match
      case "bullet"         => bullet
      case "blitz"          => blitz
      case "rapid"          => rapid
      case "classical"      => classical
      case "correspondence" => correspondence
      case "ultraBullet"    => ultraBullet
      case "standard"       => standard
      case "chess960"       => chess960
      case "kingOfTheHill"  => kingOfTheHill
      case "threeCheck"     => threeCheck
      case "antichess"      => antichess
      case "atomic"         => atomic
      case "horde"          => horde
      case "racingKings"    => racingKings
      case "crazyhouse"     => crazyhouse
      case "puzzle"         => puzzle
      // impossible because PerfKey can't be instanciated with arbitrary values
      case key => sys.error(s"Unknown perf key: $key")

    def keyed(key: PerfKey): KeyedPerf = KeyedPerf(key, apply(key))

  case class UserWithPerfs(user: lila.core.user.User, perfs: UserPerfs):
    export user.*
  object UserWithPerfs:
    given UserIdOf[UserWithPerfs] = _.id
