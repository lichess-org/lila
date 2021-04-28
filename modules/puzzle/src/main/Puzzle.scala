package lila.puzzle

import scalaz.NonEmptyList
import chess.format.{ FEN, Forsyth, Uci }

import lila.rating.Glicko

case class Puzzle(
    id: Puzzle.Id,
    fen: String,
    line: NonEmptyList[Uci],
    glicko: Glicko,
    plays: Int,
    vote: Float, // denormalized ratio of voteUp/voteDown
    gameId: Option[lila.game.Game.ID],
    themes: Set[PuzzleTheme.Key],
    author: Option[String] = None,
    description: Option[String] = None
) {
  // ply after "initial move" when we start solving
  def initialPly: Int = {
    fen.split(' ').lift(3).flatMap(_.toIntOption) ?? { move =>
      move - 1
    }
  }

  lazy val fenAfterInitialMove: FEN = {
    gameId match {
      case Some(_) =>
        for {
          sit1 <- Forsyth << fen
          sit2 <- line.head match {
            case Uci.Drop(role, pos)        => sit1.drop(role, pos).toOption.map(_.situationAfter)
            case Uci.Move(orig, dest, prom) => sit1.move(orig, dest, prom).toOption.map(_.situationAfter)
          }
        } yield FEN(Forsyth >> sit2)
      case None =>
        for {
          sit1 <- Forsyth << fen
        } yield FEN(Forsyth >> sit1)
    }

  } err s"Can't apply puzzle $id first move"

  def color: chess.Color =
    gameId match {
      case Some(_) => Forsyth.getColor(fen).fold[chess.Color](chess.Sente)(!_)
      case None    => Forsyth.getColor(fen).getOrElse(chess.Sente)
    }

  def lastMove: String =
    gameId match {
      case Some(_) => line.head.uci
      case None    => ""
    }
}

object Puzzle {

  val idSize = 5

  case class Id(value: String) extends AnyVal with StringValue

  def toId(id: String) = id.size == idSize option Id(id)

  /* The mobile app requires numerical IDs.
   * We convert string ids from and to Longs using base 62
   */
  object numericalId {

    private val powers: List[Long] =
      (0 until idSize).toList.map(m => Math.pow(62, m).toLong)

    def apply(id: Id): Long =
      id.value.toList
        .zip(powers)
        .foldLeft(0L) {
          case (l, (char, pow)) =>
            l + charToInt(char) * pow
        }

    def apply(l: Long): Option[Id] =
      (l > 130_000) ?? {
        val str = powers.reverse
          .foldLeft(("", l)) {
            case ((id, rest), pow) =>
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
    val id          = "_id"
    val gameId      = "gameId"
    val fen         = "fen"
    val line        = "line"
    val glicko      = "glicko"
    val vote        = "vote"
    val voteUp      = "vu"
    val voteDown    = "vd"
    val plays       = "plays"
    val themes      = "themes"
    val day         = "day"
    val dirty       = "dirty" // themes need to be denormalized
    val author      = "a"
    val description = "dsc"
  }

  implicit val idIso = lila.common.Iso.string[Id](Id.apply, _.value)
}
