package lila.insight

import chess.format.Nag
import chess.Role
import lila.analyse.{ Accuracy, Advice }
import lila.game.{ Game, Pov, GameRepo }
import lila.user.User

object PovToEntry {

  private type Ply = Int

  case class RichPov(
    pov: Pov,
    provisional: Boolean,
    initialFen: Option[String],
    analysis: Option[lila.analyse.Analysis],
    division: chess.Division,
    moveAccuracy: Option[List[Int]],
    advices: Map[Ply, Advice])

  def apply(game: Game, userId: String, provisional: Boolean): Fu[Either[Game, Entry]] =
    enrich(game, userId, provisional) map
      (_ flatMap convert toRight game) addFailureEffect { e =>
        println(s"http://l.org/${game.id}")
      }

  private def removeWrongAnalysis(game: Game): Boolean = {
    if (game.metadata.analysed && !game.analysable) {
      GameRepo setUnanalysed game.id
      lila.analyse.AnalysisRepo remove game.id
      true
    }
    false
  }

  private def enrich(game: Game, userId: String, provisional: Boolean): Fu[Option[RichPov]] =
    if (removeWrongAnalysis(game)) fuccess(none)
    else lila.game.Pov.ofUserId(game, userId) ?? { pov =>
      lila.game.GameRepo.initialFen(game) zip
        (game.metadata.analysed ?? lila.analyse.AnalysisRepo.doneById(game.id)) map {
          case (fen, an) => RichPov(
            pov = pov,
            provisional = provisional,
            initialFen = fen,
            analysis = an,
            division = chess.Replay.boards(
              moveStrs = game.pgnMoves,
              initialFen = fen,
              variant = game.variant
            ).toOption.fold(chess.Division.empty)(chess.Divider.apply),
            moveAccuracy = an.map { Accuracy.diffsList(pov, _) },
            advices = an.?? { _.advices.map { a => a.info.ply -> a }.toMap }
          ).some
        }
    }

  private def pgnMoveToRole(pgn: String): Role = pgn.head match {
    case 'N'       => chess.Knight
    case 'B'       => chess.Bishop
    case 'R'       => chess.Rook
    case 'Q'       => chess.Queen
    case 'K' | 'O' => chess.King
    case _         => chess.Pawn
  }

  private def makeMoves(from: RichPov): List[Move] = {
    val cpDiffs = ~from.moveAccuracy toVector
    val prevInfos = from.analysis.?? { an =>
      Accuracy.prevColorInfos(from.pov, an) |> { is =>
        from.pov.color.fold(is, is.map(_.reverse))
      }
    }
    from.pov.game.moveTimes(from.pov.color).zip(
      from.pov.game.pgnMoves(from.pov.color) map pgnMoveToRole
    ).zipWithIndex.map {
        case ((tenths, role), i) =>
          val ply = i * 2 + from.pov.color.fold(1, 2)
          val prevInfo = prevInfos lift i
          val opportunism = from.advices.get(ply - 1) flatMap {
            case o if o.nag == Nag.Blunder => from.advices get ply match {
              case Some(p) if p.nag == Nag.Blunder => false.some
              case _                               => true.some
            }
            case _ => none
          }
          val luck = from.advices.get(ply) flatMap {
            case o if o.nag == Nag.Blunder => from.advices.get(ply + 1) match {
              case Some(p) if p.nag == Nag.Blunder => true.some
              case _                               => false.some
            }
            case _ => none
          }
          Move(
            phase = Phase.of(from.division, ply),
            tenths = tenths,
            role = role,
            eval = prevInfo.flatMap(_.score).map(_.ceiled.centipawns),
            mate = prevInfo.flatMap(_.mate),
            cpl = cpDiffs lift i,
            opportunism = opportunism,
            luck = luck)
      }
  }

  private def convert(from: RichPov): Option[Entry] = for {
    myId <- from.pov.player.userId
    myRating <- from.pov.player.rating
    opRating <- from.pov.opponent.rating
    perfType <- from.pov.game.perfType
  } yield Entry(
    id = Entry povToId from.pov,
    userId = myId,
    color = from.pov.color,
    perf = perfType,
    eco = Ecopening fromGame from.pov.game,
    opponentRating = opRating,
    opponentStrength = RelativeStrength(opRating - myRating),
    moves = makeMoves(from),
    result = from.pov.game.winnerUserId match {
      case None                 => Result.Draw
      case Some(u) if u == myId => Result.Win
      case _                    => Result.Loss
    },
    termination = Termination fromStatus from.pov.game.status,
    ratingDiff = ~from.pov.player.ratingDiff,
    analysed = from.analysis.isDefined,
    provisional = from.provisional,
    date = from.pov.game.createdAt)
}
