package lila
package monitor

import com.mongodb.casbah.MongoDB
import com.mongodb.casbah.Imports._

case class MongoStatus(
    memory: Int = 0,
    connection: Int = 0,
    query: Int = 0,
    totalTime: Double = 0,
    lockTime: Double = 0,
    qps: Int = 0,
    lock: Double = 0d) {
}

case object MongoStatus {

  def default = new MongoStatus()

  def apply(mongodb: MongoDB)(prev: MongoStatus): MongoStatus = {
    val status: MongoDBObject = mongodb.command("serverStatus")
    val query = status.expand[Int]("network.numRequests") | 0
    val totalTime = status.expand[Double]("globalLock.totalTime") | 0
    val lockTime = status.expand[Double]("globalLock.lockTime") | 0
    new MongoStatus(
      memory = status.expand[Int]("mem.resident") | 0,
      connection = status.expand[Int]("connections.current") | 0,
      query = query,
      totalTime = totalTime,
      lockTime = lockTime,
      qps = query - prev.query,
      lock = (totalTime - prev.totalTime == 0).fold(
        0d,
        (lockTime - prev.lockTime)/(totalTime - prev.totalTime) * 100
      )
    )
  }
}
