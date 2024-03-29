package lila.hub
package game

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

trait Event:
  def typ: String
  def data: JsValue
  def only: Option[Color]   = None
  def owner: Boolean        = false
  def watcher: Boolean      = false
  def troll: Boolean        = false
  def moveBy: Option[Color] = None
