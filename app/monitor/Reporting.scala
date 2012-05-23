package lila
package monitor

import socket.GetNbMembers
import round.GetNbHubs

import akka.actor._
import akka.pattern.{ ask, pipe }
import akka.util.duration._
import akka.util.{ Duration, Timeout }
import akka.dispatch.{ Future, Promise }
import play.api.libs.concurrent._
import play.api.Play.current
import scala.io.Source
import java.lang.management.ManagementFactory

final class Reporting(
  rpsProvider: RpsProvider) extends Actor {

  case class SiteSocket(nbMembers: Int)
  case class LobbySocket(nbMembers: Int)
  case class GameSocket(nbHubs: Int, nbMembers: Int)

  var nbGames = 0
  var nbPlaying = 0
  var loadAvg = 0f
  var nbThreads = 0
  var memory = 0l
  var latency = 0
  var site = SiteSocket(0)
  var lobby = LobbySocket(0)
  var game = GameSocket(0, 0)
  var rps = 0
  var cpu = 0
  var remoteAi = false

  var displays = 0

  val osStats = ManagementFactory.getOperatingSystemMXBean
  val threadStats = ManagementFactory.getThreadMXBean
  val memoryStats = ManagementFactory.getMemoryMXBean
  val cpuStats = new CPU()
  implicit val executor = Akka.system.dispatcher

  implicit val timeout = Timeout(100 millis)

  def receive = {

    case GetNbMembers   ⇒ sender ! allMembers

    case GetNbGames     ⇒ sender ! nbGames

    case GetNbPlaying   ⇒ sender ! nbPlaying

    case GetStatus      ⇒ sender ! status

    case GetMonitorData ⇒ sender ! monitorData

    case Update(env) ⇒ {
      val before = nowMillis
      Future.sequence(List(
        (env.site.hub ? GetNbMembers).mapTo[Int],
        (env.lobby.hub ? GetNbMembers).mapTo[Int],
        (env.round.hubMaster ? GetNbHubs).mapTo[Int],
        (env.round.hubMaster ? GetNbMembers).mapTo[Int],
        Future(env.game.gameRepo.countAll.unsafePerformIO),
        Future(env.game.gameRepo.countPlaying.unsafePerformIO)
      )) onSuccess {
        case List(
          siteMembers,
          lobbyMembers,
          gameHubs,
          gameMembers,
          all,
          playing) ⇒ {
          latency = (nowMillis - before).toInt
          site = SiteSocket(siteMembers)
          lobby = LobbySocket(lobbyMembers)
          game = GameSocket(gameHubs, gameMembers)
          nbGames = all
          nbPlaying = playing
          loadAvg = osStats.getSystemLoadAverage.toFloat
          nbThreads = threadStats.getThreadCount
          memory = memoryStats.getHeapMemoryUsage.getUsed / 1024 / 1024
          rps = rpsProvider.rps
          cpu = ((cpuStats.getCpuUsage() * 1000).round / 10.0).toInt
          remoteAi = env.ai.remoteAi.currentHealth
        }
      } onComplete {
        case Left(a) ⇒ println("Reporting: " + a.getMessage)
        case a       ⇒
      }
    }
  }

  private def display() {

    val data = Formatter.dataLine(List(
      "site" -> site.nbMembers,
      "lobby" -> lobby.nbMembers,
      "game" -> game.nbMembers,
      "hubs" -> game.nbHubs,
      "recent" -> nbPlaying,
      "lat." -> latency,
      "thread" -> nbThreads,
      "load" -> loadAvg.toString.replace("0.", "."),
      "mem" -> memory,
      "AI" -> remoteAi.fold("1", "0")
    ))

    if (displays % 8 == 0) println(data.header)
    displays = displays + 1

    println(data.line)
  }

  private def status = List(
    allMembers,
    nbGames,
    nbPlaying,
    game.nbHubs,
    loadAvg.toString,
    remoteAi.fold(1, 0)
  ) mkString " "

  private def monitorData = List(
    "users" -> allMembers,
    "site" -> site.nbMembers,
    "lobby" -> lobby.nbMembers,
    "games" -> game.nbMembers,
    "hubs" -> game.nbHubs,
    "recent" -> nbPlaying,
    "lat" -> latency,
    "thread" -> nbThreads,
    "cpu" -> cpu,
    "load" -> loadAvg,
    "memory" -> memory,
    "rps" -> rps
  ) map {
      case (name, value) ⇒ value + ":" + name
    }

  private def allMembers = site.nbMembers + lobby.nbMembers + game.nbMembers

  object Formatter {

    def dataLine(data: List[(String, Any)]) = new {

      def header = data map (_._1) mkString " "

      def line = data map {
        case (name, value) ⇒ {
          val s = value.toString
          List.fill(name.size - s.size)(" ").mkString + s + " "
        }
      } mkString
    }
  }
}
