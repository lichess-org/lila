package lila.ai.stockfish.remote.actorApi

import lila.game.Game

case object CalculateLoad

case class Play(pgn: String, fen: String, level: Int) 
case class Analyse(pgn: String, fen: String) 
