package lidraughts.insight

import draughts.{ Role, Board }
import lidraughts.analyse.{ Accuracy, Advice }
import lidraughts.game.{ Game, Pov, GameRepo }
import scalaz.NonEmptyList

object PovToEntry {

  private type Ply = Int

  case class RichPov(
      pov: Pov,
      provisional: Boolean,
      initialFen: Option[String],
      analysis: Option[lidraughts.analyse.Analysis],
      division: draughts.Division,
      moveAccuracy: Option[List[Int]],
      boards: NonEmptyList[Board],
      movetimes: NonEmptyList[Int],
      advices: Map[Ply, Advice]
  )

  def apply(game: Game, userId: String, provisional: Boolean): Fu[Either[Game, Entry]] =
    enrich(game, userId, provisional) map
      (_ flatMap convert toRight game)

  private def removeWrongAnalysis(game: Game): Boolean = {
    if (game.metadata.analysed && !game.analysable) {
      GameRepo setUnanalysed game.id
      lidraughts.analyse.AnalysisRepo remove game.id
      true
    }
    false
  }

  private def enrich(game: Game, userId: String, provisional: Boolean): Fu[Option[RichPov]] =
    if (removeWrongAnalysis(game)) fuccess(none)
    else lidraughts.game.Pov.ofUserId(game, userId) ?? { pov =>
      lidraughts.game.GameRepo.initialFen(game) zip
        (game.metadata.analysed ?? lidraughts.analyse.AnalysisRepo.byId(game.id)) map {
          case (fen, an) => for {
            boards <- draughts.Replay.boards(
              moveStrs = game.pdnMoves,
              initialFen = fen map draughts.format.FEN,
              variant = game.variant
            ).toOption.flatMap(_.toNel)
            movetimes <- game.moveTimes(pov.color).flatMap(_.map(_.roundTenths).toNel)
          } yield RichPov(
            pov = pov,
            provisional = provisional,
            initialFen = fen,
            analysis = an,
            division = draughts.Divider(boards.toList),
            moveAccuracy = an.map { Accuracy.diffsList(pov, _) },
            boards = boards,
            movetimes = movetimes,
            advices = an.?? { _.advices.map { a => a.info.ply -> a }(scala.collection.breakOut) }
          )
        }
    }

  private def pdnMoveToRole(pdn: String): Role = pdn.head match {
    /*case 'N' => draughts.Knight
    case 'B' => draughts.Bishop
    case 'R' => draughts.Rook
    case 'Q' => draughts.Queen*/
    case 'K' | 'O' => draughts.King
    case _ => draughts.Man
  }

  private def makeMoves(from: RichPov): List[Move] = {
    val cpDiffs = ~from.moveAccuracy toVector
    val prevInfos = from.analysis.?? { an =>
      Accuracy.prevColorInfos(from.pov, an) |> { is =>
        from.pov.color.fold(is, is.map(_.invert))
      }
    }
    val movetimes = from.movetimes.toList
    val roles = from.pov.game.pdnMoves(from.pov.color) map pdnMoveToRole
    val boards = {
      val pivot = if (from.pov.color == from.pov.game.startColor) 0 else 1
      from.boards.toList.zipWithIndex.collect {
        case (e, i) if (i % 2) == pivot => e
      }
    }
    movetimes.zip(roles).zip(boards).zipWithIndex.map {
      case tenths ~ role ~ board ~ i =>
        val ply = i * 2 + from.pov.color.fold(1, 2)
        val prevInfo = prevInfos lift i
        val opportunism = from.advices.get(ply - 1) flatMap {
          case o if o.judgment.isBlunder => from.advices get ply match {
            case Some(p) if p.judgment.isBlunder => false.some
            case _ => true.some
          }
          case _ => none
        }
        val luck = from.advices.get(ply) flatMap {
          case o if o.judgment.isBlunder => from.advices.get(ply + 1) match {
            case Some(p) if p.judgment.isBlunder => true.some
            case _ => false.some
          }
          case _ => none
        }
        Move(
          phase = Phase.of(from.division, ply),
          tenths = tenths,
          role = role,
          eval = prevInfo.flatMap(_.cp).map(_.ceiled.centipieces),
          win = prevInfo.flatMap(_.win).map(_.moves),
          cpl = cpDiffs lift i map (_ min 1000),
          material = board.materialImbalance * from.pov.color.fold(1, -1),
          opportunism = opportunism,
          luck = luck
        )
    }
  }

  private def convert(from: RichPov): Option[Entry] = {
    import from._
    import pov.game
    for {
      myId <- pov.player.userId
      myRating <- pov.player.rating
      opRating <- pov.opponent.rating
      perfType <- game.perfType
    } yield Entry(
      id = Entry povToId pov,
      number = 0, // temporary :-/ the Indexer will set it
      userId = myId,
      color = pov.color,
      perf = perfType,
      eco =
        if (game.playable || game.turns < 4 || game.fromPosition || game.variant.exotic) none
        else draughts.opening.Ecopening fromGame game.pdnMoves.toList,
      myCastling = Castling.fromMoves(game pdnMoves pov.color),
      opponentRating = opRating,
      opponentStrength = RelativeStrength(opRating - myRating),
      opponentCastling = Castling.fromMoves(game pdnMoves !pov.color),
      moves = makeMoves(from),
      result = game.winnerUserId match {
        case None => Result.Draw
        case Some(u) if u == myId => Result.Win
        case _ => Result.Loss
      },
      termination = Termination fromStatus game.status,
      ratingDiff = ~pov.player.ratingDiff,
      analysed = analysis.isDefined,
      provisional = provisional,
      date = game.createdAt
    )
  }
}
