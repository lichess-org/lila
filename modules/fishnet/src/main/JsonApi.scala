package lila.fishnet

import chess.format.{ Fen, Uci }
import chess.variant.Variant
import chess.eval.Eval.{ Cp, Mate }
import play.api.libs.json.*

import lila.common.Json.{ *, given }
import lila.core.chess.Depth
import lila.core.net.IpAddress
import lila.fishnet.Work as W

object JsonApi:

  sealed trait Request:
    val fishnet: Request.Fishnet

    def instance(ip: IpAddress) = Client.Instance(fishnet.version, ip, nowInstant)

  object Request:

    case class Fishnet(
        version: Client.Version,
        apikey: Client.Key
    )

    case class Stockfish(
        flavor: Option[String]
    ):
      def isNnue = flavor.has("nnue")

    case class Acquire(
        fishnet: Fishnet
    ) extends Request

    case class PostAnalysis(
        fishnet: Fishnet,
        stockfish: Stockfish,
        analysis: List[Option[Evaluation.EvalOrSkip]]
    ) extends Request:

      def completeOrPartial =
        if analysis.headOption.so(_.isDefined) then CompleteAnalysis(fishnet, stockfish, analysis.flatten)
        else PartialAnalysis(fishnet, stockfish, analysis)

    case class CompleteAnalysis(
        fishnet: Fishnet,
        stockfish: Stockfish,
        analysis: List[Evaluation.EvalOrSkip]
    ):

      import Evaluation.*
      def evaluations = analysis.collect { case EvalOrSkip.Evaluated(e) => e }

      def medianNodes = scalalib.Maths.median:
        evaluations
          .withFilter(e => !(e.mateFound || e.deadDraw))
          .flatMap(_.nodes)

    case class PartialAnalysis(
        fishnet: Fishnet,
        stockfish: Stockfish,
        analysis: List[Option[Evaluation.EvalOrSkip]]
    )

    case class Evaluation(
        pv: List[Uci],
        score: Evaluation.Score,
        time: Option[Int],
        nodes: Option[Int],
        nps: Option[Int],
        depth: Option[Depth]
    ):
      val cappedNps = nps.map(_.min(Evaluation.npsCeil))

      val cappedPv = pv.take(lila.analyse.Info.LineMaxPlies)

      def isCheckmate = score.mate.has(Mate(0))
      def mateFound = score.mate.isDefined
      def deadDraw = score.cp.has(Cp(0))

    object Evaluation:

      enum EvalOrSkip:
        case Skipped
        case Evaluated(eval: Evaluation)

      case class Score(cp: Option[Cp], mate: Option[Mate]):
        def invert = copy(cp.map(_.invert), mate.map(_.invert))
        def invertIf(cond: Boolean) = if cond then invert else this

      val npsCeil = 10_000_000

  case class Game(
      game_id: String,
      position: Fen.Full,
      variant: Variant,
      moves: String
  )

  def fromGame(g: W.Game) =
    Game(
      game_id = if g.studyId.isDefined then "" else g.id,
      position = g.initialFen | g.variant.initialFen,
      variant = g.variant,
      moves = g.moves
    )

  sealed trait Work:
    val id: String
    val game: Game

  case class Analysis(
      id: String,
      game: Game,
      nodes: Int,
      skipPositions: List[Int]
  ) extends Work

  def analysisFromWork(m: Work.Analysis): Analysis =
    Analysis(
      id = m.id.value,
      game = fromGame(m.game),
      nodes = m.nodesPerMove,
      skipPositions = m.skipPositions
    )

  object readers:
    import play.api.libs.functional.syntax.*
    import Request.Evaluation.EvalOrSkip
    given Reads[Request.Stockfish] = Json.reads
    given Reads[Request.Fishnet] = Json.reads
    given Reads[Request.Acquire] = Json.reads
    given Reads[Request.Evaluation.Score] = Json.reads
    given Reads[List[Uci]] = Reads.of[String].map(Uci.readList(_).getOrElse(Nil))

    given EvaluationReads: Reads[Request.Evaluation] = (
      (__ \ "pv")
        .readNullable[List[Uci]]
        .map(~_)
        .and((__ \ "score").read[Request.Evaluation.Score])
        .and((__ \ "time").readNullable[Int])
        .and((__ \ "nodes").readNullable[Long].map(_.map(_.toSaturatedInt)))
        .and((__ \ "nps").readNullable[Long].map(_.map(_.toSaturatedInt)))
        .and((__ \ "depth").readNullable[Depth])
    )(Request.Evaluation.apply)
    given Reads[Option[EvalOrSkip]] = Reads:
      case JsNull => JsSuccess(None)
      case obj =>
        if ~(obj.boolean("skipped")) then JsSuccess(EvalOrSkip.Skipped.some)
        else EvaluationReads.reads(obj).map(EvalOrSkip.Evaluated(_).some)
    given Reads[Request.PostAnalysis] = Json.reads

  object writers:
    given Writes[Variant] = writeAs(_.key)
    given Writes[Game] = Json.writes
    given OWrites[Work] = OWrites { work =>
      (work match
        case a: Analysis =>
          Json.obj(
            "work" -> Json.obj(
              "type" -> "analysis",
              "id" -> a.id,
              "nodes" -> Json.obj(
                "sf18" -> a.nodes,
                "sf17_1" -> a.nodes,
                "sf16" -> a.nodes,
                "classical" -> a.nodes * 3
              ),
              "timeout" -> Cleaner.timeoutPerPly.toMillis
            ),
            "skipPositions" -> a.skipPositions
          )
      ) ++ Json.toJson(work.game).as[JsObject]
    }
