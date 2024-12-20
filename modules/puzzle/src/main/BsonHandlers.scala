package lila.puzzle

import chess.format.{ Fen, Uci }
import chess.rating.glicko.Glicko
import reactivemongo.api.bson.*
import scala.util.{ Success, Try }

import lila.db.BSON
import lila.db.dsl.{ *, given }

object BsonHandlers:

  import Puzzle.BSONFields.*
  import lila.rating.Glicko.glickoHandler

  private[puzzle] given puzzleReader: BSONDocumentReader[Puzzle] with
    def readDocument(r: BSONDocument) = for
      id      <- r.getAsTry[PuzzleId](id)
      gameId  <- r.getAsTry[GameId](gameId)
      fen     <- r.getAsTry[Fen.Full](fen)
      lineStr <- r.getAsTry[String](line)
      line    <- lineStr.split(' ').toList.flatMap(Uci.Move.apply).toNel.toTry("Empty move list?!")
      glicko  <- r.getAsTry[Glicko](glicko)
      plays   <- r.getAsTry[Int](plays)
      vote    <- r.getAsTry[Float](vote)
      themes  <- r.getAsTry[Set[PuzzleTheme.Key]](themes)
    yield Puzzle(
      id = id,
      gameId = gameId,
      fen = fen,
      line = line,
      glicko = glicko,
      plays = plays,
      vote = vote,
      themes = themes.diff(PuzzleTheme.hiddenThemes)
    )

  private[puzzle] given roundIdHandler: BSONHandler[PuzzleRound.Id] = tryHandler[PuzzleRound.Id](
    { case BSONString(v) =>
      v.split(PuzzleRound.idSep) match
        case Array(userId, puzzleId) => Success(PuzzleRound.Id(UserId(userId), PuzzleId(puzzleId)))
        case _                       => handlerBadValue(s"Invalid puzzle round id $v")
    },
    id => BSONString(id.toString)
  )

  private[puzzle] given BSONHandler[PuzzleRound.Theme] = tryHandler[PuzzleRound.Theme](
    { case BSONString(v) =>
      PuzzleTheme
        .find(v.tail)
        .fold[Try[PuzzleRound.Theme]](handlerBadValue(s"Invalid puzzle round theme $v")) { theme =>
          Success(PuzzleRound.Theme(theme.key, v.head == '+'))
        }
    },
    rt => BSONString(s"${if rt.vote then "+" else "-"}${rt.theme}")
  )

  given roundHandler: BSON[PuzzleRound] with
    import PuzzleRound.BSONFields.*
    def reads(r: BSON.Reader) = PuzzleRound(
      id = r.get[PuzzleRound.Id](id),
      win = r.get[PuzzleWin](win),
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

  import PuzzlePath.given
  private[puzzle] given pathIdHandler: BSONHandler[PuzzlePath.Id] = stringIsoHandler

  import PuzzleAngle.given
  private[puzzle] given BSONHandler[PuzzleAngle] = stringIsoHandler
