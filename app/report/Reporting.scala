package lila
package report

import socket.GetNbMembers
import akka.actor._
import akka.pattern.{ ask, pipe }
import akka.util.duration._
import akka.util.{ Duration, Timeout }
import scala.io.Source

final class Reporting extends Actor {

  private var nbMembers = 0
  private var nbGames = 0
  private var nbPlaying = 0
  private var loadAvg: Option[Float] = None
  private var remoteAi = false

  private val loadAvgFile = "/proc/loadavg"

  implicit val timeout = Timeout(200 millis)

  def receive = {

    case GetNbMembers ⇒ sender ! nbMembers

    case GetNbGames   ⇒ sender ! nbGames

    case GetStatus    ⇒ sender ! status

    case Update(env) ⇒ {
      (env.siteHub ? GetNbMembers).mapTo[Int] onSuccess {
        case nb ⇒ nbMembers = nb
      }
      nbGames = env.gameRepo.countAll.unsafePerformIO
      nbPlaying = env.gameHubMemo.count.toInt
      loadAvg = parseFloatOption {
        Source.fromFile(loadAvgFile).getLines.mkString takeWhile (_ != ' ')
      }
      remoteAi = env.remoteAi.currentHealth
    }
  }

  private def status = List(
    nbMembers,
    nbGames,
    nbPlaying,
    loadAvg.fold(_.toString, "?"),
    remoteAi.fold(1, 0)
  ) mkString " "
}
