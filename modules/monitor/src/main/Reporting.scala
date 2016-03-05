package lila.monitor

import java.lang.management.ManagementFactory
import scala.concurrent.duration._
import scala.util.{ Success, Failure }

import akka.actor._
import akka.pattern.{ ask, pipe }
import play.api.libs.concurrent._

import actorApi._
import lila.hub.actorApi.monitor._
import lila.socket.actorApi.{ NbMembers, PopulationGet }

private[monitor] final class Reporting(
    reqWindowCount: lila.common.WindowCount,
    moveWindowCount: lila.common.WindowCount,
    socket: ActorRef,
    db: lila.db.Env,
    hub: lila.hub.Env) extends Actor {

  private val bus = context.system.lilaBus

  bus.subscribe(self, 'nbMembers)

  override def postStop() {
    bus.unsubscribe(self)
  }

  var nbMembers = 0
  var nbPlaying = 0
  var loadAvg = 0f
  var nbThreads = 0
  var memory = 0l
  var latency = 0
  var rps = 0
  var mps = 0
  var cpu = 0
  var mongoStatus = MongoStatus.default
  val moveMillis = scala.collection.mutable.ArrayBuffer.empty[Int]
  var moveAvgMillis = 0

  var idle = true
  var lastUpdated = nowMillis

  val osStats = ManagementFactory.getOperatingSystemMXBean
  val threadStats = ManagementFactory.getThreadMXBean
  val memoryStats = ManagementFactory.getMemoryMXBean
  val cpuStats = new CPU
  implicit val timeout = makeTimeout(100 millis)

  def receive = {

    case Move(millis) =>
      moveWindowCount.add
      millis foreach { m =>
        moveMillis += m
      }

    case AddRequest     => reqWindowCount.add

    case PopulationGet  => sender ! nbMembers

    case NbMembers(nb)  => nbMembers = nb

    case GetNbMoves     => sender ! moveWindowCount.get
    case GetMoveLatency => sender ! moveAvgMillis

    case Update =>
      if (moveMillis.size > 0) moveAvgMillis = moveMillis.sum / moveMillis.size
      moveMillis.clear
      socket ? PopulationGet foreach {
        case 0 => idle = true
        case _ => {
          val before = nowMillis
          MongoStatus(db.db)(mongoStatus) onComplete {
            case Failure(e) => logwarn("[reporting] " + e.getMessage)
            case Success(mongoS) => {
              latency = (nowMillis - before).toInt
              mongoStatus = mongoS
              loadAvg = osStats.getSystemLoadAverage.toFloat
              nbThreads = threadStats.getThreadCount
              memory = memoryStats.getHeapMemoryUsage.getUsed / 1024 / 1024
              mps = moveWindowCount.get
              rps = reqWindowCount.get
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
    "mlat" -> moveAvgMillis,
    "dbMemory" -> mongoStatus.memory,
    "dbConn" -> mongoStatus.connection,
    "dbQps" -> idle.fold("??", mongoStatus.qps.toString)) map {
      case (name, value) => s"$value:$name"
    }
}
