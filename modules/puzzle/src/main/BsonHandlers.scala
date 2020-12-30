package lila.puzzle

import chess.format.{ FEN, Uci }
import reactivemongo.api.bson._
import scala.util.Success
import scala.util.Try

import lila.db.BSON
import lila.db.dsl._
import lila.game.Game
import lila.rating.Glicko

private[puzzle] object BsonHandlers {

  implicit val PuzzleIdBSONHandler = stringIsoHandler(Puzzle.idIso)

  import Puzzle.BSONFields._

  implicit val PuzzleBSONReader = new BSONDocumentReader[Puzzle] {
    def readDocument(r: BSONDocument) = for {
      id      <- r.getAsTry[Puzzle.Id](id)
      gameId  <- r.getAsTry[Game.ID](gameId)
      fen     <- r.getAsTry[FEN](fen)
      lineStr <- r.getAsTry[String](line)
      line    <- lineStr.split(' ').toList.flatMap(Uci.Move.apply).toNel.toTry("Empty move list?!")
      glicko  <- r.getAsTry[Glicko](glicko)
      plays   <- r.getAsTry[Int](plays)
      vote    <- r.getAsTry[Float](vote)
      themes  <- r.getAsTry[Set[PuzzleTheme.Key]](themes)
    } yield Puzzle(
      id = id,
      gameId = gameId,
      fen = fen,
      line = line,
      glicko = glicko,
      plays = plays,
      vote = vote,
      themes = themes
    )
  }

  implicit val RoundIdHandler = tryHandler[PuzzleRound.Id](
    { case BSONString(v) =>
      v split PuzzleRound.idSep match {
        case Array(userId, puzzleId) => Success(PuzzleRound.Id(userId, Puzzle.Id(puzzleId)))
        case _                       => handlerBadValue(s"Invalid puzzle round id $v")
      }
    },
    id => BSONString(id.toString)
  )

  implicit val RoundThemeHandler = tryHandler[PuzzleRound.Theme](
    { case BSONString(v) =>
      PuzzleTheme
        .find(v.tail)
        .fold[Try[PuzzleRound.Theme]](handlerBadValue(s"Invalid puzzle round theme $v")) { theme =>
          Success(PuzzleRound.Theme(theme.key, v.head == '+'))
        }
    },
    rt => BSONString(s"${if (rt.vote) "+" else "-"}${rt.theme}")
  )

  implicit val RoundHandler = new BSON[PuzzleRound] {
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

  implicit val PathIdBSONHandler: BSONHandler[PuzzlePath.Id] = stringIsoHandler(PuzzlePath.pathIdIso)

  implicit val ThemeKeyBSONHandler: BSONHandler[PuzzleTheme.Key] = stringIsoHandler(PuzzleTheme.keyIso)
}
