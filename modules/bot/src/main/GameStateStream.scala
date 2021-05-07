package lila.bot

import akka.actor._
import akka.stream.scaladsl._
import play.api.i18n.Lang
import play.api.libs.json._
import scala.concurrent.duration._

import lila.chat.Chat
import lila.chat.UserLine
import lila.common.Bus
import lila.game.actorApi.{ AbortedBy, FinishGame, MoveGameEvent }
import lila.game.{ Game, Pov }
import lila.hub.actorApi.map.Tell
import lila.round.actorApi.BotConnected
import lila.round.actorApi.round.QuietFlag

final class GameStateStream(
    onlineApiUsers: OnlineApiUsers,
    jsonView: BotJsonView
)(implicit
    ec: scala.concurrent.ExecutionContext,
    system: ActorSystem
) {
  import GameStateStream._

  private val blueprint =
    Source.queue[Option[JsObject]](32, akka.stream.OverflowStrategy.dropHead)

  def apply(init: Game.WithInitialFen, as: chess.Color, u: lila.user.User)(implicit
      lang: Lang
  ): Source[Option[JsObject], _] = {

    // terminate previous one if any
    Bus.publish(PoisonPill, uniqChan(init.game pov as))

    blueprint mapMaterializedValue { queue =>
      val actor = system.actorOf(
        Props(mkActor(init, as, User(u.id, u.isBot), queue)),
        name = s"GameStateStream:${init.game.id}:${lila.common.ThreadLocalRandom nextString 8}"
      )
      queue.watchCompletion().foreach { _ =>
        actor ! PoisonPill
      }
    }
  }

  private def uniqChan(pov: Pov) = s"gameStreamFor:${pov.fullId}"

  private def mkActor(
      init: Game.WithInitialFen,
      as: chess.Color,
      user: User,
      queue: SourceQueueWithComplete[Option[JsObject]]
  )(implicit lang: Lang) =
    new Actor {

      val id = init.game.id

      var gameOver = false

      private val classifiers = List(
        MoveGameEvent makeChan id,
        s"boardDrawOffer:$id",
        "finishGame",
        "abortGame",
        uniqChan(init.game pov as),
        Chat chanOf Chat.Id(id)
      ) :::
        user.isBot.option(Chat chanOf Chat.Id(s"$id/w")).toList

      override def preStart(): Unit = {
        super.preStart()
        Bus.subscribe(self, classifiers)
        jsonView gameFull init foreach { json =>
          // prepend the full game JSON at the start of the stream
          queue offer json.some
          // close stream if game is over
          if (init.game.finished) onGameOver(none)
          else self ! SetOnline
        }
        lila.mon.bot.gameStream("start").increment()
        Bus.publish(Tell(init.game.id, BotConnected(as, v = true)), "roundSocket")
      }

      override def postStop(): Unit = {
        super.postStop()
        Bus.unsubscribe(self, classifiers)
        // hang around if game is over
        // so the opponent has a chance to rematch
        context.system.scheduler.scheduleOnce(if (gameOver) 10 second else 1 second) {
          Bus.publish(Tell(init.game.id, BotConnected(as, v = false)), "roundSocket")
        }
        queue.complete()
        lila.mon.bot.gameStream("stop").increment().unit
      }

      def receive = {
        case MoveGameEvent(g, _, _) if g.id == id && !g.finished => pushState(g).unit
        case lila.chat.actorApi.ChatLine(chatId, UserLine(username, _, text, false, false)) =>
          pushChatLine(username, text, chatId.value.lengthIs == Game.gameIdSize).unit
        case FinishGame(g, _, _) if g.id == id                          => onGameOver(g.some).unit
        case AbortedBy(pov) if pov.gameId == id                         => onGameOver(pov.game.some).unit
        case lila.game.actorApi.BoardDrawOffer(pov) if pov.gameId == id => pushState(pov.game).unit
        case SetOnline =>
          onlineApiUsers.setOnline(user.id)
          context.system.scheduler
            .scheduleOnce(6 second) {
              // gotta send a message to check if the client has disconnected
              queue offer None
              self ! SetOnline
              Bus.publish(Tell(id, QuietFlag), "roundSocket")
            }
            .unit
      }

      def pushState(g: Game): Funit =
        jsonView gameState Game.WithInitialFen(g, init.fen) dmap some flatMap queue.offer void

      def pushChatLine(username: String, text: String, player: Boolean): Funit =
        queue offer jsonView.chatLine(username, text, player).some void

      def onGameOver(g: Option[Game]) =
        g ?? pushState >>- {
          gameOver = true
          self ! PoisonPill
        }
    }
}

private object GameStateStream {

  private case object SetOnline
  private case class User(id: lila.user.User.ID, isBot: Boolean)
}
