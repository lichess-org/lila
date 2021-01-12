package lila.puzzle

import org.joda.time.DateTime

import lila.common.Day
import lila.user.User

case class PuzzleRound(
    id: PuzzleRound.Id,
    win: Boolean,              // last result
    fixedAt: Option[DateTime], // date of first-replaying lost puzzle and winning it
    date: DateTime,            // date of first playing the puzzle
    vote: Option[Int] = None,
    themes: List[PuzzleRound.Theme] = Nil
) {

  def userId = id.userId

  def themeVote(theme: PuzzleTheme.Key, vote: Option[Boolean]): Option[List[PuzzleRound.Theme]] =
    themes.find(_.theme == theme) match {
      case None =>
        vote map { v =>
          PuzzleRound.Theme(theme, v) :: themes
        }
      case Some(prev) =>
        vote match {
          case None                      => themes.filter(_.theme != theme).some
          case Some(v) if v == prev.vote => none
          case Some(v) =>
            themes.map {
              case t if t.theme == theme => t.copy(vote = v)
              case t                     => t
            }.some
        }
    }

  def nonEmptyThemes = themes.nonEmpty option themes

  def updateWithWin(win: Boolean) = copy(
    win = win,
    fixedAt = fixedAt orElse win.option(DateTime.now)
  )
}

object PuzzleRound {

  val idSep = ':'

  case class Id(userId: User.ID, puzzleId: Puzzle.Id) {

    override def toString = s"${userId}$idSep${puzzleId}"
  }

  case class Theme(theme: PuzzleTheme.Key, vote: Boolean)

  case class WithPuzzle(round: PuzzleRound, puzzle: Puzzle)

  // max 7 theme upvotes
  // unlimited downvotes
  def themesLookSane(themes: List[Theme]): Boolean = themes.count(_.vote) < 8

  object BSONFields {
    val id      = "_id"
    val win     = "w" // last result
    val fixedAt = "f" // date of first-replaying lost puzzle and winning it
    val vote    = "v"
    val themes  = "t"
    val puzzle  = "p" // only if themes is set!
    val weight  = "e"
    val user    = "u"
    val date    = "d" // date of first playing the puzzle
    val theme   = "h"
  }

  import lila.db.dsl._
  def puzzleLookup(colls: PuzzleColls, pipeline: List[Bdoc] = Nil) =
    $doc(
      "$lookup" -> $doc(
        "from" -> colls.puzzle.name.value,
        "as"   -> "puzzle",
        "let" -> $doc(
          "pid" -> $doc("$arrayElemAt" -> $arr($doc("$split" -> $arr("$_id", ":")), 1))
        ),
        "pipeline" -> {
          $doc(
            "$match" -> $doc(
              "$expr" -> $doc(
                $doc("$eq" -> $arr("$_id", "$$pid"))
              )
            )
          ) :: pipeline
        }
      )
    )
}
