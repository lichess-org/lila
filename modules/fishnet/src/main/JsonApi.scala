package lila.fishnet

import chess.format.{ Fen, Uci }
import chess.variant.Variant
import chess.eval.Score
import chess.eval.Eval.Cp
import play.api.libs.json.*

import lila.common.Json.{ *, given }
import lila.core.chess.Depth
import lila.fishnet.Work as W

object JsonApi:

  sealed trait Request

  object Request:

    def isValid(js: JsValue): Boolean = js.arr("analysis").forall(_.value.sizeIs <= Analyser.maxPlies + 1)

    case class Stockfish(
        flavor: Option[String]
    ):
      def isNnue = flavor.has("nnue")

    case class Acquire() extends Request

    case class PostAnalysis(
        stockfish: Stockfish,
        analysis: List[Option[Evaluation.EvalOrSkip]]
    ) extends Request:

      def completeOrPartial =
        if analysis.headOption.so(_.isDefined) then CompleteAnalysis(stockfish, analysis.flatten)
        else PartialAnalysis(stockfish, analysis)

    case class CompleteAnalysis(
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
        stockfish: Stockfish,
        analysis: List[Option[Evaluation.EvalOrSkip]]
    )

    case class Evaluation(
        pv: List[Uci],
        score: Option[Score],
        time: Option[Int],
        nodes: Option[Int],
        nps: Option[Int],
        depth: Option[Depth]
    ):
      val cappedNps = nps.map(_.min(Evaluation.npsCeil))

      val cappedPv = pv.take(lila.analyse.Info.LineMaxPlies)

      def mateFound = score.forall(_.isMateFound) // filtered ambiguous #+/-0 is also mate
      def deadDraw = score.exists(_.cp.has(Cp(0)))

    object Evaluation:

      enum EvalOrSkip:
        case Skipped
        case Evaluated(eval: Evaluation)

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
    given Reads[Request.Acquire] = Json.reads
    val scoreReads: Reads[Option[Score]] = Reads: js =>
      (js.int("cp"), js.int("mate"), js.boolean("mateGiven")) match
        case (Some(cp), _, _) => JsSuccess(Score.cp(cp).some)
        case (_, Some(0), Some(true)) => JsSuccess(Score.MateGiven.some)
        case (_, Some(0), Some(false)) => JsSuccess(Score.mated.some)
        // older clients send mate 0 also when the side to move has won
        case (_, Some(0), None) => JsSuccess(none)
        case (_, Some(mate), _) => JsSuccess(Score.mate(mate).some)
        case _ => JsError("expected cp or mate")
    given Reads[List[Uci]] = Reads.of[String].map(Uci.readList(_).getOrElse(Nil))

    given EvaluationReads: Reads[Request.Evaluation] = (
      (__ \ "pv")
        .readNullable[List[Uci]]
        .map(~_)
        .and((__ \ "score").read(using scoreReads))
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
