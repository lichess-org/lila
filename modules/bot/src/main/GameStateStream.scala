package lila.bot

import akka.actor._
import play.api.libs.iteratee._
import play.api.libs.json._
import scala.concurrent.duration._

import chess.format.FEN

import lila.chat.UserLine
import lila.game.actorApi.{ FinishGame, AbortedBy }
import lila.game.{ Game, GameRepo }
import lila.hub.actorApi.map.Tell
import lila.hub.actorApi.round.MoveEvent
import lila.socket.actorApi.BotConnected
import lila.user.User

final class GameStateStream(
    system: ActorSystem,
    jsonView: BotJsonView,
    roundSocketHub: ActorSelection
) {

  import lila.common.HttpStream._

  def apply(me: User, init: Game.WithInitialFen, as: chess.Color): Enumerator[String] = {

    val id = init.game.id

    var stream: Option[ActorRef] = None

    val enumerator = Concurrent.unicast[Option[JsObject]](
      onStart = channel => {
        val actor = system.actorOf(Props(new Actor {

          var gameOver = false

          override def preStart(): Unit = {
            super.preStart()
            system.lilaBus.subscribe(
              self,
              Symbol(s"moveGame:$id"), 'finishGame, 'abortGame, Symbol(s"chat:$id"), Symbol(s"chat:$id/w")
            )
            jsonView gameFull init foreach { json =>
              // prepend the full game JSON at the start of the stream
              channel push json.some
              // close stream if game is over
              if (init.game.finished) onGameOver
              else self ! SetOnline
            }
          }

          override def postStop(): Unit = {
            super.postStop()
            system.lilaBus.unsubscribe(self)
            // hang around if game is over
            // so the opponent has a chance to rematch
            context.system.scheduler.scheduleOnce(if (gameOver) 10 second else 1 second) {
              setConnected(false)
            }
          }

          def receive = {
            case g: Game if g.id == id => pushState(g)
            case lila.chat.actorApi.ChatLine(chatId, UserLine(username, text, false, false)) =>
              pushChatLine(username, text, chatId.value.size == Game.gameIdSize)
            case FinishGame(g, _, _) if g.id == id => onGameOver
            case AbortedBy(pov) if pov.gameId == id => onGameOver

            case SetOnline =>
              setConnected(true)
              context.system.scheduler.scheduleOnce(6 second) {
                // gotta send a message to check if the client has disconnected
                channel push None
                self ! SetOnline
              }
          }

          def pushState(g: Game) = jsonView gameState Game.WithInitialFen(g, init.fen) map some map channel.push
          def pushChatLine(username: String, text: String, player: Boolean) = channel push jsonView.chatLine(username, text, player).some
          def onGameOver = {
            gameOver = true
            channel.eofAndEnd()
          }
          def setConnected(v: Boolean) =
            roundSocketHub ! Tell(init.game.id, BotConnected(as, v))
        }))
        stream = actor.some
      },
      onComplete = onComplete(stream, system)
    )

    enumerator &> stringifyOrEmpty
  }
}
