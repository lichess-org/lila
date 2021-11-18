package lila.api

import akka.actor._
import akka.stream.scaladsl._
import play.api.libs.json._
import scala.concurrent.duration._
import org.joda.time.DateTime

import lila.challenge.Challenge
import lila.common.Bus
import lila.game.actorApi.StartGame
import lila.game.Game
import lila.user.{ User, UserRepo }

final class EventStream(
    challengeJsonView: lila.challenge.JsonView,
    challengeMaker: lila.challenge.ChallengeMaker,
    onlineApiUsers: lila.bot.OnlineApiUsers,
    userRepo: UserRepo
)(implicit
    ec: scala.concurrent.ExecutionContext,
    system: ActorSystem
) {

  private case object SetOnline

  private val blueprint =
    Source.queue[Option[JsObject]](32, akka.stream.OverflowStrategy.dropHead)

  def apply(
      me: User,
      gamesInProgress: List[Game],
      challenges: List[Challenge]
  ): Source[Option[JsObject], _] = {

    // kill previous one if any
    Bus.publish(PoisonPill, s"eventStreamFor:${me.id}")

    blueprint mapMaterializedValue { queue =>
      gamesInProgress map toJson map some foreach queue.offer
      challenges map toJson map some foreach queue.offer

      val actor = system.actorOf(Props(mkActor(me, queue)))

      queue.watchCompletion().foreach { _ =>
        actor ! PoisonPill
      }
    }
  }

  private def mkActor(me: User, queue: SourceQueueWithComplete[Option[JsObject]]) =
    new Actor {

      val classifiers = List(
        s"userStartGame:${me.id}",
        s"rematchFor:${me.id}",
        s"eventStreamFor:${me.id}",
        "challenge"
      )

      var lastSetSeenAt = me.seenAt | me.createdAt
      var online        = true

      override def preStart(): Unit = {
        super.preStart()
        Bus.subscribe(self, classifiers)
      }

      override def postStop() = {
        super.postStop()
        Bus.unsubscribe(self, classifiers)
        queue.complete()
        online = false
      }

      self ! SetOnline

      def receive = {

        case SetOnline =>
          onlineApiUsers.setOnline(me.id)

          if (lastSetSeenAt isBefore DateTime.now.minusMinutes(10)) {
            userRepo setSeenAt me.id
            lastSetSeenAt = DateTime.now
          }

          context.system.scheduler
            .scheduleOnce(6 second) {
              if (online) {
                // gotta send a message to check if the client has disconnected
                queue offer None
                self ! SetOnline
              }
            }
            .unit

        case StartGame(game) => queue.offer(toJson(game).some).unit

        case lila.challenge.Event.Create(c) if c.destUserId has me.id => queue.offer(toJson(c).some).unit

        // pretend like the rematch is a challenge
        case lila.hub.actorApi.round.RematchOffer(gameId) =>
          challengeMaker.makeRematchFor(gameId, me) foreach {
            _ foreach { c =>
              queue offer toJson(c.copy(_id = gameId)).some
            }
          }
      }
    }

  private def toJson(game: Game) =
    Json.obj(
      "type" -> "gameStart",
      "game" -> Json.obj("id" -> game.id)
    )
  private def toJson(c: Challenge) =
    Json.obj(
      "type"      -> "challenge",
      "challenge" -> challengeJsonView(none)(c)(lila.i18n.defaultLang)
    )
}
