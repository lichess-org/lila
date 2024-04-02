package lila.tree

import chess.{ Game as ChessGame, * }

trait Game:
  val id: GameId
  val chess: ChessGame
  def bothClockStates: Option[Vector[Centis]]
  def drawOfferPlies: Set[Ply]

  export chess.{ situation, ply, clock, sans, startedAtPly, player as turnColor }
  export chess.situation.board
  export chess.situation.board.{ history, variant }
