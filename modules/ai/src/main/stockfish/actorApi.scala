package lila.ai
package stockfish
package actorApi

import akka.actor.ActorRef

sealed trait State
case object Starting extends State
case object Idle extends State
case object IsReady extends State
case object Running extends State

sealed trait Stream { def text: String }
case class Out(text: String) extends Stream
case class Err(text: String) extends Stream

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
    playedMove: String,
    fen: Option[String]) extends Req {

  def analyse = true
}

case class FullAnalReq(moves: String, fen: Option[String])

case class Job(req: Req, sender: akka.actor.ActorRef, buffer: List[String]) {

  def +(str: String) = req.analyse.fold(copy(buffer = str :: buffer), this)

  // bestmove xyxy ponder xyxy
  def complete(str: String): Valid[Any] = req match {
    case r: PlayReq            ⇒ str.split(' ') lift 1 toValid "no bestmove found in " + str
    case AnalReq(_, played, _) ⇒ AnalyseParser(str :: buffer, played)
  }
}

