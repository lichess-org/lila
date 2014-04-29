package lila.ai
package actorApi

import akka.actor.ActorRef

case class MoveResult(move: String)
case class PlayResult(progress: lila.game.Progress, move: chess.Move)

case class AddTime(time: Int)

sealed trait State
case object Starting extends State
case object Idle extends State
case object IsReady extends State
case object Running extends State

sealed trait Stream { def text: String }
case class Out(text: String) extends Stream
case class Err(text: String) extends Stream

sealed trait Req {
  def moves: List[String]
  def fen: Option[String]
  def analyse: Boolean
  def requestedByHuman: Boolean
  def priority: Int

  def chess960 = fen.isDefined
}

case class PlayReq(
    moves: List[String],
    fen: Option[String],
    level: Int) extends Req {

  val analyse = false
  val requestedByHuman = true

  val priority = 999999 - level
}

case class AnalReq(
    moves: List[String],
    fen: Option[String],
    totalSize: Int,
    requestedByHuman: Boolean) extends Req {

  val priority =
    if (requestedByHuman) -totalSize
    else -1000 - totalSize

  def analyse = true

  def isStart = moves.isEmpty && fen.isEmpty
}

case class FullAnalReq(
  moves: List[String],
  fen: Option[String],
  requestedByHuman: Boolean)

case class Job(req: Req, sender: akka.actor.ActorRef, buffer: List[String]) {

  def +(str: String) = if (req.analyse) copy(buffer = str :: buffer) else this

  // bestmove xyxy ponder xyxy
  def complete(str: String): Option[Any] = req match {
    case _: PlayReq => str split ' ' lift 1
    case _: AnalReq => buffer.headOption map EvaluationParser.apply
  }
}
