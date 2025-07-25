package lila.core
package game

import _root_.chess.format.Fen
import _root_.chess.format.pgn.{ Pgn, SanStr, Tags }
import _root_.chess.variant.Variant
import _root_.chess.{ ByColor, Centis, Clock, Color, Division, Ply, Speed, Status }
import cats.derived.*
import play.api.libs.json.*
import reactivemongo.akkastream.AkkaStreamCursor
import reactivemongo.api.bson.collection.BSONCollection
import reactivemongo.api.bson.{ BSONDocumentHandler, BSONHandler }

import lila.core.id.{ GameFullId, GameId, GamePlayerId, TeamId }
import lila.core.perf.{ PerfKey, UserWithPerfs }
import lila.core.user.User
import lila.core.userId.{ UserId, UserIdOf }

val maxPlaying = Max(200) // including correspondence
val maxPlayingRealtime = Max(100)
val favOpponentOverGames = 1000

case class PlayerRef(gameId: GameId, playerId: GamePlayerId)
object PlayerRef:
  def apply(fullId: GameFullId): PlayerRef = PlayerRef(fullId.gameId, fullId.playerId)

case class SideAndStart(color: Color, startedAtPly: Ply):
  def startColor = startedAtPly.turn

enum GameRule:
  case noAbort, noRematch, noGiveTime, noClaimWin, noEarlyDraw
object GameRule:
  val byKey = values.mapBy(_.toString)

opaque type OnStart = GameId => Unit
object OnStart extends FunctionWrapper[OnStart, GameId => Unit]

case class GameStart(id: GameId)
case class PerfsUpdate(game: Game, perfs: ByColor[UserWithPerfs])

case class TvSelect(gameId: GameId, speed: Speed, channel: String, data: JsObject)
case class ChangeFeatured(mgs: JsObject)

case class StartGame(game: Game)
case class FinishGame(
    game: Game,
    // users and perfs BEFORE the game result is applied
    usersBeforeGame: ByColor[Option[UserWithPerfs]]
)
case class AbortedBy(pov: Pov)

case class CorresAlarmEvent(userId: UserId, pov: Pov, opponent: String)

case class WithInitialFen(game: Game, fen: Option[Fen.Full])

opaque type Blurs = Long
object Blurs extends OpaqueLong[Blurs]:
  extension (bits: Blurs) def nb: Int = java.lang.Long.bitCount(bits.value)

enum Source(val id: Int) derives Eq:
  def name = toString.toLowerCase
  case Lobby extends Source(id = 1)
  case Friend extends Source(id = 2)
  case Ai extends Source(id = 3)
  case Api extends Source(id = 4)
  case Arena extends Source(id = 5)
  case Position extends Source(id = 6)
  case Import extends Source(id = 7)
  case ImportLive extends Source(id = 9) // wut?
  case Simul extends Source(id = 10)
  case Pool extends Source(id = 12)
  case Swiss extends Source(id = 13)

object Source:
  val byId = values.mapBy(_.id)
  val byName = values.mapBy(_.name)
  val searchable = List(Lobby, Friend, Ai, Position, Arena, Simul, Pool, Swiss)
  val expirable = Set(Lobby, Arena, Pool, Swiss)
  def apply(id: Int): Option[Source] = byId.get(id)

trait Event:
  def typ: String
  def data: JsValue
  def only: Option[Color] = None
  def owner: Boolean = false
  def watcher: Boolean = false
  def troll: Boolean = false
  def moveBy: Option[Color] = None

val anonCookieName = "rk2"

trait GameApi:
  def getSourceAndUserIds(id: GameId): Fu[(Option[Source], List[UserId])]
  def incBookmarks(id: GameId, by: Int): Funit
  def computeMoveTimes(g: Game, color: Color): Option[List[Centis]]
  def analysable(g: Game): Boolean
  def nbPlaying(userId: UserId): Fu[Int]
  def anonCookieJson(pov: lila.core.game.Pov): Option[JsObject]

abstract class GameRepo(val coll: BSONCollection):
  given gameHandler: BSONDocumentHandler[Game]
  given statusHandler: BSONHandler[Status]
  val light: GameLightRepo
  def game(gameId: GameId): Fu[Option[Game]]
  def gameFromSecondary(gameId: GameId): Fu[Option[Game]]
  def gamesFromSecondary(gameIds: Seq[GameId]): Fu[List[Game]]
  def gameOptionsFromSecondary(gameIds: Seq[GameId]): Fu[List[Option[Game]]]
  def getSourceAndUserIds(id: GameId): Fu[(Option[Source], List[UserId])]
  def initialFen(gameId: GameId): Fu[Option[Fen.Full]]
  def initialFen(game: Game): Fu[Option[Fen.Full]]
  def withInitialFen(game: Game): Fu[WithInitialFen]
  def gameWithInitialFen(gameId: GameId): Fu[Option[WithInitialFen]]
  def isAnalysed(game: Game): Fu[Boolean]
  def insertDenormalized(g: Game, initialFen: Option[Fen.Full] = None): Funit
  def recentAnalysableGamesByUserId(userId: UserId, nb: Int): Fu[List[Game]]
  def lastGamesBetween(u1: User, u2: User, since: Instant, nb: Int): Fu[List[Game]]
  def analysed(id: GameId): Fu[Option[Game]]
  def setAnalysed(id: GameId, v: Boolean): Funit
  def finish(id: GameId, winnerColor: Option[Color], winnerId: Option[UserId], status: Status): Funit
  def remove(id: GameId): Funit
  def countWhereUserTurn(userId: UserId): Fu[Int]
  def sortedCursor(user: UserId, pk: PerfKey): AkkaStreamCursor[Game]

trait GameProxy:
  def updateIfPresent(gameId: GameId)(f: Update[Game]): Funit
  def game(gameId: GameId): Fu[Option[Game]]
  def gameIfPresent(gameId: GameId): Fu[Option[Game]]
  def pov[U: UserIdOf](gameId: GameId, user: U): Fu[Option[Pov]]
  def upgradeIfPresent(games: List[Game]): Fu[List[Game]]
  def flushIfPresent(gameId: GameId): Funit

trait UciMemo:
  def get(game: Game): Fu[Vector[String]]
  def sign(game: Game): Fu[String]

trait PgnDump:
  def apply(
      game: Game,
      initialFen: Option[Fen.Full],
      flags: PgnDump.WithFlags,
      teams: Option[ByColor[TeamId]] = None
  ): Fu[Pgn]
  def tags(
      game: Game,
      initialFen: Option[Fen.Full],
      importedTags: Option[Tags],
      withOpening: Boolean,
      withRating: Boolean,
      teams: Option[ByColor[TeamId]] = None
  ): Fu[Tags]

trait Namer:
  def gameVsText(game: Game, withRatings: Boolean = false)(using lightUser: LightUser.Getter): Fu[String]
  def playerText(player: Player, withRating: Boolean = false)(using lightUser: LightUser.Getter): Fu[String]
  def gameVsTextBlocking(game: Game, withRatings: Boolean = false)(using
      lightUser: LightUser.GetterSync
  ): String
  def playerTextBlocking(player: Player, withRating: Boolean = false)(using
      lightUser: LightUser.GetterSync
  ): String

trait Explorer:
  def apply(id: GameId): Fu[Option[Game]]

trait Divider:
  def apply(id: GameId, sans: => Vector[SanStr], variant: Variant, initialFen: Option[Fen.Full]): Division

object PgnDump:
  case class WithFlags(
      clocks: Boolean = true,
      moves: Boolean = true,
      tags: Boolean = true,
      evals: Boolean = true,
      opening: Boolean = true,
      rating: Boolean = true,
      literate: Boolean = false,
      pgnInJson: Boolean = false,
      delayMoves: Boolean = false,
      lastFen: Boolean = false,
      accuracy: Boolean = false,
      division: Boolean = false,
      bookmark: Boolean = false
  ):
    def requiresAnalysis = evals || accuracy
    def keepDelayIf(cond: Boolean) = copy(delayMoves = delayMoves && cond)

object BSONFields:
  val id = "_id"
  val playerUids = "us"
  val winnerId = "wid"
  val createdAt = "ca"
  val movedAt = "ua" // ua = updatedAt (bc)
  val turns = "t"
  val analysed = "an"
  val pgnImport = "pgni"
  val playingUids = "pl"

def allowRated(variant: Variant, clock: Option[Clock.Config]) =
  variant.standard || clock.exists: c =>
    c.estimateTotalTime >= Centis(3000) &&
      c.limitSeconds > 0 || c.incrementSeconds > 1

def isBoardCompatible(clock: Clock.Config): Boolean = Speed(clock) >= Speed.Rapid
def isBotCompatible(clock: Clock.Config): Boolean = Speed(clock) >= Speed.Bullet

def interleave[A](a: Seq[A], b: Seq[A]): Vector[A] =
  val iterA = a.iterator
  val iterB = b.iterator
  val builder = Vector.newBuilder[A]
  while iterA.hasNext && iterB.hasNext do builder += iterA.next() += iterB.next()
  builder ++= iterA ++= iterB

  builder.result()

def reasonableMinimumNumberOfMoves(variant: Variant): Int =
  import _root_.chess.variant.*
  variant.match
    case Standard | Chess960 | Horde => 20
    case Antichess | Crazyhouse | KingOfTheHill => 15
    case ThreeCheck | Atomic | RacingKings => 10
    case _ => 15 // from position
