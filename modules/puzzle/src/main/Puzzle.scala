package lila.puzzle

import cats.data.NonEmptyList
import chess.format.{ FEN, Forsyth, Uci }

import lila.rating.Glicko

case class Puzzle(
    id: Puzzle.Id,
    gameId: lila.game.Game.ID,
    fen: FEN,
    line: NonEmptyList[Uci.Move],
    glicko: Glicko,
    plays: Int,
    vote: Float, // denormalized ratio of voteUp/voteDown
    themes: Set[PuzzleTheme.Key]
) {
  // ply after "initial move" when we start solving
  def initialPly: Int =
    fen.fullMove ?? { fm =>
      fm * 2 - color.fold(1, 2)
    }

  lazy val fenAfterInitialMove: FEN = {
    for {
      sit1 <- Forsyth << fen
      sit2 <- sit1.move(line.head).toOption.map(_.situationAfter)
    } yield Forsyth >> sit2
  } err s"Can't apply puzzle $id first move"

  def color = fen.color.fold[chess.Color](chess.White)(!_)
}

object Puzzle {

  val idSize = 5

  case class Id(value: String) extends AnyVal with StringValue

  /* The mobile app requires numerical IDs.
   * We convert string ids from and to Longs using base 62
   */
  object numericalId {

    private val powers: List[Long] =
      (0 until idSize).toList.map(m => Math.pow(62, m).toLong)

    def apply(id: Id): Long = id.value.toList
      .zip(powers)
      .foldLeft(0L) { case (l, (char, pow)) =>
        l + charToInt(char) * pow
      }

    def apply(l: Long): Option[Id] = {
      val str = powers.reverse
        .foldLeft(("", l)) { case ((id, rest), pow) =>
          val frac = rest / pow
          (s"${intToChar(frac.toInt)}$id", rest - frac * pow)
        }
        ._1
      (str.size == idSize) option Id(str)
    }

    private def charToInt(c: Char) = {
      val i = c.toInt
      if (i > 96) i - 71
      else if (i > 64) i - 65
      else i + 4
    }

    private def intToChar(i: Int): Char = {
      if (i < 26) i + 65
      else if (i < 52) i + 71
      else i - 4
    }.toChar
  }

  case class UserResult(
      puzzleId: Id,
      userId: lila.user.User.ID,
      result: Result,
      rating: (Int, Int)
  )

  object BSONFields {
    val id       = "_id"
    val gameId   = "gameId"
    val fen      = "fen"
    val line     = "line"
    val glicko   = "glicko"
    val vote     = "vote"
    val voteUp   = "vu"
    val voteDown = "vd"
    val plays    = "plays"
    val themes   = "themes"
    val day      = "day"
    val dirty    = "dirty" // themes need to be denormalized
  }

  implicit val idIso = lila.common.Iso.string[Id](Id.apply, _.value)
}
