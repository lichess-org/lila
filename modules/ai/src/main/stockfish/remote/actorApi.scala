package lila.ai.stockfish.remote.actorApi

case object CalculateLoad

case class Play(uciMoves: List[String], fen: String, level: Int) 
case class Analyse(uciMoves: List[String], fen: String) 
