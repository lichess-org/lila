package lila.bot

import akka.actor.*
import akka.stream.scaladsl.*
import play.api.i18n.Lang
import play.api.libs.json.*
import play.api.mvc.RequestHeader
import scalalib.ThreadLocalRandom

import lila.chat.{ Chat, UserLine }
import lila.common.{ Bus, HTTPRequest }
import lila.common.actorBus.*
import lila.core.game.{ AbortedBy, FinishGame, WithInitialFen }
import lila.core.misc.map.Tell
import lila.core.round.{ BotConnected, QuietFlag }
import lila.game.actorApi.{ BoardDrawOffer, BoardGone, BoardTakeback, BoardTakebackOffer, MoveGameEvent }

final class GameStateStream(
    onlineApiUsers: OnlineApiUsers,
    jsonView: BotJsonView
)(using system: ActorSystem)(using Executor, lila.core.i18n.Translator):

  import GameStateStream.*

  private val blueprint =
    Source.queue[Option[JsObject]](32, akka.stream.OverflowStrategy.dropHead)

  def apply(init: WithInitialFen, as: Color)(using
      lang: Lang,
      req: RequestHeader,
      me: Me
  ): Source[Option[JsObject], ?] =

    // terminate previous one if any
    Bus.publish(PoisonPill, uniqChan(init.game.pov(as)))

    blueprint.mapMaterializedValue: queue =>
      val actor = system.actorOf(
        Props(mkActor(init, as, User(me, me.isBot), queue)),
        name = s"GameStateStream:${init.game.id}:${ThreadLocalRandom.nextString(8)}"
      )
      queue
        .watchCompletion()
        .addEffectAnyway:
          actor ! PoisonPill

  private def uniqChan(pov: Pov)(using req: RequestHeader) =
    s"gameStreamFor:${pov.fullId}:${HTTPRequest.userAgent(req) | "?"}"

  private def mkActor(
      init: WithInitialFen,
      as: Color,
      user: User,
      queue: SourceQueueWithComplete[Option[JsObject]]
  )(using Lang, RequestHeader): Actor = new:

    val id = init.game.id

    var gameOver = false

    private val classifiers = List(
      MoveGameEvent.makeChan(id),
      BoardDrawOffer.makeChan(id),
      BoardTakeback.makeChan(id),
      BoardGone.makeChan(id),
      "finishGame",
      "abortGame",
      uniqChan(init.game.pov(as)),
      Chat.chanOf(id.into(ChatId))
    ) :::
      user.isBot.option(Chat.chanOf(ChatId(s"$id/w"))).toList

    override def preStart(): Unit =
      super.preStart()
      Bus.subscribe(self, classifiers)
      jsonView.gameFull(init).foreach { json =>
        // prepend the full game JSON at the start of the stream
        queue.offer(json.some)
        // close stream if game is over
        if init.game.finished then onGameOver(none)
        else self ! SetOnline
      }
      lila.mon.bot.gameStream("start").increment()
      Bus.publish(Tell(init.game.id.value, BotConnected(as, v = true)), "roundSocket")

    override def postStop(): Unit =
      super.postStop()
      classifiers.foreach(Bus.unsubscribe(self, _))
      // hang around if game is over
      // so the opponent has a chance to rematch
      context.system.scheduler.scheduleOnce(if gameOver then 10.second else 1.second):
        Bus.publish(Tell(init.game.id.value, BotConnected(as, v = false)), "roundSocket")
      queue.complete()
      lila.mon.bot.gameStream("stop").increment()

    def receive =
      case MoveGameEvent(g, _, _) if g.id == id && !g.finished => pushState(g)
      case lila.chat.ChatLine(chatId, UserLine(username, _, _, _, text, false, false), _) =>
        pushChatLine(username, text, chatId.value.lengthIs == GameId.size)
      case FinishGame(g, _) if g.id == id                                 => onGameOver(g.some)
      case AbortedBy(pov) if pov.gameId == id                             => onGameOver(pov.game.some)
      case BoardDrawOffer(g) if g.id == id                                => pushState(g)
      case BoardTakebackOffer(g) if g.id == id                            => pushState(g)
      case BoardTakeback(g) if g.id == id                                 => pushState(g)
      case BoardGone(pov, seconds) if pov.gameId == id && pov.color != as => opponentGone(seconds)
      case SetOnline =>
        onlineApiUsers.setOnline(user.id)
        context.system.scheduler
          .scheduleOnce(6.second):
            // gotta send a message to check if the client has disconnected
            queue.offer(None)
            self ! SetOnline
            Bus.publish(Tell(id.value, QuietFlag), "roundSocket")

    def pushState(g: Game): Funit =
      jsonView.gameState(WithInitialFen(g, init.fen)).dmap(some).flatMap(queue.offer).void

    def pushChatLine(username: UserName, text: String, player: Boolean) =
      queue.offer(jsonView.chatLine(username, text, player).some)

    def opponentGone(claimInSeconds: Option[Int]) = queue.offer:
      claimInSeconds.fold(jsonView.opponentGoneIsBack)(jsonView.opponentGoneClaimIn).some

    def onGameOver(g: Option[Game]) =
      for _ <- g.so(pushState)
      yield
        gameOver = true
        self ! PoisonPill

private object GameStateStream:

  private case object SetOnline
  private case class User(id: UserId, isBot: Boolean)
