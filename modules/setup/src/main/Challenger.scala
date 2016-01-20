package lila.setup

import akka.actor._
import akka.pattern.{ ask, pipe }
import play.api.libs.json.Json
import play.twirl.api.Html

import lila.hub.actorApi.map.Tell
import lila.hub.actorApi.SendTo
import lila.hub.actorApi.setup._
import lila.user.UserRepo
import makeTimeout.short

private[setup] final class Challenger(
    roundHub: ActorSelection,
    renderer: ActorSelection,
    prefApi: lila.pref.PrefApi,
    relationApi: lila.relation.RelationApi) extends Actor {

  private val bus = context.system.lilaBus

  def receive = {

    case msg@RemindChallenge(gameId, from, to) =>
      UserRepo.named(from) zip UserRepo.named(to) zip (renderer ? msg) flatMap {
        case ((Some(fromU), Some(toU)), html: Html) =>
          prefApi.getPref(toU) zip relationApi.follows(toU.id, fromU.id) flatMap {
            case (pref, follows) =>
              lila.pref.Pref.Challenge.block(fromU, toU, pref.challenge, follows,
                fromCheat = fromU.engine && !toU.engine) match {
                case None => fuccess {
                  bus.publish(SendTo(to, Json.obj(
                    "t" -> "challengeReminder",
                    "d" -> Json.obj(
                      "id" -> gameId,
                      "html" -> html.toString
                    )
                  )), 'users)
                }
                case Some(err) => fufail(err)
              }
          }
        case _ => funit
      }

    case msg@DeclineChallenge(gameId) => roundHub ! Tell(gameId, msg)
  }
}
