package lila.puzzle

import cats.data.NonEmptyList
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
    sfen.stepNumber ?? { mn =>
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

  def lastUsi: String =
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
