package lila.http

import repo._
import com.mongodb.casbah.MongoConnection
import com.mongodb.casbah.commons.conversions.scala._

final class Env(configuration: Env.Settings, val name: String = "default") {

  lazy val gameRepo = new GameRepo(mongodb("game2"))

  private lazy val mongodb = MongoConnection(
    get[String]("mongo.host"),
    get[Int]("mongo.port")
  )(get[String]("mongo.dbname"))

  private def get[A](key: String) = configuration(key).asInstanceOf[A]

  def ~(settings: Env.Settings) = new Env(
    configuration ++ settings,
    name
  )
}

object Env {

  type Settings = Map[String, Any]

  private val defaults = Map(
    "mongo.host" -> "127.0.0.1",
    "mongo.port" -> 27017,
    "mongo.dbname" -> "lichess"
  )

  def test = new Env(defaults ++ Map(
    "mongo.dbname" -> "lichess_test"
  ), "test")
}

