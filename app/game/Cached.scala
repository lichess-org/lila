package lila
package game

import akka.actor._
import akka.pattern.ask
import akka.util.duration._
import akka.util.Timeout
import akka.dispatch.{ Future, Await }
import akka.pattern.ask
import akka.util.duration._
import akka.util.Timeout

import play.api.Play.current
import play.api.libs.concurrent._

import memo.Builder

final class Cached(
    gameRepo: GameRepo,
    nbTtl: Int) {

  import Cached._

  val atMost = 5 seconds
  implicit val timeout = Timeout(atMost)

  def nbGames: Int = get(NbGames)

  def nbMates: Int = get(NbMates)

  private def get(key: Key): Int = 
    Await.result(actor ? key mapTo manifest[Int], atMost)

  private val actor = Akka.system.actorOf(Props(new Actor {

    private val cache = Builder.cache[Key, Int](nbTtl, {
      case NbGames ⇒ gameRepo.count(_.all).unsafePerformIO
      case NbMates ⇒ gameRepo.count(_.mate).unsafePerformIO
    })

    def receive = {
      case key: Key ⇒ sender ! (cache get key)
    }
  }))
}

object Cached {

  sealed trait Key

  case object NbGames extends Key
  case object NbMates extends Key
}
