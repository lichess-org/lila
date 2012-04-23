package lila
package report

import socket.GetNbMembers
import game.GetNbHubs

import akka.actor._
import akka.pattern.{ ask, pipe }
import akka.util.duration._
import akka.util.{ Duration, Timeout }
import akka.dispatch.{ Future, Promise }
import play.api.libs.concurrent._
import play.api.Play.current
import scala.io.Source
import java.lang.management.ManagementFactory

final class Reporting extends Actor {

  case class SiteSocket(nbMembers: Int)
  case class LobbySocket(nbMembers: Int)
  case class GameSocket(nbHubs: Int, nbMembers: Int)

  private var nbGames = 0
  private var nbPlaying = 0
  private var loadAvg = 0f
  private var nbThreads = 0
  private var memory = 0l
  private var site = SiteSocket(0)
  private var lobby = LobbySocket(0)
  private var game = GameSocket(0, 0)
  private var remoteAi = false

  private var displays = 0

  val osStats = ManagementFactory.getOperatingSystemMXBean
  val threadStats = ManagementFactory.getThreadMXBean
  val memoryStats = ManagementFactory.getMemoryMXBean
  implicit val executor = Akka.system.dispatcher

  implicit val timeout = Timeout(200 millis)

  def receive = {

    case GetNbMembers ⇒ sender ! site.nbMembers

    case GetNbGames   ⇒ sender ! nbGames

    case GetNbPlaying ⇒ sender ! nbPlaying

    case GetStatus    ⇒ sender ! status

    case Update(env) ⇒ {
      Future.sequence(List(
        (env.siteHub ? GetNbMembers).mapTo[Int],
        (env.lobbyHub ? GetNbMembers).mapTo[Int],
        (env.gameHubMaster ? GetNbHubs).mapTo[Int],
        (env.gameHubMaster ? GetNbMembers).mapTo[Int]
      )) onSuccess {
        case List(siteMembers, lobbyMembers, gameHubs, gameMembers) ⇒ {
          site = SiteSocket(siteMembers)
          lobby = LobbySocket(lobbyMembers)
          game = GameSocket(gameHubs, gameMembers)
          nbGames = env.gameRepo.countAll.unsafePerformIO
          nbPlaying = env.gameRepo.countPlaying.unsafePerformIO
          loadAvg = osStats.getSystemLoadAverage.toFloat
          nbThreads = threadStats.getThreadCount
          memory = memoryStats.getHeapMemoryUsage.getUsed / 1024 / 1024
          remoteAi = env.remoteAi.currentHealth

          display()
        }
      } onComplete {
        case Left(a) ⇒ throw a
        case a       ⇒
      }
    }
  }

  private def display() {

    val data = List(
      "site" -> site.nbMembers,
      "lobby" -> lobby.nbMembers,
      "game" -> game.nbMembers,
      "hubs" -> game.nbHubs,
      "threads" -> nbThreads,
      "load" -> loadAvg,
      "memory" -> memory
    )
    if (displays % 10 == 0) {
      println(data map (_._1) mkString " ")
    }
    displays = displays + 1
    data.foreach {
      case (name, value) ⇒ {
        val s = value.toString
        print(List.fill(name.size - s.size)(" ").mkString + s + " ")
      }
    }
    println
  }

  private def status = List(
    site.nbMembers,
    nbGames,
    nbPlaying,
    game.nbHubs,
    loadAvg.toString,
    remoteAi.fold(1, 0)
  ) mkString " "
}
