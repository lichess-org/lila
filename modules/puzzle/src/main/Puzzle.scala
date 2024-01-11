package lila.puzzle

import cats.data.NonEmptyList
import cats.implicits._
import shogi.format.forsyth.Sfen
import shogi.format.usi.Usi

import lila.rating.Glicko

case class Puzzle(
    id: Puzzle.Id,
    sfen: Sfen,
    line: NonEmptyList[Usi],
    ambiguousPromotions: List[Int],
    glicko: Glicko,
    plays: Int,
    vote: Float, // denormalized ratio of voteUp/voteDown
    gameId: Option[lila.game.Game.ID],
    themes: Set[PuzzleTheme.Key],
    author: Option[String] = None,
    description: Option[String] = None,
    submittedBy: Option[String] = None
) {
  // ply after "initial move" when we start solving
  def initialPly: Int =
    sfen.moveNumber ?? { mn =>
      mn - 1
    }

  lazy val sfenAfterInitialMove: Sfen = {
    gameId match {
      case Some(_) =>
        for {
          sit1 <- sfen.toSituation(shogi.variant.Standard)
          sit2 <- sit1(line.head).toOption
        } yield sit2.toSfen
      case None => sfen.some
    }
  } err s"Can't apply puzzle $id first move"

  def color: shogi.Color =
    gameId match {
      case Some(_) => sfen.color.fold[shogi.Color](shogi.Sente)(!_)
      case None    => sfen.color.getOrElse(shogi.Sente)
    }

  def lastMove: String =
    gameId match {
      case Some(_) => line.head.usi
      case None    => ""
    }
}

object Puzzle {

  val idSize = 5

  case class Id(value: String) extends AnyVal with StringValue

  def toId(id: String) = id.sizeIs == idSize option Id(id)

  // idk - tweak it later
  def glickoDefault(nbMoves: Int) = Glicko(600d + nbMoves * 150d, 500d, 0.08d)

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

    def apply(l: Long): Option[Id] = (l > 130_000) ?? {
      val str = powers.reverse
        .foldLeft(("", l)) { case ((id, rest), pow) =>
          val frac = rest / pow
          (s"${intToChar(frac.toInt)}$id", rest - frac * pow)
        }
        ._1
      (str.sizeIs == idSize) option Id(str)
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
    val id                  = "_id"
    val gameId              = "gameId"
    val sfen                = "sfen"
    val line                = "line"
    val ambiguousPromotions = "ambP"
    val glicko              = "glicko"
    val vote                = "vote"
    val voteUp              = "vu"
    val voteDown            = "vd"
    val plays               = "plays"
    val themes              = "themes"
    val day                 = "day"
    val dirty               = "dirty" // themes need to be denormalized
    val author              = "a"
    val description         = "dsc"
    val submittedBy         = "sb"
  }

  implicit val idIso = lila.common.Iso.string[Id](Id.apply, _.value)
}
