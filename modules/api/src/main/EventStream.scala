package lidraughts.api

import akka.actor._
import play.api.libs.iteratee._
import play.api.libs.json._
import scala.concurrent.duration._

import lidraughts.challenge.{ Challenge, ChallengeMaker }
import lidraughts.game.actorApi.UserStartGame
import lidraughts.game.Game
import lidraughts.user.User

final class EventStream(
    system: ActorSystem,
    challengeJsonView: lidraughts.challenge.JsonView,
    setOnline: User.ID => Unit
) {

  import lidraughts.common.HttpStream._

  def apply(me: User, gamesInProgress: List[Game], challenges: List[Challenge]): Enumerator[Option[JsObject]] = {

    var stream: Option[ActorRef] = None

    val enumerator = Concurrent.unicast[Option[JsObject]](
      onStart = channel => {
        val actor = system.actorOf(Props(new Actor {

          self ! SetOnline

          def receive = {

            case SetOnline =>
              setOnline(me.id)
              context.system.scheduler.scheduleOnce(6 second) {
                // gotta send a message to check if the client has disconnected
                channel push None
                self ! SetOnline
              }

            case UserStartGame(userId, game) if userId == me.id => channel push toJson(game).some

            case lidraughts.challenge.Event.Create(c) if c.destUserId has me.id => channel push toJson(c).some

            // pretend like the rematch is a challenge
            case lidraughts.hub.actorApi.round.RematchOffer(gameId) => ChallengeMaker.makeRematchFor(gameId, me) foreach {
              _ foreach { c =>
                channel push toJson(c.copy(_id = gameId)).some
              }
            }
          }
        }))
        system.lidraughtsBus.subscribe(
          actor,
          Symbol(s"userStartGame:${me.id}"),
          Symbol(s"rematchFor:${me.id}"),
          'challenge
        )
        stream = actor.some
      },
      onComplete = onComplete(stream, system)
    )

    lidraughts.common.Iteratee.prepend(
      ((gamesInProgress map toJson) ::: (challenges map toJson)) map some,
      enumerator
    )
  }

  private def toJson(game: Game) = Json.obj(
    "type" -> "gameStart",
    "game" -> Json.obj("id" -> game.id)
  )
  private def toJson(c: Challenge) = Json.obj(
    "type" -> "challenge",
    "challenge" -> challengeJsonView(none)(c)
  )
}
