package lila.bot

import akka.actor._
import play.api.libs.iteratee._
import play.api.libs.json._

import scala.concurrent.duration._
import chess.format.FEN
import lila.chat.Chat
import lila.chat.UserLine
import lila.game.Event.ReloadOwner
import lila.game.actorApi.{ AbortedBy, FinishGame, MoveGameEvent }
import lila.game.{ Game, GameRepo }
import lila.hub.actorApi.map.Tell
import lila.hub.actorApi.round.MoveEvent
import lila.round.actorApi.round.{ DrawNo, DrawYes }
import lila.socket.actorApi.BotConnected
import lila.user.User

final class GameStateStream(
    system: ActorSystem,
    jsonView: BotJsonView
) {

  private case object SetOnline

  def apply(me: User, init: Game.WithInitialFen, as: chess.Color): Enumerator[Option[JsObject]] = {

    val id = init.game.id

    var stream: Option[ActorRef] = None

    Concurrent.unicast[Option[JsObject]](
      onStart = channel => {
        val actor = system.actorOf(Props(new Actor {

          var gameOver = false

          private val classifiers = List(
            MoveGameEvent makeSymbol id,
            'finishGame, 'abortGame,
            Chat classify Chat.Id(id),
            Chat classify Chat.Id(s"$id/w")
          )

          override def preStart(): Unit = {
            super.preStart()
            system.lilaBus.subscribe(self, classifiers: _*)
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
            system.lilaBus.unsubscribe(self, classifiers)
            // hang around if game is over
            // so the opponent has a chance to rematch
            context.system.scheduler.scheduleOnce(if (gameOver) 10 second else 1 second) {
              setConnected(false)
            }
          }

          def receive = {
            case MoveGameEvent(g, _, _) if g.id == id => pushState(g)
            case lila.chat.actorApi.ChatLine(chatId, UserLine(username, _, text, false, false)) =>
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
          def setConnected(v: Boolean) = system.lilaBus.publish(
            Tell(init.game.id, BotConnected(as, v)),
            'roundSocket
          )
        }))
        stream = actor.some
      },
      onComplete = stream foreach { _ ! PoisonPill }
    )
  }
}
