package lila.puzzle

import shogi.format.usi.{ UciToUsi, Usi }
import shogi.format.forsyth.Sfen
import reactivemongo.api.bson._
import scala.util.{ Success, Try }

import lila.db.BSON
import lila.db.dsl._
import lila.game.Game
import lila.rating.Glicko

object BsonHandlers {

  implicit val PuzzleIdBSONHandler = stringIsoHandler(Puzzle.idIso)

  import Puzzle.{ BSONFields => F }

  implicit private[puzzle] val PuzzleBSONHandler = new BSON[Puzzle] {
    def reads(r: BSON.Reader) = {
      val line = r
        .get[String](F.line)
        .split(' ')
        .toList
        .flatMap(m => Usi.apply(m).orElse(UciToUsi.apply(m)))
        .toNel
        .get
      Puzzle(
        id = r.get[Puzzle.Id](F.id),
        sfen = r.get[Sfen](F.sfen),
        line = line,
        ambiguousPromotions = r.getO[List[Int]](F.ambiguousPromotions).getOrElse(Nil),
        glicko = r.getD[Glicko](F.glicko, Puzzle.glickoDefault(line.size)),
        plays = r.intD(F.plays),
        vote = r.floatD(F.vote),
        themes = r.getD[Set[PuzzleTheme.Key]](F.themes),
        gameId = r.getO[Game.ID](F.gameId),
        author = r.strO(F.author),
        description = r.strO(F.description),
        submittedBy = r.strO(F.submittedBy)
      )
    }

    def writes(w: BSON.Writer, p: Puzzle) =
      BSONDocument(
        F.id                  -> p.id,
        F.sfen                -> p.sfen,
        F.line                -> p.line.map(_.usi).toList.mkString(" "),
        F.ambiguousPromotions -> p.ambiguousPromotions,
        F.glicko              -> p.glicko,
        F.plays               -> p.plays,
        F.vote                -> p.vote,
        F.themes              -> p.themes,
        F.gameId              -> p.gameId,
        F.author              -> p.author,
        F.description         -> p.description,
        F.submittedBy         -> p.submittedBy
      )

  }

  implicit private[puzzle] val RoundIdHandler = tryHandler[PuzzleRound.Id](
    { case BSONString(v) =>
      v split PuzzleRound.idSep match {
        case Array(userId, puzzleId) => Success(PuzzleRound.Id(userId, Puzzle.Id(puzzleId)))
        case _                       => handlerBadValue(s"Invalid puzzle round id $v")
      }
    },
    id => BSONString(id.toString)
  )

  implicit private[puzzle] val RoundThemeHandler = tryHandler[PuzzleRound.Theme](
    { case BSONString(v) =>
      PuzzleTheme
        .find(v.tail)
        .fold[Try[PuzzleRound.Theme]](handlerBadValue(s"Invalid puzzle round theme $v")) { theme =>
          Success(PuzzleRound.Theme(theme.key, v.head == '+'))
        }
    },
    rt => BSONString(s"${if (rt.vote) "+" else "-"}${rt.theme}")
  )

  implicit private[puzzle] val RoundHandler = new BSON[PuzzleRound] {
    import PuzzleRound.BSONFields._
    def reads(r: BSON.Reader) = PuzzleRound(
      id = r.get[PuzzleRound.Id](id),
      win = r.bool(win),
      fixedAt = r.dateO(fixedAt),
      date = r.date(date),
      vote = r.intO(vote),
      themes = r.getsD[PuzzleRound.Theme](themes)
    )
    def writes(w: BSON.Writer, r: PuzzleRound) =
      $doc(
        id      -> r.id,
        win     -> r.win,
        fixedAt -> r.fixedAt,
        date    -> r.date,
        vote    -> r.vote,
        themes  -> w.listO(r.themes)
      )
  }

  implicit private[puzzle] val PathIdBSONHandler: BSONHandler[PuzzlePath.Id] = stringIsoHandler(
    PuzzlePath.pathIdIso
  )

  implicit private[puzzle] val ThemeKeyBSONHandler: BSONHandler[PuzzleTheme.Key] = stringIsoHandler(
    PuzzleTheme.keyIso
  )
}
