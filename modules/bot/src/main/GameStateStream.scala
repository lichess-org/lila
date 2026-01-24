package lila.bot

import akka.actor.*
import akka.stream.scaladsl.*
import play.api.i18n.Lang
import play.api.libs.json.*
import scalalib.ThreadLocalRandom

import lila.chat.{ Chat, UserLine }
import lila.common.Bus
import lila.common.actorBus.*
import lila.core.game.{ AbortedBy, FinishGame, WithInitialFen }
import lila.core.round.{ Tell, RoundBus }
import lila.core.user.KidMode
import lila.core.net.UserAgent
import lila.game.actorApi.{
  BoardDrawOffer,
  BoardGone,
  BoardTakeback,
  BoardTakebackOffer,
  BoardMoretime,
  MoveGameEvent
}
import lila.bot.OnlineApiUsers.*

final class GameStateStream(
    onlineApiUsers: OnlineApiUsers,
    jsonView: BotJsonView
)(using system: ActorSystem)(using Executor, lila.core.i18n.Translator):

  import GameStateStream.*

  private val blueprint =
    Source.queue[Option[JsObject]](32, akka.stream.OverflowStrategy.dropHead)

  def apply(init: WithInitialFen, as: Color)(using
      lang: Lang,
      ua: lila.core.net.UserAgent,
      me: Me
  ): Source[Option[JsObject], ?] =

    // terminate previous one if any
    Bus.publishDyn(PoisonPill, uniqChan(init.game.pov(as)))

    blueprint.mapMaterializedValue: queue =>
      val actor = system.actorOf(
        Props(mkActor(init, as, User(me, me.isBot, me.kid), queue)),
        name = s"GameStateStream:${init.game.id}:${ThreadLocalRandom.nextString(8)}"
      )
      queue
        .watchCompletion()
        .addEffectAnyway:
          actor ! PoisonPill

  private def uniqChan(pov: Pov)(using ua: UserAgent) =
    s"gameStreamFor:${pov.fullId}:$ua"

  private def mkActor(
      init: WithInitialFen,
      as: Color,
      user: User,
      queue: SourceQueueWithComplete[Option[JsObject]]
  )(using Lang, UserAgent): Actor = new:

    import init.game.id

    var gameOver = false

    private val classifiers = List(
      MoveGameEvent.makeChan(id),
      BoardDrawOffer.makeChan(id),
      BoardTakeback.makeChan(id),
      BoardMoretime.makeChan(id),
      BoardGone.makeChan(id),
      uniqChan(init.game.pov(as))
    ) :::
      user.kid.no.option(Chat.chanOf(id.into(ChatId))).toList :::
      user.isBot.option(Chat.chanOf(ChatId(s"$id/w"))).toList

    override def preStart(): Unit =
      super.preStart()
      Bus.subscribeActorRef[lila.core.game.FinishGame](self)
      Bus.subscribeActorRef[lila.core.game.AbortedBy](self)
      Bus.subscribeActorRefDyn(self, classifiers)
      jsonView.gameFull(init).foreach { json =>
        // prepend the full game JSON at the start of the stream
        queue.offer(json.some)
        // close stream if game is over
        if init.game.finishedOrAborted then onGameOver(none)
        else self ! SetOnline
      }
      lila.mon.bot.gameStream("start").increment()
      Bus.pub(Tell(init.game.id, RoundBus.BotConnected(as, v = true)))

    override def postStop(): Unit =
      super.postStop()
      classifiers.foreach(Bus.unsubscribeActorRefDyn(self, _))
      Bus.unsubscribeActorRef[lila.core.game.FinishGame](self)
      Bus.unsubscribeActorRef[lila.core.game.AbortedBy](self)
      // hang around if game is over
      // so the opponent has a chance to rematch
      context.system.scheduler.scheduleOnce(if gameOver then 10.second else 1.second):
        Bus.pub(Tell(init.game.id, RoundBus.BotConnected(as, v = false)))
      queue.complete()
      lila.mon.bot.gameStream("stop").increment()

    def receive =
      case MoveGameEvent(g, _, _) if g.id == id && !g.finished => pushState(g)
      case lila.chat.ChatLine(chatId, UserLine(username, text, false, false), _) =>
        pushChatLine(username, text, chatId.value.lengthIs == GameId.size)
      case FinishGame(g, _) if g.id == id => onGameOver(g.some)
      case AbortedBy(pov) if pov.gameId == id => onGameOver(pov.game.some)
      case BoardDrawOffer(g) if g.id == id => pushState(g)
      case BoardTakebackOffer(g) if g.id == id => pushState(g)
      case BoardTakeback(g) if g.id == id => pushState(g)
      case BoardMoretime(g) if g.id == id => pushState(g)
      case BoardGone(pov, seconds) if pov.gameId == id && pov.color != as => opponentGone(seconds)
      case SetOnline =>
        onlineApiUsers.setOnline(user.id)
        context.system.scheduler.scheduleOnce(7.second, self, CheckOnline)
      case CheckOnline =>
        queue.offer(None) // prevents the client and intermediate proxies from closing the idle stream
        Bus.pub(Tell(id, RoundBus.QuietFlagCheck))
        self ! SetOnline

    def pushState(g: Game): Funit =
      jsonView.gameState(WithInitialFen(g, init.fen)).dmap(some).flatMap(queue.offer).void

    def pushChatLine(username: UserName, text: String, player: Boolean) =
      queue.offer(jsonView.chatLine(username, text, player).some)

    def opponentGone(claimInSeconds: Option[Int]) = queue.offer:
      claimInSeconds.fold(jsonView.opponentGoneIsBack)(jsonView.opponentGoneClaimIn).some

    def onGameOver(g: Option[Game]) =
      for _ <- g.so(pushState)
      do
        gameOver = true
        self ! PoisonPill

private object GameStateStream:

  private case class User(id: UserId, isBot: Boolean, kid: KidMode)
