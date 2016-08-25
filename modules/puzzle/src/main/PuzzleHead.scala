package lila.puzzle

import org.joda.time.DateTime

case class PuzzleHead(
    _id: String, // userId
    current: Option[PuzzleId],
    last: Option[PuzzleId]) {

  def id = _id
}

object PuzzleHead {

  import reactivemongo.bson._
  import lila.db.BSON
  
  implicit val puzzleHeadBSONHandler = Macros.handler[PuzzleHead]
}
