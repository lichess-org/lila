package lila.monitor

import reactivemongo.core.commands.Status
import reactivemongo.api.DB
import reactivemongo.bson._
import play.modules.reactivemongo.Implicits._

import play.api.libs.json.JsObject

private[monitor] case class MongoStatus(
  memory: Int = 0,
  connection: Int = 0,
  query: Int = 0,
  totalTime: Long = 0,
  lockTime: Long = 0,
  qps: Int = 0,
  lock: Double = 0d)

private[monitor] object MongoStatus {

  def default = new MongoStatus()

  def apply(db: DB)(prev: MongoStatus): Fu[MongoStatus] =
    db.command(Status) map { status ⇒

      def get[A](field: String)(implicit reader: BSONReader[_ <: BSONValue, A]): Option[A] = 
        status.get(field) flatMap (_.seeAsOpt[A])

      val query = ~get[Int]("network.numRequests") 
      val locks = ~get[JsObject]("locks")
      val lockNumbers = for {
        dbName ← List("lichess", ".")
        statName ← List("timeLockedMicros", "timeAcquiringMicros")
        opName ← List("r", "w", "R", "W")
      } yield (locks \ dbName \ statName \ opName).asOpt[Long]
      val lockTime = lockNumbers.flatten.map(_ / 1000).sum
      val totalTime = ~get[Long]("uptimeMillis")
      new MongoStatus(
        memory = ~get[Int]("mem.resident"),
        connection = ~get[Int]("connections.current"),
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
