package lila.ai.stockfish
package actorApi

import akka.actor.ActorRef

case class Req(
    moves: String,
    fen: Option[String],
    level: Int,
    analyse: Boolean) {

  def chess960 = fen.isDefined
}

case class BestMove(move: Option[String]) 
