package lila.api

import akka.actor.*
import akka.stream.scaladsl.*
import play.api.libs.json.*

import lila.challenge.Challenge
import lila.common.Bus
import lila.common.Json.given
import lila.game.actorApi.{ FinishGame, StartGame }
import lila.game.{ Game, Pov, Rematches }
import lila.user.{ LightUserApi, Me, User, UserRepo }

final class EventStream(
    challengeJsonView: lila.challenge.JsonView,
    challengeMaker: lila.challenge.ChallengeMaker,
    onlineApiUsers: lila.bot.OnlineApiUsers,
    userRepo: UserRepo,
    gameJsonView: lila.game.JsonView,
    rematches: Rematches,
    lightUserApi: LightUserApi
)(using system: ActorSystem)(using Executor, Scheduler):

  private case object SetOnline

  private val blueprint =
    Source.queue[Option[JsObject]](32, akka.stream.OverflowStrategy.dropHead)

  def apply(
      gamesInProgress: List[Game],
      challenges: List[Challenge]
  )(using me: Me): Source[Option[JsObject], ?] =

    // kill previous one if any
    Bus.publish(PoisonPill, s"eventStreamFor:${me.userId}")

    blueprint.mapMaterializedValue: queue =>
      gamesInProgress map { gameJson(_, "gameStart") } foreach queue.offer
      challenges map challengeJson("challenge") map some foreach queue.offer

      val actor = system.actorOf(Props(mkActor(queue)))

      queue.watchCompletion().addEffectAnyway { actor ! PoisonPill }

  private def mkActor(queue: SourceQueueWithComplete[Option[JsObject]])(using me: Me): Actor = new:

    val classifiers = List(
      s"userStartGame:${me.userId}",
      s"userFinishGame:${me.userId}",
      s"rematchFor:${me.userId}",
      s"eventStreamFor:${me.userId}",
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
        onlineApiUsers.setOnline(me)

        if lastSetSeenAt isBefore nowInstant.minusMinutes(10) then
          userRepo setSeenAt me
          lastSetSeenAt = nowInstant

        context.system.scheduler
          .scheduleOnce(6 second):
            if online then
              // gotta send a message to check if the client has disconnected
              queue offer None
              self ! SetOnline

      case StartGame(game) => queue.offer(gameJson(game, "gameStart"))

      case FinishGame(game, _) => queue.offer(gameJson(game, "gameFinish"))

      case lila.challenge.Event.Create(c) if isMyChallenge(c) =>
        val json = challengeJson("challenge")(c) ++ challengeCompat(c)
        lila.common.LilaFuture // give time for anon challenger to load the challenge page
          .delay(if c.challengerIsAnon then 2.seconds else 0.seconds):
            queue.offer(json.some).void

      case lila.challenge.Event.Decline(c) if isMyChallenge(c) =>
        queue.offer(challengeJson("challengeDeclined")(c).some)

      case lila.challenge.Event.Cancel(c) if isMyChallenge(c) =>
        queue.offer(challengeJson("challengeCanceled")(c).some)

      // pretend like the rematch is a challenge
      case lila.hub.actorApi.round.RematchOffer(gameId) =>
        challengeMaker
          .makeRematchFor(gameId, me)
          .foreach:
            _.foreach: c =>
              val json = challengeJson("challenge")(c) ++ challengeCompat(c)
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
                  val json = challengeJson("challengeCanceled")(c) ++ challengeCompat(c)
                  queue.offer(json.some)

    private def isMyChallenge(c: Challenge) =
      me.is(c.destUserId) || me.is(c.challengerUserId)

  private def gameJson(game: Game, tpe: String)(using me: Me) =
    Pov(game, me).map: pov =>
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

  private def challengeJson(tpe: String)(c: Challenge) =
    Json.obj(
      "type"      -> tpe,
      "challenge" -> challengeJsonView(none)(c)(using lila.i18n.defaultLang)
    )

  private def challengeCompat(c: Challenge)(using me: Me) = compatJson(
    bot = me.isBot && c.isBotCompatible,
    board = c.isBoardCompatible
  )

  private def compatJson(bot: Boolean, board: Boolean) =
    Json.obj("compat" -> Json.obj("bot" -> bot, "board" -> board))
