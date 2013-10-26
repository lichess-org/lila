package lila.monitor

import java.lang.management.ManagementFactory
import scala.concurrent.duration._
import scala.util.{ Success, Failure }

import akka.actor._
import akka.pattern.{ ask, pipe }
import play.api.libs.concurrent._
import play.api.Play.current

import actorApi._
import lila.hub.actorApi.GetNbMembers
import lila.hub.actorApi.map.Size
import lila.hub.actorApi.monitor._
import lila.hub.actorApi.round.MoveEvent

private[monitor] final class Reporting(
    rpsProvider: RpsProvider,
    mpsProvider: RpsProvider,
    socket: ActorRef,
    db: lila.db.Env,
    hub: lila.hub.Env) extends Actor {

  context.system.eventStream.subscribe(self, classOf[MoveEvent])

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
  var aiLoads = List[Option[Int]]()

  var idle = true

  val osStats = ManagementFactory.getOperatingSystemMXBean
  val threadStats = ManagementFactory.getThreadMXBean
  val memoryStats = ManagementFactory.getMemoryMXBean
  val cpuStats = new CPU
  implicit val timeout = makeTimeout(100 millis)

  def receive = {

    case _: MoveEvent ⇒ mpsProvider.add

    case AddRequest   ⇒ rpsProvider.add

    case GetNbMembers ⇒ List(
      (hub.socket.site ? GetNbMembers),
      (hub.socket.lobby ? GetNbMembers),
      (hub.socket.round ? GetNbMembers)
    ).map(_.mapTo[Int]).suml pipeTo sender

    case GetNbMoves ⇒ sender ! mpsProvider.rps

    case Update ⇒ socket ? GetNbMembers foreach {
      case 0 ⇒ idle = true
      case _ ⇒ {
        val before = nowMillis
        MongoStatus(db.db)(mongoStatus) zip
          (hub.actor.ai ? lila.hub.actorApi.ai.GetLoad).mapTo[List[Option[Int]]] zip
          (hub.socket.site ? GetNbMembers).mapTo[Int] zip
          (hub.socket.lobby ? GetNbMembers).mapTo[Int] zip
          (hub.socket.round ? Size).mapTo[Int] zip
          (hub.socket.round ? GetNbMembers).mapTo[Int] zip
          (hub.actor.game ? lila.hub.actorApi.game.Count).mapTo[Int] onComplete {
            case Failure(e) ⇒ logwarn("[reporting] " + e.getMessage)
            case Success(((((((mongoS, aiL), siteMembers), lobbyMembers), gameHubs), gameMembers), games)) ⇒ {
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
              aiLoads = aiL
              socket ! MonitorData(monitorData(idle))
              idle = false
            }
          }
      }
    }
  }

  private def aiLoadString = aiLoads.map(_.fold("!!")(_.toString)) mkString ","

  private def monitorData(idle: Boolean) = List(
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
    "dbQps" -> idle.fold("??", mongoStatus.qps.toString),
    "dbLock" -> math.round(mongoStatus.lock * 10) / 10d,
    "ai" -> aiLoadString
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
