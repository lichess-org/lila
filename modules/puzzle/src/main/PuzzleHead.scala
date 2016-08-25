package lila.puzzle

import org.joda.time.DateTime

case class PuzzleHead(
    _id: String, // userId
    current: Option[PuzzleId],
    last: Option[PuzzleId]) {

  def id = _id
}

object PuzzleHead {

  object BSONFields {
    val id = "_id"
    val current = "c"
    val last = "l"
  }

  import reactivemongo.bson._
  import lila.db.BSON
  
  implicit val puzzleHeadBSONHandler = Macros.handler[PuzzleHead]
}
