package shogi

import cats.data.Validated

import shogi.format.forsyth.Sfen
import shogi.format.ParsedMove
import shogi.format.usi.Usi

case class Game(
    situation: Situation,
    usiMoves: Vector[Usi] = Vector.empty,
    clock: Option[Clock] = None,
    plies: Int = 0,
    startedAtPly: Int = 0,
    startedAtMove: Int = 1
) {

  private def applySituation(sit: Situation, metrics: MoveMetrics = MoveMetrics.empty): Game = 
    copy(
      situation = sit,
      plies = plies + 1,
      usiMoves = sit.history.lastMove.fold(usiMoves)(usiMoves :+ _),
      clock = clock map { c =>
        val newC = c.step(metrics, sit.status.isEmpty)
        if (plies - startedAtPly == 1) newC.start else newC
      }
    )

  def apply(usi: Usi, metrics: MoveMetrics): Validated[String, Game] =
    situation(usi).map(applySituation(_, metrics))
  
  def apply(usi: Usi): Validated[String, Game] =
    situation(usi).map(applySituation(_))

  def apply(parsedMove: ParsedMove, metrics: MoveMetrics): Validated[String, Game] =
    situation(parsedMove).map(applySituation(_, metrics))

  
  def apply(parsedMove: ParsedMove): Validated[String, Game] =
    situation(parsedMove).map(applySituation(_))

  def board = situation.board

  def hands = situation.hands

  def color = situation.color

  def history = situation.history

  def variant = situation.variant

  // It starts at 1, and is incremented after Gote's move.
  def fullTurnNumber: Int = 1 + plies / 2

  def playedPlies: Int = plies - startedAtPly

  def moveNumber: Int = startedAtMove + playedPlies

  def withBoard(b: Board) = copy(situation = situation.copy(board = b))

  def withHands(hs: Hands) = copy(situation = situation.copy(hands = hs))

  def withColor(c: Color) = copy(situation = situation.copy(color = c))

  def withClock(c: Option[Clock]) = copy(clock = c)

  def withPlies(p: Int) = copy(plies = p)

  def toSfen: Sfen = Sfen(this)
}

object Game {
  def apply(situation: Situation): Game =
    new Game(situation)
  
  def apply(variant: shogi.variant.Variant): Game =
    Game(Situation(variant))

  def apply(variantOption: Option[shogi.variant.Variant], sfen: Option[Sfen]): Game = {
    val variant = variantOption | shogi.variant.Standard
    val g       = apply(variant)
    sfen
      .filterNot(_.initialOf(variant))
      .flatMap {
        _.toSituationPlus(variant)
      }
      .fold(g) { parsed =>
        g.copy(
          situation = Situation(
            board = parsed.situation.board,
            hands = parsed.situation.hands,
            color = parsed.situation.color,
            history = History.empty,
            variant = g.variant
          ),
          plies = parsed.plies,
          startedAtPly = parsed.plies,
          startedAtMove = parsed.moveNumber
        )
      }
  }
}
