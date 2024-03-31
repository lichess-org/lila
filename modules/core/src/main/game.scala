package lila.core
package game

import cats.derived.*
import play.api.libs.json.*
import chess.Color

case class PovRef(gameId: GameId, color: Color):
  def unary_!           = PovRef(gameId, !color)
  override def toString = s"$gameId/${color.name}"

case class PlayerRef(gameId: GameId, playerId: GamePlayerId)
object PlayerRef:
  def apply(fullId: GameFullId): PlayerRef = PlayerRef(fullId.gameId, fullId.playerId)

enum GameRule:
  case noAbort, noRematch, noGiveTime, noClaimWin, noEarlyDraw
object GameRule:
  val byKey = values.mapBy(_.toString)

opaque type OnStart = GameId => Unit
object OnStart extends FunctionWrapper[OnStart, GameId => Unit]

case class TvSelect(gameId: GameId, speed: chess.Speed, channel: String, data: JsObject)
case class ChangeFeatured(mgs: JsObject)

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

trait GameRepo:
  def getSourceAndUserIds(id: GameId): Fu[(Option[Source], List[UserId])]
