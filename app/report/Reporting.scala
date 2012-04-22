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
  private var nbGameSockets = 0
  private var loadAvg: Option[Float] = None
  private var remoteAi = false

  private val loadAvgFile = "/proc/loadavg"

  implicit val timeout = Timeout(200 millis)

  def receive = {

    case GetNbMembers ⇒ sender ! nbMembers

    case GetNbGames   ⇒ sender ! nbGames

    case GetNbPlaying ⇒ sender ! nbPlaying

    case GetStatus    ⇒ sender ! status

    case Update(env) ⇒ {
      (env.siteHub ? GetNbMembers).mapTo[Int] onSuccess {
        case nb ⇒ nbMembers = nb
      }
      nbGames = env.gameRepo.countAll.unsafePerformIO
      nbPlaying = env.gameRepo.countPlaying.unsafePerformIO
      nbGameSockets = env.gameHubMemo.count.toInt
      loadAvg = getLoadAvg
      remoteAi = env.remoteAi.currentHealth
    }
  }

  private def getLoadAvg: Option[Float] = {
    val source = Source.fromFile(loadAvgFile)
    try {
      val lines = source.mkString
      parseFloatOption(lines takeWhile (_ != ' '))
    } finally {
      source.close()
    }
  }

  private def status = List(
    nbMembers,
    nbGames,
    nbPlaying,
    nbGameSockets,
    loadAvg.fold(_.toString, "?"),
    remoteAi.fold(1, 0)
  ) mkString " "
}
