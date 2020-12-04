package lila.puzzle

import chess.format.{ FEN, Uci }
import reactivemongo.api.bson._
import scala.util.Success

import lila.db.BSON
import lila.db.dsl._
import lila.game.Game
import lila.rating.Glicko
import scala.util.Try

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
      vote    <- r.getAsTry[Int](vote)
      plays   <- r.getAsTry[Int](plays)
      themes  <- r.getAsTry[Set[PuzzleTheme.Key]](themes)
    } yield Puzzle(
      id = id,
      gameId = gameId,
      fen = fen,
      line = line,
      glicko = glicko,
      vote = vote,
      plays = plays,
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
      date = r.date(date),
      win = r.bool(win),
      vote = r.boolO(vote),
      themes = r.getsD[PuzzleRound.Theme](themes),
      weight = r.intO(weight)
    )
    def writes(w: BSON.Writer, r: PuzzleRound) =
      $doc(
        id     -> r.id,
        date   -> r.date,
        win    -> r.win,
        vote   -> r.vote,
        themes -> w.listO(r.themes),
        weight -> r.weight
      )
  }

  implicit val PathIdBSONHandler: BSONHandler[Puzzle.PathId] = stringIsoHandler(Puzzle.pathIdIso)

  implicit val ThemeKeyBSONHandler: BSONHandler[PuzzleTheme.Key] = stringIsoHandler(PuzzleTheme.keyIso)
}
