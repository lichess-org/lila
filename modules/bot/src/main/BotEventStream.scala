package lila.bot

import akka.actor._
import play.api.libs.iteratee._
import play.api.libs.json._

import lila.game.actorApi.UserStartGame
import lila.game.Game
import lila.user.User
import lila.challenge.Challenge

final class BotEventStream(
    system: ActorSystem,
    challengeJsonView: lila.challenge.JsonView
) {

  import BotStream._

  def apply(me: User, gamesInProgress: List[Game], challenges: List[Challenge]): Enumerator[String] = {

    var stream: Option[ActorRef] = None

    val enumerator = Concurrent.unicast[JsObject](
      onStart = channel => {
        val actor = system.actorOf(Props(new Actor {

          gamesInProgress foreach pushGameStart
          challenges foreach pushChallenge

          def receive = {

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
