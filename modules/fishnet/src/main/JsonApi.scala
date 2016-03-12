package lila.fishnet

import org.joda.time.DateTime
import play.api.libs.json._

import chess.format.{ Uci, Forsyth }
import chess.variant.Variant

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
    variant: Variant,
    moves: List[Uci]) extends Work

  case class Analysis(
    game_id: String,
    position: String,
    variant: Variant,
    moves: List[Uci]) extends Work

  def fromWork(w: lila.fishnet.Work) = w match {
    case m: lila.fishnet.Work.Move => Move(
      game_id = m.gameId,
      position = m.position | Forsyth.initial,
      variant = m.variant,
      moves = m.moves)
    case a: lila.fishnet.Work.Analysis => Analysis(
      game_id = a.gameId,
      position = a.position | Forsyth.initial,
      variant = a.variant,
      moves = a.moves)
  }

  implicit val EngineReads = Json.reads[Client.Engine]
  implicit val ClientVersionReads = Reads.of[String].map(new Client.Version(_))
  implicit val ClientKeyReads = Reads.of[String].map(new Client.Key(_))
  implicit val AcquireReads = Json.reads[Acquire]

  implicit val VariantWrites = Writes[Variant] { v => JsString(v.key) }
  implicit val UciWrites = Writes[Uci] { uci => JsString(uci.uci) }
  implicit val MoveWrites = Json.writes[Move]
  implicit val AnalysisWrites = Json.writes[Analysis]
}
