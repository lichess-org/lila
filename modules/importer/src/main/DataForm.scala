package lila.importer

import shogi.format.pgn.{ KifParser, ParsedPgn, Reader, Tag, TagType, Tags }
import shogi.format.{ FEN, Forsyth }
import shogi.{ Color, Mode, Replay, Status }
import play.api.data._
import play.api.data.Forms._
import scala.util.chaining._
import scalaz.Validation.FlatMap._

import lila.game._

final class DataForm {

  lazy val importForm = Form(
    mapping(
      "kif"     -> nonEmptyText.verifying("invalidKif", p => checkKif(p).isSuccess),
      "analyse" -> optional(nonEmptyText)
    )(ImportData.apply)(ImportData.unapply)
  )

  def checkKif(kif: String): Valid[Preprocessed] = ImportData(kif, none).preprocess(none)
}

private case class TagResult(status: Status, winner: Option[Color])
case class Preprocessed(
    game: NewGame,
    replay: Replay,
    initialFen: Option[FEN],
    parsed: ParsedPgn
)

case class ImportData(kif: String, analyse: Option[String]) {

  private type TagPicker = Tag.type => TagType

  private val maxPlies = 600

  private def evenIncomplete(result: Reader.Result): Replay =
    result match {
      case Reader.Result.Complete(replay)      => replay
      case Reader.Result.Incomplete(replay, _) => replay
    }

  def preprocess(user: Option[String]): Valid[Preprocessed] =
    KifParser.full(kif) flatMap { parsed =>
      Reader.fullWithKif(
        kif,
        sans => sans.copy(value = sans.value take maxPlies),
        Tags.empty
      ) map evenIncomplete map { case replay @ Replay(setup, _, state) =>
        val initBoard    = parsed.tags.fen.map(_.value) flatMap Forsyth.<< map (_.board)
        val fromPosition = initBoard.nonEmpty && !parsed.tags.fen.contains(FEN(Forsyth.initial))
        val variant = {
          parsed.tags.variant | {
            if (fromPosition) shogi.variant.FromPosition
            else shogi.variant.Standard
          }
        } match {
          case shogi.variant.FromPosition if parsed.tags.fen.isEmpty => shogi.variant.Standard
          case shogi.variant.Standard if fromPosition                => shogi.variant.FromPosition
          case v                                                     => v
        }
        val game = state.copy(situation = state.situation withVariant variant)
        val initialFen = parsed.tags.fen.map(_.value) flatMap {
          Forsyth.<<<@(variant, _)
        } map Forsyth.>> map FEN.apply

        val status = parsed.tags(_.Termination).map(_.toLowerCase) match {
          case Some("投了") | None              => Status.Resign
          case Some("詰み")                     => Status.Mate
          case Some("中断")                     => Status.Aborted
          case Some("持将棋") | Some("千日手")      => Status.Draw
          case Some("入玉勝ち")                   => Status.Impasse27
          case Some("切れ負け") | Some("time-up") => Status.Outoftime
          case Some("反則勝ち") | Some("反則負け")    => Status.Cheat
          case Some(_)                        => Status.UnknownFinish
        }

        val date = parsed.tags.anyDate

        def name(whichName: TagPicker, whichRating: TagPicker): String =
          parsed.tags(whichName).fold("?") { n =>
            n + ~parsed.tags(whichRating).map(e => s" (${e take 8})")
          }

        val dbGame = Game
          .make(
            shogi = game,
            sentePlayer = Player.make(shogi.Sente, None) withName name(_.Sente, _.SenteElo),
            gotePlayer = Player.make(shogi.Gote, None) withName name(_.Gote, _.GoteElo),
            mode = Mode.Casual,
            source = Source.Import,
            pgnImport = PgnImport.make(user = user, date = date, kif = kif).some
          )
          .sloppy
          .start pipe { dbGame =>
          // apply the result from the board or the tags
          game.situation.status match {
            case Some(situationStatus) => dbGame.finish(situationStatus, game.situation.winner).game
            case None =>
              parsed.tags.resultColor
                .map {
                  case Some(color)                            => TagResult(status, color.some)
                  case None if status == Status.UnknownFinish => TagResult(Status.Draw, none)
                  case None                                   => TagResult(status, none)
                }
                .filter(_.status > Status.Started)
                .fold(dbGame) { res =>
                  dbGame.finish(res.status, res.winner).game
                }
          }
        }

        Preprocessed(NewGame(dbGame), replay.copy(state = game), initialFen, parsed)
      }
    }
}
