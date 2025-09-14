package lila.api

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
import lila.bot.OnlineApiUsers.*
import lila.core.net.Bearer

final class EventStream(
    challengeJsonView: lila.challenge.JsonView,
    challengeMaker: lila.challenge.ChallengeMaker,
    challengeApi: lila.challenge.ChallengeApi,
    onlineApiUsers: lila.bot.OnlineApiUsers,
    userRepo: UserRepo,
    gameJsonView: lila.game.JsonView,
    rematches: Rematches,
    lightUserApi: LightUserApi
)(using system: ActorSystem, scheduler: Scheduler)(using Executor, lila.core.i18n.Translator):

  private val blueprint =
    Source.queue[Option[JsObject]](32, akka.stream.OverflowStrategy.dropHead)

  def apply(
      gamesInProgress: List[Game],
      challenges: List[Challenge],
      bearer: Bearer
  )(using me: Me): Source[Option[JsObject], ?] =

    given Lang = me.realLang | lila.core.i18n.defaultLang

    // kill previous one if any
    val killChannel = s"eventStreamForBearer:$bearer"
    Bus.publishDyn(PoisonPill, killChannel)

    blueprint.mapMaterializedValue: queue =>
      gamesInProgress.map { gameJson(_, "gameStart") }.foreach(queue.offer)
      challenges.map(challengeJson("challenge")).map(some).foreach(queue.offer)

      val actor = system.actorOf(Props(mkActor(queue, killChannel)))

      queue.watchCompletion().addEffectAnyway { actor ! PoisonPill }

  private def mkActor(queue: SourceQueueWithComplete[Option[JsObject]], killChannel: String)(using
      me: Me
  )(using Lang): Actor =
    new:

      val channels = List(
        s"userStartGame:${me.userId}",
        s"userFinishGame:${me.userId}",
        s"rematchFor:${me.userId}",
        killChannel
      )

      var lastSetSeenAt = me.seenAt | me.createdAt

      override def preStart(): Unit =
        super.preStart()
        Bus.subscribeActorRefDyn(self, channels)
        Bus.subscribeActorRef[lila.core.challenge.PositiveEvent](self)
        Bus.subscribeActorRef[NegativeEvent](self)

      override def postStop() =
        super.postStop()
        channels.foreach(Bus.unsubscribeActorRefDyn(self, _))
        Bus.subscribeActorRef[lila.core.challenge.PositiveEvent](self)
        Bus.subscribeActorRef[NegativeEvent](self)
        queue.complete()

      self ! SetOnline

      def receive =

        case SetOnline =>
          onlineApiUsers.setOnline(me)

          if lastSetSeenAt.isBefore(nowInstant.minusMinutes(10)) then
            userRepo.setSeenAt(me)
            lastSetSeenAt = nowInstant

          scheduler.scheduleOnce(7.second, self, CheckOnline)

        case CheckOnline =>
          queue.offer(None) // prevents the client and intermediate proxies from closing the idle stream
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
