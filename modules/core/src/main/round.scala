package lila.core
package round

import _root_.chess.format.{ Fen, Uci }
import _root_.chess.{ Color, Move }
import play.api.libs.json.{ JsArray, JsObject }
import scalalib.bus.NotBuseable

import lila.core.game.{ Game, Pov }
import lila.core.id.{ GameAnyId, GameId, GamePlayerId, SimulId, TourId }
import lila.core.net.IpAddress
import lila.core.userId.UserId

case class Berserk(gameId: GameId, userId: UserId)

// for messages that also need to be sent via `lila.bus.Bus`
// only allowed to be bused via `Tell`, to include a `GameId`
enum RoundBus extends NotBuseable:
  case Abort(playerId: GamePlayerId)
  case AbortForce
  case BotConnected(color: Color, v: Boolean)
  case BotPlay(playerId: GamePlayerId, uci: Uci, promise: Option[Promise[Unit]] = None)
  case Draw(playerId: GamePlayerId, draw: Boolean)
  case FishnetPlay(uci: Uci, sign: String)
  case IsOnGame(color: Color, promise: Promise[Boolean])
  case QuietFlag
  case Rematch(playerId: GamePlayerId, rematch: Boolean)
  case Resign(playerId: GamePlayerId)
  case ResignForce(playerId: GamePlayerId)
  case DrawForce(playerId: GamePlayerId)
  case Takeback(playerId: GamePlayerId, takeback: Boolean)

case class Tell(id: GameId, msg: RoundBus)
case class TellMany(ids: Seq[GameId], msg: StartClock.type | RoundBus.QuietFlag.type)

case class MoveEvent(
    gameId: GameId,
    fen: Fen.Full,
    move: String
)
case class CorresMoveEvent(
    move: MoveEvent,
    playerUserId: Option[UserId],
    mobilePushable: Boolean,
    alarmable: Boolean,
    unlimited: Boolean
)
case class CorresTakebackOfferEvent(gameId: GameId)
case class CorresDrawOfferEvent(gameId: GameId)
case class BoardDrawEvent(gameId: GameId)
case class SimulMoveEvent(move: MoveEvent, simulId: SimulId, opponentUserId: UserId)
case class TourStandingOld(data: JsArray)
case class TourStanding(tourId: TourId, data: JsArray)
case object FishnetStart
case class RematchOffer(gameId: GameId)
case class RematchCancel(gameId: GameId)
case class Mlat(millis: Int)
case class DeleteUnplayed(gameId: GameId)
case class SocketExists(gameId: GameId, promise: Promise[Boolean])

case object Threefold
case object ResignAi
case class DrawClaim(playerId: GamePlayerId)
case class Blindfold(playerId: GamePlayerId, blindfold: Boolean)
object Moretime:
  val defaultDuration = 15.seconds
case class Moretime(
    playerId: GamePlayerId,
    seconds: FiniteDuration = Moretime.defaultDuration,
    force: Boolean = false
)
case class ClientFlag(color: Color, fromPlayerId: Option[GamePlayerId])
case object Abandon
case class ForecastPlay(lastMove: Move)
case class Cheat(color: Color)
case class HoldAlert(playerId: GamePlayerId, mean: Int, sd: Int, ip: IpAddress)
case class GoBerserk(color: Color, promise: Promise[Boolean])
case object NoStart
case object StartClock
case object TooManyPlies

opaque type IsOfferingRematch = game.PovRef => Boolean
object IsOfferingRematch extends FunctionWrapper[IsOfferingRematch, game.PovRef => Boolean]

trait BenignError extends lilaism.LilaException
case class ClientError(message: String) extends BenignError
case class FishnetError(message: String) extends BenignError
case class GameIsFinishedError(id: GameId) extends BenignError:
  val message = s"game $id is finished"

trait RoundJson:
  def mobileOffline(game: Game, id: GameAnyId): Fu[JsObject]

opaque type CurrentlyPlaying = UserId => Fu[Option[Pov]]
object CurrentlyPlaying extends FunctionWrapper[CurrentlyPlaying, UserId => Fu[Option[Pov]]]

trait RoundApi:
  def tell(gameId: GameId, msg: Matchable): Unit
  def ask[A](gameId: GameId)(makeMsg: Promise[A] => Matchable): Fu[A]
  def getGames(gameIds: List[GameId]): Fu[List[(GameId, Option[Game])]]
  def resignAllGamesOf(userId: UserId): Funit
