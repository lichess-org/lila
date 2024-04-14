package lila.core
package game

import cats.derived.*
import play.api.libs.json.*
import reactivemongo.api.bson.BSONDocumentHandler
import reactivemongo.api.bson.collection.BSONCollection
import _root_.chess.{ Color, ByColor, Speed, Ply }
import _root_.chess.format.Fen

import lila.core.id.{ GameId, GameFullId, GamePlayerId }
import lila.core.userId.UserId
import lila.core.rating.data.{ IntRating, IntRatingDiff, RatingProvisional }
import lila.core.perf.UserWithPerfs

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

case class TvSelect(gameId: GameId, speed: Speed, channel: String, data: JsObject)
case class ChangeFeatured(mgs: JsObject)

case class StartGame(game: Game)
case class InsertGame(game: Game)
case class FinishGame(
    game: Game,
    // users and perfs BEFORE the game result is applied
    usersBeforeGame: ByColor[Option[UserWithPerfs]]
)
case class AbortedBy(pov: Pov)

case class CorresAlarmEvent(userId: UserId, pov: Pov, opponent: String)

case class WithInitialFen(game: Game, fen: Option[Fen.Full])

opaque type Blurs = Long
object Blurs extends OpaqueLong[Blurs]

enum Source(val id: Int) derives Eq:
  def name = toString.toLowerCase
  case Lobby      extends Source(id = 1)
  case Friend     extends Source(id = 2)
  case Ai         extends Source(id = 3)
  case Api        extends Source(id = 4)
  case Arena      extends Source(id = 5)
  case Position   extends Source(id = 6)
  case Import     extends Source(id = 7)
  case ImportLive extends Source(id = 9)
  case Simul      extends Source(id = 10)
  case Relay      extends Source(id = 11)
  case Pool       extends Source(id = 12)
  case Swiss      extends Source(id = 13)

object Source:
  val byId                           = values.mapBy(_.id)
  val searchable                     = List(Lobby, Friend, Ai, Position, Import, Arena, Simul, Pool, Swiss)
  val expirable                      = Set(Lobby, Arena, Pool, Swiss)
  def apply(id: Int): Option[Source] = byId.get(id)

trait Event:
  def typ: String
  def data: JsValue
  def only: Option[Color]   = None
  def owner: Boolean        = false
  def watcher: Boolean      = false
  def troll: Boolean        = false
  def moveBy: Option[Color] = None

trait GameApi:
  def getSourceAndUserIds(id: GameId): Fu[(Option[Source], List[UserId])]
  def incBookmarks(id: GameId, by: Int): Funit

abstract class GameRepo(val coll: BSONCollection):
  given gameHandler: BSONDocumentHandler[Game]
  val light: GameLightRepo
  def game(gameId: GameId): Fu[Option[Game]]
  def gameFromSecondary(gameId: GameId): Fu[Option[Game]]
  def gamesFromSecondary(gameIds: Seq[GameId]): Fu[List[Game]]
  def gameOptionsFromSecondary(gameIds: Seq[GameId]): Fu[List[Option[Game]]]
  def getSourceAndUserIds(id: GameId): Fu[(Option[Source], List[UserId])]
  def initialFen(gameId: GameId): Fu[Option[Fen.Full]]
  def initialFen(game: Game): Fu[Option[Fen.Full]]
  def withInitialFen(game: Game): Fu[WithInitialFen]
  def isAnalysed(game: Game): Fu[Boolean]

trait GameProxy:
  def updateIfPresent(gameId: GameId)(f: Game => Game): Funit
  def game(gameId: GameId): Fu[Option[Game]]
  def upgradeIfPresent(games: List[Game]): Fu[List[Game]]
  def flushIfPresent(gameId: GameId): Funit

object BSONFields:
  val id         = "_id"
  val playerUids = "us"
  val winnerId   = "wid"
  val createdAt  = "ca"
  val movedAt    = "ua" // ua = updatedAt (bc)

def interleave[A](a: Seq[A], b: Seq[A]): Vector[A] =
  val iterA   = a.iterator
  val iterB   = b.iterator
  val builder = Vector.newBuilder[A]
  while iterA.hasNext && iterB.hasNext do builder += iterA.next() += iterB.next()
  builder ++= iterA ++= iterB

  builder.result()
