package lila.core
package round

import chess.format.Uci
import chess.Color
import chess.format.Fen
import play.api.libs.json.{ JsArray, JsObject }

case class Abort(playerId: GamePlayerId)
case class Berserk(gameId: GameId, userId: UserId)
case class BotPlay(playerId: GamePlayerId, uci: Uci, promise: Option[Promise[Unit]] = None)
case class Rematch(playerId: GamePlayerId, rematch: Boolean)
case class Resign(playerId: GamePlayerId)
case class Draw(playerId: GamePlayerId, draw: Boolean)
case class Takeback(playerId: GamePlayerId, takeback: Boolean)
case class ResignForce(playerId: GamePlayerId)
case class BotConnected(color: Color, v: Boolean)
case object QuietFlag

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
case class IsOnGame(color: chess.Color, promise: Promise[Boolean])
case class TourStandingOld(data: JsArray)
case class TourStanding(tourId: TourId, data: JsArray)
case class FishnetPlay(uci: Uci, sign: String)
case object FishnetStart
case class RematchOffer(gameId: GameId)
case class RematchCancel(gameId: GameId)
case class Mlat(millis: Int)
case class DeleteUnplayed(gameId: GameId)

case object AbortForce
case object Threefold
case object ResignAi
case class DrawForce(playerId: GamePlayerId)
case class DrawClaim(playerId: GamePlayerId)
case class Blindfold(playerId: GamePlayerId, blindfold: Boolean)
object Moretime:
  val defaultDuration = 15.seconds
case class Moretime(playerId: GamePlayerId, seconds: FiniteDuration = Moretime.defaultDuration)
case class ClientFlag(color: Color, fromPlayerId: Option[GamePlayerId])
case object Abandon
case class ForecastPlay(lastMove: chess.Move)
case class Cheat(color: Color)
case class HoldAlert(playerId: GamePlayerId, mean: Int, sd: Int, ip: IpAddress)
case class GoBerserk(color: Color, promise: Promise[Boolean])
case object NoStart
case object StartClock
case object TooManyPlies

opaque type IsOfferingRematch = game.PovRef => Boolean
object IsOfferingRematch extends FunctionWrapper[IsOfferingRematch, game.PovRef => Boolean]

trait BenignError                        extends lilaism.LilaException
case class ClientError(message: String)  extends BenignError
case class FishnetError(message: String) extends BenignError
case class GameIsFinishedError(id: GameId) extends BenignError:
  val message = s"game $id is finished"
