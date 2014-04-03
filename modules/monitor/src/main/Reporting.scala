package lila.monitor

import java.lang.management.ManagementFactory
import scala.concurrent.duration._
import scala.util.{ Success, Failure }

import akka.actor._
import akka.pattern.{ ask, pipe }
import play.api.libs.concurrent._

import actorApi._
import lila.hub.actorApi.map.Size
import lila.hub.actorApi.monitor._
import lila.hub.actorApi.round.MoveEvent
import lila.socket.actorApi.{ NbMembers, PopulationGet }

private[monitor] final class Reporting(
    rpsProvider: RpsProvider,
    mpsProvider: RpsProvider,
    socket: ActorRef,
    db: lila.db.Env,
    hub: lila.hub.Env) extends Actor {

  private val bus = context.system.lilaBus

  bus.subscribe(self, 'moveEvent, 'nbMembers)

  override def postStop() {
    bus.unsubscribe(self)
  }

  var nbMembers = 0
  var nbGames = 0
  var nbPlaying = 0
  var loadAvg = 0f
  var nbThreads = 0
  var memory = 0l
  var latency = 0
  var rps = 0
  var mps = 0
  var cpu = 0
  var mongoStatus = MongoStatus.default

  var idle = true

  val osStats = ManagementFactory.getOperatingSystemMXBean
  val threadStats = ManagementFactory.getThreadMXBean
  val memoryStats = ManagementFactory.getMemoryMXBean
  val cpuStats = new CPU
  implicit val timeout = makeTimeout(100 millis)

  def receive = {

    case _: MoveEvent  => mpsProvider.add

    case AddRequest    => rpsProvider.add

    case PopulationGet => sender ! nbMembers

    case NbMembers(nb) => nbMembers = nb

    case GetNbMoves    => sender ! mpsProvider.rps

    case Update => socket ? PopulationGet foreach {
      case 0 => idle = true
      case _ => {
        val before = nowMillis
        MongoStatus(db.db)(mongoStatus) zip
          (hub.socket.round ? Size).mapTo[Int] zip
          (hub.actor.game ? lila.hub.actorApi.game.Count).mapTo[Int] onComplete {
            case Failure(e) => logwarn("[reporting] " + e.getMessage)
            case Success(((mongoS, gameHubs), games)) => {
              latency = (nowMillis - before).toInt
              mongoStatus = mongoS
              nbGames = games
              loadAvg = osStats.getSystemLoadAverage.toFloat
              nbThreads = threadStats.getThreadCount
              memory = memoryStats.getHeapMemoryUsage.getUsed / 1024 / 1024
              rps = rpsProvider.rps
              mps = mpsProvider.rps
              cpu = ((cpuStats.getCpuUsage() * 1000).round / 10.0).toInt
              socket ! MonitorData(monitorData(idle))
              idle = false
            }
          }
      }
    }
  }

  private def monitorData(idle: Boolean) = List(
    "users" -> nbMembers,
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
    "dbLock" -> math.round(mongoStatus.lock * 10) / 10d
  ) map {
      case (name, value) => value + ":" + name
    }

  private def dataLine(data: List[(String, Any)]) = new {

    def header = data map (_._1) mkString " "

    def line = data map {
      case (name, value) => {
        val s = value.toString
        List.fill(name.size - s.size)(" ").mkString + s + " "
      }
    } mkString
  }
}
