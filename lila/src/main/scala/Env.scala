package lila

import repo._
import com.mongodb.casbah.MongoConnection
import com.mongodb.casbah.commons.conversions.scala._

class Env(configuration: Map[String, Any]) {

  def gameRepo = new GameRepo(mongodb("game2"))

  private def mongoConnection = MongoConnection(
    get[String]("mongo.host"),
    get[Int]("mongo.port")
  )

  private def mongodb = mongoConnection(
    get[String]("mongo.dbname")
  )

  private def get[A](key: String) = configuration(key).asInstanceOf[A]
}
