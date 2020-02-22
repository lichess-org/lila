package lidraughts.importer

import draughts.format.pdn.{ Parser, Reader, ParsedPdn, Tag, TagType, Tags }
import draughts.format.{ FEN, Forsyth }
import draughts.{ Replay, Color, Mode, Status }
import play.api.data._
import play.api.data.Forms._
import scalaz.Validation.FlatMap._

import lidraughts.game._

private[importer] final class DataForm {

  lazy val importForm = Form(mapping(
    "pdn" -> nonEmptyText.verifying("invalidPdn", p => checkPdn(p).isSuccess),
    "analyse" -> optional(nonEmptyText)
  )(ImportData.apply)(ImportData.unapply))

  def checkPdn(pdn: String): Valid[Preprocessed] = ImportData(pdn, none).preprocess(none)
}

private[importer] case class TagResult(status: Status, winner: Option[Color])
case class Preprocessed(
    game: NewGame,
    replay: Replay,
    initialFen: Option[FEN],
    parsed: ParsedPdn
)

case class ImportData(pdn: String, analyse: Option[String]) {

  private type TagPicker = Tag.type => TagType

  private val maxPlies = 500

  private def evenIncomplete(result: Reader.Result): Replay = result match {
    case Reader.Result.Complete(replay) => replay
    case Reader.Result.Incomplete(replay, _) => replay
  }

  def preprocess(user: Option[String]): Valid[Preprocessed] = Parser.full(pdn) flatMap {
    case parsed @ ParsedPdn(_, tags, sans) => Reader.fullWithSans(
      pdn,
      sans => sans.copy(value = sans.value take maxPlies),
      tags,
      iteratedCapts = true
    ) map evenIncomplete map {
        case replay @ Replay(setup, _, state) =>
          val initBoard = parsed.tags.fen.map(_.value) flatMap Forsyth.<< map (_.board)
          val fromPosition = initBoard.nonEmpty && !(parsed.tags.fen.contains(FEN(Forsyth.initial)) || parsed.tags.fen.contains(FEN(Forsyth.initialMoveAndPieces)))
          val variant = {
            parsed.tags.variant | {
              if (fromPosition) draughts.variant.FromPosition
              else draughts.variant.Standard
            }
          } match {
            case draughts.variant.FromPosition if parsed.tags.fen.isEmpty => draughts.variant.Standard
            case draughts.variant.Standard if fromPosition => draughts.variant.FromPosition
            case v => v
          }
          val game = state.copy(situation = state.situation withVariant variant)
          val initialFen = parsed.tags.fen.map(_.value) flatMap {
            Forsyth.<<<@(variant, _)
          } map Forsyth.>> map FEN.apply

          val status = parsed.tags(_.Termination).map(_.toLowerCase) match {
            case Some("normal") | None => Status.Resign
            case Some("abandoned") => Status.Aborted
            case Some("time forfeit") => Status.Outoftime
            case Some("rules infraction") => Status.Cheat
            case Some(_) => Status.UnknownFinish
          }

          val date = parsed.tags.anyDate

          def name(whichName: TagPicker, whichRating: TagPicker): String = parsed.tags(whichName).fold("?") { n =>
            n + ~parsed.tags(whichRating).map(e => s" (${e take 8})")
          }

          val dbGame = Game.make(
            draughts = game,
            whitePlayer = Player.make(draughts.White, None) withName name(_.White, _.WhiteElo),
            blackPlayer = Player.make(draughts.Black, None) withName name(_.Black, _.BlackElo),
            mode = Mode.Casual,
            source = Source.Import,
            pdnImport = PdnImport.make(user = user, date = date, pdn = pdn).some
          ).sloppy.start |> { dbGame =>
            // apply the result from the board or the tags
            game.situation.status match {
              case Some(situationStatus) => dbGame.finish(situationStatus, game.situation.winner).game
              case None => parsed.tags.resultColor.map {
                case Some(color) => TagResult(status, color.some)
                case None if status == Status.Outoftime => TagResult(status, none)
                case None => TagResult(Status.Draw, none)
              }.filter(_.status > Status.Started).fold(dbGame) { res =>
                dbGame.finish(res.status, res.winner).game
              }
            }
          }

          Preprocessed(NewGame(dbGame), replay.copy(state = game), initialFen, parsed)
      }
  }
}
