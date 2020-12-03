package lila.puzzle

import org.joda.time.DateTime

import lila.user.User

case class PuzzleRound(
    id: PuzzleRound.Id,
    date: DateTime,
    win: Boolean,
    vote: Option[Boolean] = None,
    themes: List[PuzzleRound.Theme] = Nil,
    weight: Option[Int] = None
) {

  def userId = id.userId

  def themeVote(theme: PuzzleTheme.Key, vote: Option[Boolean]): Option[Boolean] =
    themes.find(_.theme == theme)
}

object PuzzleRound {

  val idSep = ':'

  case class Id(userId: User.ID, puzzleId: Puzzle.Id) {

    override def toString = s"${userId}$idSep${puzzleId}"
  }

  case class Theme(theme: PuzzleTheme.Key, vote: Boolean)

  object BSONFields {
    val id     = "_id"
    val date   = "d"
    val win    = "w"
    val vote   = "v"
    val themes = "t"
    val weight = "w"
    val user   = "u" // student denormalization
  }
}
