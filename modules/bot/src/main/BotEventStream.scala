package lila.bot

import akka.actor._
import play.api.libs.iteratee._
import play.api.libs.json._

import lila.game.actorApi.UserStartGame
import lila.user.User

final class BotEventStream(
    system: ActorSystem,
    jsonView: BotJsonView
) {

  import BotStream._

  def apply(me: User): Enumerator[String] = {

    var stream: Option[ActorRef] = None

    val enumerator = Concurrent.unicast[JsObject](
      onStart = channel => {
        val actor = system.actorOf(Props(new Actor {

          def receive = {
            case UserStartGame(userId, game) if userId == me.id => channel push Json.obj(
              "type" -> "gameStart",
              "game" -> Json.obj("id" -> game.id)
            )
          }
        }))
        system.lilaBus.subscribe(actor, Symbol(s"userStartGame:${me.id}"))
        stream = actor.some
      },
      onComplete = onComplete(stream, system)
    )

    enumerator &> stringify
  }
}
