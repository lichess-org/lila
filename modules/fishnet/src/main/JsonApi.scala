package lila.fishnet

import org.joda.time.DateTime
import play.api.libs.json._

import chess.format.{ Uci, Forsyth, FEN }
import chess.variant.Variant

import lila.fishnet.{ Work => W }

object JsonApi {

  sealed trait Request {
    val fishnet: Request.Fishnet
    val engine: Request.Engine

    def instance(ip: Client.IpAddress) = Client.Instance(
      fishnet.version,
      fishnet.python | Client.Python(""),
      Client.Engine(engine.name),
      ip,
      DateTime.now)
  }

  object Request {

    sealed trait Result

    case class Fishnet(
      version: Client.Version,
      python: Option[Client.Python],
      apikey: Client.Key)

    sealed trait Engine {
      def name: String
    }

    case class BaseEngine(name: String) extends Engine

    case class FullEngine(
      name: String,
      options: EngineOptions) extends Engine

    case class EngineOptions(
        threads: Option[String],
        hash: Option[String]) {
      def threadsInt = threads flatMap parseIntOption
      def hashInt = hash flatMap parseIntOption
    }

    case class Acquire(
      fishnet: Fishnet,
      engine: BaseEngine) extends Request

    case class PostMove(
      fishnet: Fishnet,
      engine: BaseEngine,
      move: MoveResult) extends Request with Result

    case class MoveResult(bestmove: String) {
      def uci: Option[Uci] = Uci(bestmove)
    }

    case class PostAnalysis(
        fishnet: Fishnet,
        engine: FullEngine,
        analysis: List[Option[Evaluation]]) extends Request with Result {

      def completeOrPartial =
        if (analysis.headOption.??(_.isDefined)) CompleteAnalysis(fishnet, engine, analysis.flatten)
        else PartialAnalysis(fishnet, engine, analysis)
    }

    case class CompleteAnalysis(
        fishnet: Fishnet,
        engine: FullEngine,
        analysis: List[Evaluation]) {

      def medianNodes = analysis
        .filterNot(_.mateFound)
        .filterNot(_.deadDraw)
        .flatMap(_.nodes).toNel map lila.common.Maths.median[Int]

      def strong = medianNodes.fold(true)(_ > Evaluation.acceptableNodes)
      def weak = !strong
    }

    case class PartialAnalysis(
      fishnet: Fishnet,
      engine: FullEngine,
      analysis: List[Option[Evaluation]])

    case class Evaluation(
        pv: Option[String],
        score: Evaluation.Score,
        time: Option[Int],
        nodes: Option[Int],
        nps: Option[Int],
        depth: Option[Int]) {

      // use first pv move as bestmove
      val pvList = pv.??(_.split(' ').toList)

      val cappedNps = nps.map(_ min Evaluation.npsCeil)

      val cappedPvList = pvList take lila.analyse.Info.LineMaxPlies

      def isCheckmate = score.mate contains 0
      def mateFound = score.mate.isDefined
      def deadDraw = score.cp contains 0
    }

    object Evaluation {

      case class Score(cp: Option[Int], mate: Option[Int])

      val npsCeil = 10 * 1000 * 1000

      val desiredNodes = 3 * 1000 * 1000
      val acceptableNodes = desiredNodes * 0.9
    }
  }

  case class Game(
    game_id: String,
    position: FEN,
    variant: Variant,
    moves: String)

  def fromGame(g: W.Game) = Game(
    game_id = g.id,
    position = g.initialFen | FEN(g.variant.initialFen),
    variant = g.variant,
    moves = g.moves)

  sealed trait Work {
    val id: String
    val game: Game
  }
  case class Move(
    id: String,
    level: Int,
    game: Game) extends Work

  case class Analysis(
    id: String,
    game: Game) extends Work

  def fromWork(w: W): Work = w match {
    case m: W.Move     => Move(w.id.value, m.level, fromGame(m.game))
    case a: W.Analysis => Analysis(w.id.value, fromGame(a.game))
  }

  object readers {
    implicit val ClientVersionReads = Reads.of[String].map(new Client.Version(_))
    implicit val ClientPythonReads = Reads.of[String].map(new Client.Python(_))
    implicit val ClientKeyReads = Reads.of[String].map(new Client.Key(_))
    implicit val EngineOptionsReads = Json.reads[Request.EngineOptions]
    implicit val BaseEngineReads = Json.reads[Request.BaseEngine]
    implicit val FullEngineReads = Json.reads[Request.FullEngine]
    implicit val FishnetReads = Json.reads[Request.Fishnet]
    implicit val AcquireReads = Json.reads[Request.Acquire]
    implicit val MoveResultReads = Json.reads[Request.MoveResult]
    implicit val PostMoveReads = Json.reads[Request.PostMove]
    implicit val ScoreReads = Json.reads[Request.Evaluation.Score]
    implicit val EvaluationReads = Json.reads[Request.Evaluation]
    implicit val EvaluationOptionReads = Reads[Option[Request.Evaluation]] {
      case JsNull => JsSuccess(None)
      case obj    => EvaluationReads reads obj map some
    }
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
          case a: Analysis => Json.obj("type" -> "analysis", "id" -> work.id)
          case m: Move     => Json.obj("type" -> "move", "id" -> work.id, "level" -> m.level)
        })
      ) ++ Json.toJson(work.game).as[JsObject]
    }
  }
}
