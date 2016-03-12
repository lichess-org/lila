package lila.fishnet

import org.joda.time.DateTime
import play.api.libs.json._

import chess.format.{ Uci, Forsyth, FEN }
import chess.variant.Variant

import lila.fishnet.{ Work => W }

object JsonApi {

  sealed trait Request {
    val fishnet: Request.Fishnet
    val engine: Client.Engine

    def instance = Client.Instance(
      fishnet.uuid,
      fishnet.version,
      engine,
      DateTime.now)
  }

  object Request {

    case class Fishnet(
      version: Client.Version,
      apikey: Client.Key,
      uuid: Client.UUID)

    case class Acquire(
      fishnet: Fishnet,
      engine: Client.Engine) extends Request

    case class PostMove(
      fishnet: Fishnet,
      engine: Client.Engine,
      move: MoveResult) extends Request

    case class MoveResult(bestmove: String, depth: Int) {
      def uci: Option[Uci] = Uci(bestmove)
    }

    case class PostAnalysis(
      fishnet: Fishnet,
      engine: Client.Engine,
      analysis: List[AnalysisPly]) extends Request

    case class AnalysisPly(
      bestmove: Option[String],
      pv: Option[String],
      score: Score)

    case class Score(
      cp: Option[Int],
      mate: Option[Int])
  }

  case class Game(
    game_id: String,
    position: FEN,
    variant: Variant,
    moves: String)

  def fromGame(g: W.Game) = Game(
    game_id = g.id,
    position = g.initialFen | FEN(Forsyth.initial),
    variant = g.variant,
    moves = g.moves)

  sealed trait Work { val game: Game }
  case class Move(level: Int, game: Game) extends Work
  case class Analysis(game: Game) extends Work

  def fromWork(w: W): Work = w match {
    case m: W.Move     => Move(m.level, fromGame(m.game))
    case a: W.Analysis => Analysis(fromGame(a.game))
  }

  object readers {
    implicit val EngineReads = Json.reads[Client.Engine]
    implicit val ClientVersionReads = Reads.of[String].map(new Client.Version(_))
    implicit val ClientKeyReads = Reads.of[String].map(new Client.Key(_))
    implicit val ClientUUIDReads = Reads.of[String].map(new Client.UUID(_))
    implicit val FishnetReads = Json.reads[Request.Fishnet]
    implicit val AcquireReads = Json.reads[Request.Acquire]
    implicit val MoveResultReads = Json.reads[Request.MoveResult]
    implicit val PostMoveReads = Json.reads[Request.PostMove]
    implicit val ScoreReads = Json.reads[Request.Score]
    implicit val AnalysisPlyReads = Json.reads[Request.AnalysisPly]
    implicit val PostAnalysisReads = Json.reads[Request.PostAnalysis]
  }

  object writers {
    implicit val VariantWrites = Writes[Variant] { v => JsString(v.key) }
    implicit val FENWrites = Writes[FEN] { fen => JsString(fen.value) }
    implicit val GameWrites = Json.writes[Game]
    implicit val WorkIdWrites = Writes[Work.Id] { id => JsString(id.value) }
    implicit val WorkWrites = OWrites[Work] { work =>
      Json.obj(
        "work" -> (work match {
          case a: Analysis => Json.obj("type" -> "analysis")
          case m: Move     => Json.obj("type" -> "move", "level" -> m.level)
        })
      ) ++ Json.toJson(work.game).as[JsObject]
    }
  }
}
