package lila.api

import scala.concurrent.duration._
import akka.actor._
import play.api.libs.iteratee._
import play.api.libs.json._

import lila.challenge.Challenge
import lila.game.actorApi.UserStartGame
import lila.game.Game
import lila.user.User

final class EventStream(
    system: ActorSystem,
    challengeJsonView: lila.challenge.JsonView,
    setOnline: User.ID => Unit
) {

  import lila.common.HttpStream._

  def apply(me: User, gamesInProgress: List[Game], challenges: List[Challenge]): Enumerator[String] = {

    var stream: Option[ActorRef] = None

    val enumerator = Concurrent.unicast[JsObject](
      onStart = channel => {
        val actor = system.actorOf(Props(new Actor {

          gamesInProgress foreach pushGameStart
          challenges foreach pushChallenge
          self ! SetOnline

          def receive = {

            case SetOnline =>
              println(nowSeconds, s"set online ${me.id}")
              setOnline(me.id)
              context.system.scheduler.scheduleOnce(6 second, self, SetOnline)

            case UserStartGame(userId, game) if userId == me.id => pushGameStart(game)

            case lila.challenge.Event.Create(c) if c.destUserId has me.id => pushChallenge(c)
          }

          def pushGameStart(game: Game) = channel push Json.obj(
            "type" -> "gameStart",
            "game" -> Json.obj("id" -> game.id)
          )

          def pushChallenge(c: lila.challenge.Challenge) = channel push Json.obj(
            "type" -> "challenge",
            "challenge" -> challengeJsonView(none)(c)
          )
        }))
        system.lilaBus.subscribe(actor, Symbol(s"userStartGame:${me.id}"), 'challenge)
        stream = actor.some
      },
      onComplete = onComplete(stream, system)
    )

    enumerator &> stringify
  }
}
