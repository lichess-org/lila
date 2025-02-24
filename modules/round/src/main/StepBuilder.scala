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

  def apply(
      id: GameId,
      sans: Vector[SanStr],
      variant: Variant,
      initialFen: Fen.Full
  ): JsArray =
    val (init, games, error) = chess.Replay.gameMoveWhileValid(sans, initialFen, variant)
    error.foreach(logChessError(id.value))
    JsArray:
      val initStep = Step(
        ply = init.ply,
        move = none,
        fen = Fen.write(init),
        check = init.situation.check,
        dests = None,
        drops = None,
        crazyData = init.situation.board.crazyData
      )
      val moveSteps = games.map: (g, m) =>
        Step(
          ply = g.ply,
          move = m.some,
          fen = Fen.write(g),
          check = g.situation.check,
          dests = None,
          drops = None,
          crazyData = g.situation.board.crazyData
        )
      (initStep :: moveSteps).map(_.toJson)

  private val logChessError = (id: String) =>
    (err: chess.ErrorStr) =>
      val path = if id == "synthetic" then "analysis" else id
      logger.info(s"https://lichess.org/$path ${err.value.linesIterator.toList.headOption | "?"}")
