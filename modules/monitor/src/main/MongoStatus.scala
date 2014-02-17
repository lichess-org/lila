package lila.monitor

import play.api.libs.json.JsObject
import play.modules.reactivemongo.json.ImplicitBSONHandlers._
import reactivemongo.api.DB
import reactivemongo.bson._
import reactivemongo.core.commands.Status

import lila.common.PimpedJson._

private[monitor] case class MongoStatus(
  memory: Int = 0,
  connection: Int = 0,
  query: Int = 0,
  totalTime: Long = 0,
  lockTime: Long = 0,
  qps: Int = 0,
  lock: Double = 0d)

private[monitor] object MongoStatus {

  val default = new MongoStatus()

  def apply(db: DB)(prev: MongoStatus): Fu[MongoStatus] =
    db.command(Status) map { bsonMap =>
      val bson = BSONDocument(bsonMap)
      val status = JsObjectReader.read(bson)

      val query = ~(status obj "network" flatMap (_ int "numRequests"))
      val locks = ~(status obj "locks")
      val lockNumbers = for {
        dbName ← List("lichess", ".")
        statName ← List("timeLockedMicros", "timeAcquiringMicros")
        opName ← List("r", "w", "R", "W")
      } yield (locks \ dbName \ statName \ opName).asOpt[Long]
      val lockTime = lockNumbers.flatten.map(_ / 1000).sum
      val totalTime = ~(status long "uptimeMillis")
      new MongoStatus(
        memory = ~(status \ "mem" \ "resident").asOpt[Int],
        connection = ~(status \ "connections" \ "current").asOpt[Int],
        query = query,
        totalTime = totalTime,
        lockTime = lockTime,
        qps = query - prev.query,
        lock = (totalTime - prev.totalTime == 0).fold(
          0d,
          (lockTime - prev.lockTime).toDouble / (totalTime - prev.totalTime) * 100
        )
      )
    }
}
