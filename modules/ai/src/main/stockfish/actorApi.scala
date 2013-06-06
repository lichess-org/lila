package lila.ai
package stockfish
package actorApi

import akka.actor.ActorRef

sealed trait Req {
  def moves: String
  def fen: Option[String]
  def analyse: Boolean

  def chess960 = fen.isDefined
  def moveList = moves.split(' ').toList
}

case class PlayReq(
    moves: String,
    fen: Option[String],
    level: Int) extends Req {

  def analyse = false
}

case class AnalReq(
    moves: String,
    fen: Option[String]) extends Req {

  def analyse = true
  // def nextMove: Option[String] = moves lift analysis.size

  // def flush = for {
  //   move ← nextMove toValid "No move to flush"
  //   info ← AnalyseParser(infoBuffer)(move)
  // } yield copy(analysis = analysis + info, infoBuffer = Nil)
}

case class FullAnalReq(moves: String, fen: Option[String])

case class Job(req: Req, sender: akka.actor.ActorRef, buffer: List[String]) {

  def +(str: String) = req.analyse.fold(copy(buffer = str :: buffer), this)

  // bestmove xyxy ponder xyxy
  def complete(str: String): Valid[Any] =
    req.analyse.fold(
      (this + str) |> {
        case Job(req, _, buffer) ⇒
          req.moveList.lastOption toValid "empty move list" flatMap AnalyseParser(buffer)
      },
      str.split(' ') lift 1 toValid "no bestmove found in " + str)
}

