package lila.bot

import akka.actor.*
import akka.stream.scaladsl.*
import play.api.i18n.Lang
import play.api.libs.json.*
import ornicar.scalalib.ThreadLocalRandom

import lila.chat.{ Chat, UserLine }
import lila.game.actorApi.{
  AbortedBy,
  BoardDrawOffer,
  BoardGone,
  BoardTakeback,
  BoardTakebackOffer,
  FinishGame,
  MoveGameEvent
}
import lila.game.{ Game, Pov }
import lila.hub.actorApi.map.Tell
import lila.round.actorApi.BotConnected
import lila.round.actorApi.round.QuietFlag
import play.api.mvc.RequestHeader
import lila.common.{ Bus, HTTPRequest }

final class GameStateStream(
    onlineApiUsers: OnlineApiUsers,
    jsonView: BotJsonView
)(using
    ec: Executor,
    system: ActorSystem
):
  import GameStateStream.*

  private val blueprint =
    Source.queue[Option[JsObject]](32, akka.stream.OverflowStrategy.dropHead)

  def apply(init: Game.WithInitialFen, as: chess.Color)(using
      lang: Lang,
      req: RequestHeader,
      me: lila.user.Me
  ): Source[Option[JsObject], ?] =

    // terminate previous one if any
    Bus.publish(PoisonPill, uniqChan(init.game pov as))

    blueprint mapMaterializedValue { queue =>
      val actor = system.actorOf(
        Props(mkActor(init, as, User(me, me.isBot), queue)),
        name = s"GameStateStream:${init.game.id}:${ThreadLocalRandom nextString 8}"
      )
      queue.watchCompletion().addEffectAnyway {
        actor ! PoisonPill
      }
    }

  private def uniqChan(pov: Pov)(using req: RequestHeader) =
    s"gameStreamFor:${pov.fullId}:${HTTPRequest.userAgent(req) | "?"}"

  private def mkActor(
      init: Game.WithInitialFen,
      as: chess.Color,
      user: User,
      queue: SourceQueueWithComplete[Option[JsObject]]
  )(using Lang, RequestHeader): Actor = new:

    val id = init.game.id

    var gameOver = false

    private val classifiers = List(
      MoveGameEvent makeChan id,
      BoardDrawOffer makeChan id,
      BoardTakeback makeChan id,
      BoardGone makeChan id,
      "finishGame",
      "abortGame",
      uniqChan(init.game pov as),
      Chat chanOf id.into(ChatId)
    ) :::
      user.isBot.option(Chat chanOf ChatId(s"$id/w")).toList

    override def preStart(): Unit =
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
      Bus.publish(Tell(init.game.id.value, BotConnected(as, v = true)), "roundSocket")

    override def postStop(): Unit =
      super.postStop()
      Bus.unsubscribe(self, classifiers)
      // hang around if game is over
      // so the opponent has a chance to rematch
      context.system.scheduler.scheduleOnce(if (gameOver) 10 second else 1 second):
        Bus.publish(Tell(init.game.id.value, BotConnected(as, v = false)), "roundSocket")
      queue.complete()
      lila.mon.bot.gameStream("stop").increment().unit

    def receive =
      case MoveGameEvent(g, _, _) if g.id == id && !g.finished => pushState(g).unit
      case lila.chat.ChatLine(chatId, UserLine(username, _, _, text, false, false)) =>
        pushChatLine(username, text, chatId.value.lengthIs == GameId.size).unit
      case FinishGame(g, _, _) if g.id == id                              => onGameOver(g.some).unit
      case AbortedBy(pov) if pov.gameId == id                             => onGameOver(pov.game.some).unit
      case BoardDrawOffer(g) if g.id == id                                => pushState(g).unit
      case BoardTakebackOffer(g) if g.id == id                            => pushState(g).unit
      case BoardTakeback(g) if g.id == id                                 => pushState(g).unit
      case BoardGone(pov, seconds) if pov.gameId == id && pov.color != as => opponentGone(seconds).unit
      case SetOnline =>
        onlineApiUsers.setOnline(user.id)
        context.system.scheduler
          .scheduleOnce(6 second):
            // gotta send a message to check if the client has disconnected
            queue offer None
            self ! SetOnline
            Bus.publish(Tell(id.value, QuietFlag), "roundSocket")
          .unit

    def pushState(g: Game): Funit =
      jsonView gameState Game.WithInitialFen(g, init.fen) dmap some flatMap queue.offer void

    def pushChatLine(username: UserName, text: String, player: Boolean) =
      queue offer jsonView.chatLine(username, text, player).some

    def opponentGone(claimInSeconds: Option[Int]) = queue offer {
      claimInSeconds.fold(jsonView.opponentGoneIsBack)(jsonView.opponentGoneClaimIn).some
    }

    def onGameOver(g: Option[Game]) =
      g.so(pushState) >>- {
        gameOver = true
        self ! PoisonPill
      }

private object GameStateStream:

  private case object SetOnline
  private case class User(id: UserId, isBot: Boolean)
