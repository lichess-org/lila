package lila
package report

import socket.GetNbMembers
import akka.actor._
import akka.pattern.{ ask, pipe }
import akka.util.duration._
import akka.util.{ Duration, Timeout }
import scala.io.Source
import java.lang.management.ManagementFactory

final class Reporting extends Actor {

  private var nbMembers = 0
  private var nbGames = 0
  private var nbPlaying = 0
  private var nbGameSockets = 0
  private var loadAvg = 0f
  private var nbThreads = 0
  private var remoteAi = false

  val osStats = ManagementFactory.getOperatingSystemMXBean
  val threadStats = ManagementFactory.getThreadMXBean

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
      nbGameSockets = 0 // env.gameHubMemo.count.toInt
      loadAvg = 0 // osStats.getSystemLoadAverage.toFloat
      //nbThreads = threadStats.getThreadCount.pp
      remoteAi = env.remoteAi.currentHealth
    }
  }

  private def status = List(
    nbMembers,
    nbGames,
    nbPlaying,
    nbGameSockets,
    loadAvg.toString,
    remoteAi.fold(1, 0)
  ) mkString " "
}
