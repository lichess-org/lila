package lila.round

import chess.format.pgn.SanStr
import chess.format.{ Fen, Uci }
import chess.variant.Variant
import chess.{ Check, Ply, Square }
import play.api.libs.json.*

case class Step(
    ply: Ply,
    move: Option[Uci.WithSan],
    fen: Fen.Full,
    check: Check,
    // None when not computed yet
    dests: Option[Map[Square, List[Square]]],
    drops: Option[List[Square]],
    crazyData: Option[chess.variant.Crazyhouse.Data]
):
  // who's color plays next
  def color = ply.turn

  def toJson = Json.toJson(this)

object Step:

  given Writes[Step] = Writes: step =>
    import play.api.libs.json.*
    import lila.common.Json.given
    import step.*
    Json
      .obj(
        "ply" -> ply,
        "uci" -> move.map(_.uci.uci),
        "san" -> move.map(_.san),
        "fen" -> fen
      )
      .add("check", check)
      .add(
        "dests",
        dests.map {
          _.map { (orig, dests) =>
            s"${orig.asChar}${dests.map(_.asChar).mkString}"
          }.mkString(" ")
        }
      )
      .add(
        "drops",
        drops.map: drops =>
          JsString(drops.map(_.key).mkString)
      )
      .add("crazy", crazyData)

object StepBuilder:

  private val logger = lila.round.logger.branch("StepBuilder")

  def apply(id: GameId, sans: Vector[SanStr], variant: Variant, initialFen: Fen.Full): JsArray =
    val setup = chess.Position.AndFullMoveNumber(variant, initialFen)

    val moveSteps = new Array[JsValue](sans.size + 1)

    moveSteps(0) = Step(
      ply = setup.ply,
      move = none,
      fen = Fen.write(setup),
      check = setup.position.check,
      dests = None,
      drops = None,
      crazyData = setup.position.crazyData
    ).toJson

    val error = setup.position.foreach(sans, setup.ply): step =>
      moveSteps(step.ply.value - setup.ply.value) = Step(
        ply = step.ply,
        move = Uci.WithSan(step.move.toUci, step.move.toSanStr).some,
        fen = Fen.write(step.next, step.ply.fullMoveNumber),
        check = step.next.check,
        dests = None,
        drops = None,
        crazyData = step.next.crazyData
      ).toJson

    error.foreach(logChessError(id.value))

    JsArray(moveSteps)

  private val logChessError = (id: String) =>
    (err: chess.ErrorStr) =>
      val path = if id == "synthetic" then "analysis" else id
      logger.info(s"https://lichess.org/$path ${err.value}")
