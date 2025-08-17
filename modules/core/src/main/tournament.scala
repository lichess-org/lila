package lila.core
package tournament

import play.api.i18n.Lang
import scalalib.model.Seconds
import _root_.chess.ByColor

import lila.core.chess.Rank
import lila.core.id.{ TourId, TourPlayerId }
import lila.core.userId.UserId
import lila.core.game.Game

enum Status(val id: Int):
  case created extends Status(10)
  case started extends Status(20)
  case finished extends Status(30)
  def name = toString
  def is(f: Status.type => Status): Boolean = f(Status) == this

object Status:
  val byId: Map[Int, Status] = values.mapBy(_.id)
  def byName(str: String) = scala.util
    .Try(Status.valueOf(str))
    .toOption
    .orElse:
      str.toIntOption.flatMap(byId.get)

trait GetTourName:
  def sync(id: TourId)(using Lang): Option[String]
  def preload(ids: Iterable[TourId])(using Lang): Funit

trait Tournament:
  val id: TourId
  val name: String
  val status: Status
  def nbPlayers: Int
  def isFinished: Boolean
  def isStarted: Boolean
  def berserkable: Boolean
  def secondsToFinish: Seconds

trait TournamentApi:
  def allCurrentLeadersInStandard: Fu[Map[Tournament, List[UserId]]]
  def fetchModable: Fu[List[Tournament]]
  def getCached(id: TourId): Fu[Option[Tournament]]
  def getGameRanks(tour: Tournament, game: Game): Fu[Option[ByColor[Rank]]]

object leaderboard:

  opaque type Ratio = Double
  object Ratio extends OpaqueDouble[Ratio]:
    extension (a: Ratio) def percent = (a.value * 100).toInt.atLeast(1)

  trait Api:
    def timeRange(userId: UserId, range: TimeInterval): Fu[List[Entry]]

  trait Entry:
    def id: TourPlayerId
    def userId: UserId
    def tourId: TourId
    def nbGames: Int
    def score: Int
    def rank: Rank
    def rankRatio: Ratio // ratio * rankRatioMultiplier. function of rank and tour.nbPlayers. less is better.
    // freq: Option[Schedule.Freq]
    // speed: Option[Schedule.Speed]
    // perf: PerfType
    def date: Instant
