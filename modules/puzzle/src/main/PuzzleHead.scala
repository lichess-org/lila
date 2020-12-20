package lila.puzzle

case class PuzzleHead(
    _id: lila.user.User.ID,
    current: Option[PuzzleId], // current puzzle assigned to user (rated)
    last: PuzzleId             // last puzzle assigned to user
) {

  def id = _id
}

object PuzzleHead {

  object BSONFields {
    val id      = "_id"
    val current = "current"
    val last    = "last"
  }

  import reactivemongo.api.bson._

  implicit private[puzzle] val puzzleHeadBSONHandler = Macros.handler[PuzzleHead]
}
