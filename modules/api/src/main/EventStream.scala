package lila.api

import akka.actor.*
import akka.stream.scaladsl.*
import play.api.libs.json.*

import lila.challenge.Challenge
import lila.common.Bus
import lila.common.Json.given
import lila.game.actorApi.{ FinishGame, StartGame }
import lila.game.{ Game, Pov, Rematches }
import lila.user.{ LightUserApi, User, UserRepo }

final class EventStream(
    challengeJsonView: lila.challenge.JsonView,
    challengeMaker: lila.challenge.ChallengeMaker,
    onlineApiUsers: lila.bot.OnlineApiUsers,
    userRepo: UserRepo,
    gameJsonView: lila.game.JsonView,
    rematches: Rematches,
    lightUserApi: LightUserApi
)(using
    ec: Executor,
    system: ActorSystem,
    scheduler: Scheduler
):

  private case object SetOnline

  private val blueprint =
    Source.queue[Option[JsObject]](32, akka.stream.OverflowStrategy.dropHead)

  def apply(
      me: User,
      gamesInProgress: List[Game],
      challenges: List[Challenge]
  ): Source[Option[JsObject], ?] =

    // kill previous one if any
    Bus.publish(PoisonPill, s"eventStreamFor:${me.id}")

    blueprint mapMaterializedValue { queue =>
      gamesInProgress map { gameJson(_, "gameStart", me) } foreach queue.offer
      challenges map challengeJson("challenge") map some foreach queue.offer

      val actor = system.actorOf(Props(mkActor(me, queue)))

      queue.watchCompletion().addEffectAnyway {
        actor ! PoisonPill
      }
    }

  private def mkActor(me: User, queue: SourceQueueWithComplete[Option[JsObject]]): Actor = new:

    val classifiers = List(
      s"userStartGame:${me.id}",
      s"userFinishGame:${me.id}",
      s"rematchFor:${me.id}",
      s"eventStreamFor:${me.id}",
      "challenge"
    )

    var lastSetSeenAt = me.seenAt | me.createdAt
    var online        = true

    override def preStart(): Unit =
      super.preStart()
      Bus.subscribe(self, classifiers)

    override def postStop() =
      super.postStop()
      Bus.unsubscribe(self, classifiers)
      queue.complete()
      online = false

    self ! SetOnline

    def receive =

      case SetOnline =>
        onlineApiUsers.setOnline(me.id)

        if (lastSetSeenAt isBefore nowInstant.minusMinutes(10))
          userRepo setSeenAt me.id
          lastSetSeenAt = nowInstant

        context.system.scheduler
          .scheduleOnce(6 second) {
            if (online)
              // gotta send a message to check if the client has disconnected
              queue offer None
              self ! SetOnline
          }
          .unit

      case StartGame(game) => queue.offer(gameJson(game, "gameStart", me)).unit

      case FinishGame(game, _, _) => queue.offer(gameJson(game, "gameFinish", me)).unit

      case lila.challenge.Event.Create(c) if isMyChallenge(c) =>
        val json = challengeJson("challenge")(c) ++ challengeCompat(c, me)
        lila.common.LilaFuture // give time for anon challenger to load the challenge page
          .delay(if (c.challengerIsAnon) 2.seconds else 0.seconds) {
            queue.offer(json.some).void
          }
          .unit

      case lila.challenge.Event.Decline(c) if isMyChallenge(c) =>
        queue.offer(challengeJson("challengeDeclined")(c).some).unit

      case lila.challenge.Event.Cancel(c) if isMyChallenge(c) =>
        queue.offer(challengeJson("challengeCanceled")(c).some).unit

      // pretend like the rematch is a challenge
      case lila.hub.actorApi.round.RematchOffer(gameId) =>
        challengeMaker
          .makeRematchFor(gameId, me)
          .foreach:
            _.foreach: c =>
              val json = challengeJson("challenge")(c) ++ challengeCompat(c, me)
              queue offer json.some

      // pretend like the rematch cancel is a challenge cancel
      case lila.hub.actorApi.round.RematchCancel(gameId) =>
        rematches
          .getOffered(gameId)
          .map(_.nextId)
          .foreach: nextId =>
            challengeMaker
              .showCanceledRematchFor(gameId, me, nextId)
              .foreach:
                _.foreach: c =>
                  val json = challengeJson("challengeCanceled")(c) ++ challengeCompat(c, me)
                  queue.offer(json.some).unit

    private def isMyChallenge(c: Challenge) =
      c.destUserId.has(me.id) || c.challengerUserId.has(me.id)

  private def gameJson(game: Game, tpe: String, me: User) =
    Pov(game, me) map { pov =>
      Json.obj(
        "type" -> tpe,
        "game" -> {
          gameJsonView
            .ownerPreview(pov)(using lightUserApi.sync)
            .add("source" -> game.source.map(_.name)) ++ compatJson(
            bot = me.isBot && Game.isBotCompatible(game),
            board = Game.isBoardCompatible(game)
          ) ++ Json.obj(
            "id" -> game.id // API BC
          )
        }
      )
    }

  private def challengeJson(tpe: String)(c: Challenge) =
    Json.obj(
      "type"      -> tpe,
      "challenge" -> challengeJsonView(none)(c)(using lila.i18n.defaultLang)
    )

  private def challengeCompat(c: Challenge, me: User) = compatJson(
    bot = me.isBot && c.isBotCompatible,
    board = c.isBoardCompatible
  )

  private def compatJson(bot: Boolean, board: Boolean) =
    Json.obj("compat" -> Json.obj("bot" -> bot, "board" -> board))
