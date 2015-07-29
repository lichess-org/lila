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
  query: Long = 0,
  qps: Int = 0)

private[monitor] object MongoStatus {

  val default = new MongoStatus()

  def apply(db: DB)(prev: MongoStatus): Fu[MongoStatus] =
    db.command(Status) map { bsonMap =>
      val status = JsObjectReader read BSONDocument(bsonMap)
      val query = ~(status obj "network" flatMap (_ long "numRequests"))
      new MongoStatus(
        memory = ~(status \ "mem" \ "resident").asOpt[Int],
        connection = ~(status \ "connections" \ "current").asOpt[Int],
        query = query,
        qps = (query - prev.query).toInt)
    }
}
