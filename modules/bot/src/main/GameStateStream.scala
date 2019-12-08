package lila.bot

import akka.actor._
import akka.stream.scaladsl._
import play.api.libs.json._

import lila.chat.Chat
import lila.chat.UserLine
import lila.common.Bus
import lila.game.actorApi.{ AbortedBy, FinishGame, MoveGameEvent }
import lila.game.Game
import lila.hub.actorApi.map.Tell
import lila.round.actorApi.BotConnected
import scala.concurrent.duration._

final class GameStateStream(
    jsonView: BotJsonView
)(implicit system: ActorSystem) {

  private case object SetOnline

  private val blueprint =
    Source.queue[Option[JsObject]](32, akka.stream.OverflowStrategy.dropHead)

  def apply(init: Game.WithInitialFen, as: chess.Color): Source[Option[JsObject], _] =
    blueprint mapMaterializedValue { queue =>
      val actor = system.actorOf(Props(mkActor(init, as, queue)))
      queue.watchCompletion.foreach { _ => actor ! PoisonPill }
    }

  private def mkActor(
    init: Game.WithInitialFen,
    as: chess.Color,
    queue: SourceQueueWithComplete[Option[JsObject]]
  ) = new Actor {

    val id = init.game.id

    var gameOver = false

    private val classifiers = List(
      MoveGameEvent makeChan id,
      "finishGame", "abortGame",
      Chat chanOf Chat.Id(id),
      Chat chanOf Chat.Id(s"$id/w")
    )

    override def preStart(): Unit = {
      super.preStart()
      Bus.subscribe(self, classifiers)
      jsonView gameFull init foreach { json =>
        // prepend the full game JSON at the start of the stream
        queue offer json.some
        // close stream if game is over
        if (init.game.finished) onGameOver
        else self ! SetOnline
      }
    }

    override def postStop(): Unit = {
      super.postStop()
      Bus.unsubscribe(self, classifiers)
      // hang around if game is over
      // so the opponent has a chance to rematch
      context.system.scheduler.scheduleOnce(if (gameOver) 10 second else 1 second) {
        setConnected(false)
      }
      queue.complete()
    }

    def receive = {
      case MoveGameEvent(g, _, _) if g.id == id => pushState(g)
      case lila.chat.actorApi.ChatLine(chatId, UserLine(username, _, text, false, false)) =>
        pushChatLine(username, text, chatId.value.size == Game.gameIdSize)
      case FinishGame(g, _, _) if g.id == id => onGameOver
      case AbortedBy(pov) if pov.gameId == id => onGameOver
      case SetOnline =>
        context.system.scheduler.scheduleOnce(6 second) {
          // gotta send a message to check if the client has disconnected
          queue offer None
          self ! SetOnline
        }
    }

    def pushState(g: Game) =
      jsonView gameState Game.WithInitialFen(g, init.fen) map some flatMap queue.offer

    def pushChatLine(username: String, text: String, player: Boolean) =
      queue offer jsonView.chatLine(username, text, player).some

    def onGameOver() = {
      gameOver = true
      self ! PoisonPill
    }
    def setConnected(v: Boolean) = Bus.publish(
      Tell(init.game.id, BotConnected(as, v)),
      "roundSocket"
    )
  }
}
