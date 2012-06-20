package lila
package game

import akka.actor._
import akka.dispatch.{ Future, Await }
import akka.pattern.ask
import akka.util.Duration
import akka.util.duration._
import akka.util.Timeout
import play.api.Play.current
import play.api.libs.concurrent._
import scalaz.effects._

final class Featured(
    gameRepo: GameRepo) {

  import Featured._

  def one: Option[DbGame] = Await.result(
    actor ? GetOne mapTo manifest[Option[DbGame]],
    atMost)

  private val atMost = 2.second
  private implicit val timeout = Timeout(atMost)

  private val actor = Akka.system.actorOf(Props(new Actor {

    private var oneId = none[String]

    def receive = {
      case GetOne ⇒ sender ! getOne
    }

    private def getOne = oneId flatMap fetch filter valid orElse {
      feature ~ { o ⇒ oneId = o map (_.id) }
    }

    private def fetch(id: String): Option[DbGame] =
      gameRepo.game(id).unsafePerformIO

    private def valid(game: DbGame) = true

    private def feature: Option[DbGame] =
      gameRepo.featuredCandidates.unsafePerformIO.headOption
  }))
}

object Featured {

  case object GetOne
}
