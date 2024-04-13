package lila.core
package game

import cats.derived.*
import play.api.libs.json.*
import reactivemongo.api.bson.BSONDocumentHandler
import reactivemongo.api.bson.collection.BSONCollection
import _root_.chess.{ Color, ByColor, Speed, Ply }

import lila.core.id.{ GameId, GameFullId, GamePlayerId }
import lila.core.userId.UserId
import lila.core.rating.data.{ IntRating, IntRatingDiff, RatingProvisional }

case class LightPlayer(
    color: Color,
    aiLevel: Option[Int],
    userId: Option[UserId] = None,
    rating: Option[IntRating] = None,
    ratingDiff: Option[IntRatingDiff] = None,
    provisional: RatingProvisional = RatingProvisional.No,
    berserk: Boolean = false
)

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

case class CorresAlarmEvent(userId: UserId, pov: Pov, opponent: String)

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

trait GameProxy:
  def updateIfPresent(gameId: GameId)(f: Game => Game): Funit
  def game(gameId: GameId): Fu[Option[Game]]
  def upgradeIfPresent(games: List[Game]): Fu[List[Game]]
  def flushIfPresent(gameId: GameId): Funit

def interleave[A](a: Seq[A], b: Seq[A]): Vector[A] =
  val iterA   = a.iterator
  val iterB   = b.iterator
  val builder = Vector.newBuilder[A]
  while iterA.hasNext && iterB.hasNext do builder += iterA.next() += iterB.next()
  builder ++= iterA ++= iterB

  builder.result()
