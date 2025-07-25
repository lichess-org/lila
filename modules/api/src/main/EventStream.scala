package lila.api

import scala.annotation.nowarn
import akka.actor.*
import akka.stream.scaladsl.*
import play.api.i18n.Lang
import play.api.libs.json.*

import lila.challenge.{ Challenge, NegativeEvent }
import lila.common.Bus
import lila.common.actorBus.*
import lila.common.Json.given
import lila.core.game.{ FinishGame, StartGame }
import lila.game.Rematches
import lila.user.{ LightUserApi, Me, UserRepo }

final class EventStream(
    challengeJsonView: lila.challenge.JsonView,
    challengeMaker: lila.challenge.ChallengeMaker,
    challengeApi: lila.challenge.ChallengeApi,
    onlineApiUsers: lila.bot.OnlineApiUsers,
    userRepo: UserRepo,
    gameJsonView: lila.game.JsonView,
    rematches: Rematches,
    lightUserApi: LightUserApi
)(using system: ActorSystem)(using Executor, Scheduler, lila.core.i18n.Translator):

  private case object SetOnline

  private val blueprint =
    Source.queue[Option[JsObject]](32, akka.stream.OverflowStrategy.dropHead)

  def apply(
      gamesInProgress: List[Game],
      challenges: List[Challenge]
  )(using me: Me): Source[Option[JsObject], ?] =

    given Lang = me.realLang | lila.core.i18n.defaultLang

    // kill previous one if any
    Bus.publishDyn(PoisonPill, s"eventStreamFor:${me.userId}")

    blueprint.mapMaterializedValue: queue =>
      gamesInProgress.map { gameJson(_, "gameStart") }.foreach(queue.offer)
      challenges.map(challengeJson("challenge")).map(some).foreach(queue.offer)

      val actor = system.actorOf(Props(mkActor(queue)))

      queue.watchCompletion().addEffectAnyway { actor ! PoisonPill }

  private def mkActor(queue: SourceQueueWithComplete[Option[JsObject]])(using me: Me)(using Lang): Actor =
    new:

      val classifiers = List(
        s"userStartGame:${me.userId}",
        s"userFinishGame:${me.userId}",
        s"rematchFor:${me.userId}",
        s"eventStreamFor:${me.userId}"
      )

      @nowarn var lastSetSeenAt = me.seenAt | me.createdAt
      @nowarn var online = true

      override def preStart(): Unit =
        super.preStart()
        Bus.subscribeActorRefDyn(self, classifiers)
        Bus.subscribeActorRef[lila.core.challenge.PositiveEvent](self)
        Bus.subscribeActorRef[NegativeEvent](self)

      override def postStop() =
        super.postStop()
        classifiers.foreach(Bus.unsubscribeActorRefDyn(self, _))
        Bus.subscribeActorRef[lila.core.challenge.PositiveEvent](self)
        Bus.subscribeActorRef[NegativeEvent](self)
        queue.complete()
        online = false

      self ! SetOnline

      def receive =

        case SetOnline =>
          onlineApiUsers.setOnline(me)

          if lastSetSeenAt.isBefore(nowInstant.minusMinutes(10)) then
            userRepo.setSeenAt(me)
            lastSetSeenAt = nowInstant

          context.system.scheduler
            .scheduleOnce(6.second):
              if online then
                // gotta send a message to check if the client has disconnected
                queue.offer(None)
                self ! SetOnline

        case StartGame(game) => queue.offer(gameJson(game, "gameStart"))

        case FinishGame(game, _) => queue.offer(gameJson(game, "gameFinish"))

        case lila.core.challenge.PositiveEvent.Create(c) if isMyChallenge(c) =>
          challengeApi
            .byId(c.id)
            .foreach:
              _.foreach: c =>
                val json = challengeJson("challenge")(c) ++ challengeCompat(c)
                lila.common.LilaFuture // give time for anon challenger to load the challenge page
                  .delay(if c.challengerIsAnon then 2.seconds else 0.seconds):
                    queue.offer(json.some).void

        case NegativeEvent.Decline(c) if isMyChallenge(c) =>
          queue.offer(challengeJson("challengeDeclined")(c).some)

        case NegativeEvent.Cancel(c) if isMyChallenge(c) =>
          queue.offer(challengeJson("challengeCanceled")(c).some)

        // pretend like the rematch is a challenge
        case lila.core.round.RematchOffer(gameId) =>
          challengeMaker
            .makeRematchFor(gameId, me)
            .foreach:
              _.foreach: c =>
                val json = challengeJson("challenge")(c) ++ challengeCompat(c)
                queue.offer(json.some)

        // pretend like the rematch cancel is a challenge cancel
        case lila.core.round.RematchCancel(gameId) =>
          rematches
            .getOffered(gameId)
            .map(_.nextId)
            .foreach: nextId =>
              challengeMaker
                .showCanceledRematchFor(gameId, me, nextId)
                .foreach:
                  _.foreach: c =>
                    val json = challengeJson("challengeCanceled")(c) ++ challengeCompat(c)
                    queue.offer(json.some)

      private def isMyChallenge(c: lila.core.challenge.Challenge) =
        me.is(c.destUserId) || me.is(c.challengerUser.map(_.id))

  private def gameJson(game: Game, tpe: String)(using me: Me) =
    Pov(game, me).map: pov =>
      Json.obj(
        "type" -> tpe,
        "game" -> {
          gameJsonView
            .ownerPreview(pov)(using lightUserApi.sync)
            .add("source" -> game.source.map(_.name)) ++ compatJson(
            bot = me.isBot && lila.game.Game.isBotCompatible(game).|(true),
            board = lila.game.Game.isBoardCompatible(game)
          ) ++ Json.obj(
            "id" -> game.id // API BC
          )
        }
      )

  private def challengeJson(tpe: String)(c: Challenge)(using Lang) =
    Json.obj(
      "type" -> tpe,
      "challenge" -> challengeJsonView(none)(c)
    )

  private def challengeCompat(c: Challenge)(using me: Me) = compatJson(
    bot = me.isBot && c.isBotCompatible,
    board = c.isBoardCompatible
  )

  private def compatJson(bot: Boolean, board: Boolean) =
    Json.obj("compat" -> Json.obj("bot" -> bot, "board" -> board))
