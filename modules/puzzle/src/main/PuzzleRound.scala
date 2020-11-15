package lila.puzzle

import org.joda.time.DateTime

import lila.user.User

case class PuzzleRound(
    id: PuzzleRound.Id,
    date: DateTime,
    win: Boolean,
    vote: Option[Boolean],
    // tags: List[RoundTag],
    weight: Option[Int]
) {

  def userId = id.userId
}

object PuzzleRound {

  val idSep = ':'

  case class Id(userId: User.ID, puzzleId: Puzzle.Id) {

    override def toString = s"${userId}$idSep${puzzleId}"
  }

  object BSONFields {
    val id     = "_id"
    val date   = "d"
    val win    = "w"
    val vote   = "v"
    val weight = "w"
    val user   = "u" // student denormalization
  }
}
