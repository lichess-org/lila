package lila.opening

import chess.Replay
import chess.format.pgn.{ PgnMovesStr, PgnStr, SanStr }
import chess.format.{ Fen, Uci }
import chess.opening.{ Opening, OpeningDb, OpeningKey, OpeningName }

case class OpeningQuery(replay: Replay, config: OpeningConfig):
  export replay.state.sans
  val uci: Vector[Uci] = replay.moves.view.map(_.toUci).reverse.toVector
  def position = replay.state.position
  def variant = chess.variant.Standard
  val fen = Fen.writeOpening(replay.state.position)
  val exactOpening = OpeningDb.findByStandardFen(fen)
  val family = exactOpening.map(_.family)
  def pgnString = PgnMovesStr(sans.mkString(" "))
  def pgnUnderscored = sans.mkString("_")
  def initial = sans.isEmpty
  def query = openingAndExtraMoves match
    case (op, _) => OpeningQuery.Query(op.fold("-")(_.key.value), pgnString.some)
  def prev = (sans.sizeIs > 1).so(
    OpeningQuery(
      OpeningQuery.Query("", PgnMovesStr(sans.init.mkString(" ")).some),
      config
    )
  )

  val openingAndExtraMoves: (Option[Opening], List[SanStr]) =
    exactOpening
      .map(_.some -> Nil)
      .orElse(OpeningDb.search(replay.chronoMoves).map { case Opening.AtPly(op, ply) =>
        op.some -> sans.drop(ply.value + 1).toList
      })
      .getOrElse(none, sans.toList)

  def closestOpening: Option[Opening] = openingAndExtraMoves._1

  val name: String = openingAndExtraMoves match
    case (Some(op), Nil) => op.name.value
    case (Some(op), moves) => s"${op.name}, ${moves.mkString(" ")}"
    case (_, moves) => moves.mkString(" ")

  override def toString = s"$query $config"

object OpeningQuery:

  case class Query(key: String, moves: Option[PgnMovesStr])

  def queryFromUrl(key: String, moves: Option[String]) =
    Query(
      key.replace("Defence", "Defense"),
      PgnMovesStr.from(moves.map(_.trim.replace("_", " ")).filter(_.nonEmpty))
    )

  def apply(q: Query, config: OpeningConfig): Option[OpeningQuery] =
    if q.key.isEmpty && q.moves.isEmpty then fromPgn(PgnMovesStr(""), config)
    else q.moves.flatMap(fromPgn(_, config)).orElse(byOpening(q.key, config))

  private lazy val openingsByLowerCaseKey: Map[OpeningKey, Opening] =
    OpeningDb.shortestLines.mapKeys(_.map(_.toLowerCase))

  private def byOpening(str: String, config: OpeningConfig) = {
    OpeningDb.shortestLines
      .get(OpeningKey(str))
      .orElse:
        val lowercase = (lila.common.String.decodeUriPath(str) | str).toLowerCase
        openingsByLowerCaseKey.get(OpeningKey.fromName(OpeningName(lowercase)))
  }.map(_.pgn).flatMap { fromPgn(_, config) }

  private def fromPgn(pgn: PgnMovesStr, config: OpeningConfig): Option[OpeningQuery] =
    for
      parsed <- Replay.mainline(pgn.into(PgnStr)).toOption
      replay <- parsed.valid.toOption
    yield OpeningQuery(replay, config)

  val firstYear = 2017
  val firstMonth = s"$firstYear-01"
