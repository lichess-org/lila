package lila.fishnet

import org.joda.time.DateTime
import play.api.libs.json._

import chess.format.{ Uci, Forsyth, FEN }
import chess.variant.Variant

import lila.fishnet.{ Work => W }

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

  case class Game(
    game_id: String,
    position: FEN,
    variant: Variant,
    moves: Seq[String])

  def fromGame(g: W.Game) = Game(
    game_id = g.id,
    position = g.position | FEN(Forsyth.initial),
    variant = g.variant,
    moves = g.moves)

  sealed trait Work { val game: Game }
  case class Move(level: Int, game: Game) extends Work
  case class Analysis(game: Game) extends Work

  def fromWork(w: W): Work = w match {
    case m: W.Move     => Move(m.level, fromGame(m.game))
    case a: W.Analysis => Analysis(fromGame(a.game))
  }

  implicit val EngineReads = Json.reads[Client.Engine]
  implicit val ClientVersionReads = Reads.of[String].map(new Client.Version(_))
  implicit val ClientKeyReads = Reads.of[String].map(new Client.Key(_))
  implicit val AcquireReads = Json.reads[Acquire]

  implicit val VariantWrites = Writes[Variant] { v => JsString(v.key) }
  implicit val FENWrites = Writes[FEN] { fen => JsString(fen.value) }
  implicit val GameWrites = Json.writes[Game]

  implicit val WorkWrites = OWrites[Work] { work =>
    Json.obj(
      "work" -> (work match {
        case a: Analysis => Json.obj("type" -> "analysis")
        case m: Move     => Json.obj("type" -> "move", "level" -> m.level)
      })
    ) ++ Json.toJson(work.game).as[JsObject]
  }
}
