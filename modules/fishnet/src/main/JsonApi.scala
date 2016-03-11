package lila.fishnet

import play.api.libs.json._
import org.joda.time.DateTime

object JsonApi {

  sealed trait Request {
    val key: Client.Key
    val version: Client.Version
    val engine: Client.Engine

    def instance = Client.Instance(version, engine, DateTime.now)
  }

  case class Acquire(
      key: Client.Key,
      version: Client.Version,
      engine: Client.Engine) extends Request {
  }

  sealed trait Work

  case class Move(
    game_id: String,
    position: String,
    variant: String,
    moves: List[String]) extends Work

  implicit val EngineReads = Json.reads[Client.Engine]
  implicit val ClientVersionReads = Reads.of[String].map(new Client.Version(_))
  implicit val ClientKeyReads = Reads.of[String].map(new Client.Key(_))
  implicit val AcquireReads = Json.reads[Acquire]

  implicit val MoveWrites = Json.writes[Move]
}
