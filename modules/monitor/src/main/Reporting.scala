package lila.monitor

import actorApi._
import lila.socket.actorApi.GetNbSockets
import lila.hub.actorApi.GetNbMembers
import lila.hub.actorApi.monitor._

import akka.actor._
import akka.pattern.{ ask, pipe }
import scala.concurrent.duration._
import play.api.libs.concurrent._
import play.api.Play.current
import scala.util.{ Success, Failure }
import java.lang.management.ManagementFactory

private[monitor] final class Reporting(
    rpsProvider: RpsProvider,
    mpsProvider: RpsProvider,
    socket: ActorRef,
    db: lila.db.Env,
    hub: lila.hub.Env) extends Actor {

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
  var mps = 0
  var cpu = 0
  var mongoStatus = MongoStatus.default
  var clientAi = none[Int]

  var displays = 0

  val osStats = ManagementFactory.getOperatingSystemMXBean
  val threadStats = ManagementFactory.getThreadMXBean
  val memoryStats = ManagementFactory.getMemoryMXBean
  val cpuStats = new CPU
  implicit val timeout = makeTimeout(100 millis)

  def receive = {

    case AddMove        ⇒ mpsProvider.add
    case AddRequest     ⇒ rpsProvider.add

    case GetNbMembers   ⇒ sender ! allMembers

    case GetNbGames     ⇒ sender ! nbGames

    case GetNbMoves     ⇒ sender ! mps

    case GetStatus      ⇒ sender ! status

    case GetMonitorData ⇒ sender ! monitorData

    case Update ⇒ {
      val before = nowMillis
      MongoStatus(db.db)(mongoStatus) zip
        (hub.actor.ai ? lila.hub.actorApi.ai.Ping).mapTo[Option[Int]] zip
        List((hub.socket.site ? GetNbMembers),
          (hub.socket.lobby ? GetNbMembers),
          (hub.socket.round ? GetNbSockets),
          (hub.socket.round ? GetNbMembers),
          (hub.actor.game ? lila.hub.actorApi.game.Count)
        ).map(_.mapTo[Int]).sequence onComplete {
            case Failure(e) ⇒ logwarn("[reporting] " + e.getMessage)
            case Success(((mongoS, aiPing), List(siteMembers, lobbyMembers, gameHubs, gameMembers, games))) ⇒ {
              latency = (nowMillis - before).toInt
              site = SiteSocket(siteMembers)
              lobby = LobbySocket(lobbyMembers)
              game = GameSocket(gameHubs, gameMembers)
              mongoStatus = mongoS
              nbGames = games
              loadAvg = osStats.getSystemLoadAverage.toFloat
              nbThreads = threadStats.getThreadCount
              memory = memoryStats.getHeapMemoryUsage.getUsed / 1024 / 1024
              rps = rpsProvider.rps
              mps = mpsProvider.rps
              cpu = ((cpuStats.getCpuUsage() * 1000).round / 10.0).toInt
              clientAi = aiPing
              socket ! MonitorData(monitorData)
            }
          }
    }
  }

  private def display {

    val data = dataLine(List(
      "site" -> site.nbMembers,
      "lobby" -> lobby.nbMembers,
      "game" -> game.nbMembers,
      "hubs" -> game.nbHubs,
      "lat." -> latency,
      "thread" -> nbThreads,
      "load" -> loadAvg.toString.replace("0.", "."),
      "mem" -> memory,
      "cpu" -> cpu,
      "AI" -> clientAi.isDefined.fold("1", "0")
    ))

    if (displays % 8 == 0) println(data.header)
    displays = displays + 1

    println(data.line)
  }

  private def status = List(
    allMembers,
    nbGames,
    game.nbHubs,
    loadAvg.toString,
    (clientAi | 9999)
  ) mkString " "

  private def monitorData = List(
    "users" -> allMembers,
    "lobby" -> lobby.nbMembers,
    "game" -> game.nbMembers,
    "lat" -> latency,
    "thread" -> nbThreads,
    "cpu" -> cpu,
    "load" -> loadAvg,
    "memory" -> memory,
    "rps" -> rps,
    "mps" -> mps,
    "dbMemory" -> mongoStatus.memory,
    "dbConn" -> mongoStatus.connection,
    "dbQps" -> mongoStatus.qps,
    "dbLock" -> math.round(mongoStatus.lock * 10) / 10d,
    "ai" -> (clientAi | 9999)
  ) map {
      case (name, value) ⇒ value + ":" + name
    }

  private def allMembers = site.nbMembers + lobby.nbMembers + game.nbMembers

  private def dataLine(data: List[(String, Any)]) = new {

    def header = data map (_._1) mkString " "

    def line = data map {
      case (name, value) ⇒ {
        val s = value.toString
        List.fill(name.size - s.size)(" ").mkString + s + " "
      }
    } mkString
  }
}
