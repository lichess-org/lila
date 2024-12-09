package lila.game

import chess.format.pgn.{ InitialComments, ParsedPgn, Parser, Pgn, PgnTree, SanStr, Tag, TagType, Tags }
import chess.format.{ Fen, pgn as chessPgn }
import chess.{ ByColor, Centis, Color, Outcome, Ply, Tree }
import chess.rating.IntRatingDiff

import lila.core.LightUser
import lila.core.config.BaseUrl
import lila.core.game.PgnDump.WithFlags
import lila.core.game.{ Game, Player }
import lila.game.GameExt.perfType
import lila.game.Player.nameSplit

final class PgnDump(baseUrl: BaseUrl, lightUserApi: lila.core.user.LightUserApiMinimal)(using Executor)
    extends lila.core.game.PgnDump:

  import PgnDump.*

  def apply(
      game: Game,
      initialFen: Option[Fen.Full],
      flags: WithFlags,
      teams: Option[ByColor[TeamId]] = None
  ): Fu[Pgn] =
    val imported = game.pgnImport.flatMap: pgni =>
      Parser.full(pgni.pgn).toOption

    val tagsFuture =
      if flags.tags then
        tags(
          game,
          initialFen,
          imported,
          withOpening = flags.opening,
          withRating = flags.rating,
          teams = teams
        )
      else fuccess(Tags(Nil))

    tagsFuture.map: ts =>
      val ply = ts.fen.flatMap(Fen.readWithMoveNumber).fold(Ply.initial)(_.ply)
      val tree = flags.moves.so:
        makeTree(
          applyDelay(game.sans, flags.keepDelayIf(game.playable)),
          flags.clocks.so(~game.bothClockStates),
          game.startColor
        )
      Pgn(ts, InitialComments.empty, tree, ply.next)

  private def gameUrl(id: GameId) = s"$baseUrl/$id"

  private def gameLightUsers(game: Game): Fu[ByColor[Option[LightUser]]] =
    game.players.traverse(_.userId.so(lightUserApi.async))

  private def rating(p: Player) = p.rating.orElse(p.nameSplit.flatMap(_._2)).fold("?")(_.toString)

  def player(p: Player, u: Option[LightUser]): String | UserName =
    p.aiLevel.fold(
      u.fold(p.nameSplit.map(_._1.value).orElse(p.name.map(_.value)) | UserName.anonymous)(_.name)
    )("lichess AI level " + _)

  private val customStartPosition: Set[chess.variant.Variant] =
    Set(chess.variant.Chess960, chess.variant.FromPosition, chess.variant.Horde, chess.variant.RacingKings)

  private def eventOf(game: Game) =
    val perf = game.perfType.nameKey
    game.tournamentId
      .map(id => s"${game.mode} $perf tournament https://lichess.org/tournament/$id")
      .orElse(game.simulId.map(id => s"$perf simul https://lichess.org/simul/$id"))
      .getOrElse(s"${game.mode} $perf game")

  private def ratingDiffTag(p: Player, tag: Tag.type => TagType) =
    p.ratingDiff.map(rd => Tag(tag(Tag), s"${if !rd.negative then "+" else ""}$rd"))

  def tags(
      game: Game,
      initialFen: Option[Fen.Full],
      imported: Option[ParsedPgn],
      withOpening: Boolean,
      withRating: Boolean,
      teams: Option[ByColor[TeamId]] = None
  ): Fu[Tags] =
    gameLightUsers(game).map:
      case ByColor(wu, bu) =>
        Tags:
          val importedDate = imported.flatMap(_.tags(_.Date))
          List[Option[Tag]](
            Tag(
              _.Event,
              imported.flatMap(_.tags(_.Event)) | {
                if game.sourceIs(_.Import) then "Import" else eventOf(game)
              }
            ).some,
            Tag(_.Site, imported.flatMap(_.tags(_.Site)) | gameUrl(game.id)).some,
            Tag(_.Date, importedDate | Tag.UTCDate.format.print(game.createdAt)).some,
            imported.flatMap(_.tags(_.Round)).map(Tag(_.Round, _)),
            Tag(_.White, player(game.whitePlayer, wu)).some,
            Tag(_.Black, player(game.blackPlayer, bu)).some,
            Tag(_.Result, result(game)).some,
            importedDate.isEmpty.option:
              Tag(_.UTCDate, imported.flatMap(_.tags(_.UTCDate)) | Tag.UTCDate.format.print(game.createdAt))
            ,
            importedDate.isEmpty.option:
              Tag(_.UTCTime, imported.flatMap(_.tags(_.UTCTime)) | Tag.UTCTime.format.print(game.createdAt))
            ,
            withRating.option(Tag(_.WhiteElo, rating(game.whitePlayer))),
            withRating.option(Tag(_.BlackElo, rating(game.blackPlayer))),
            withRating.so(ratingDiffTag(game.whitePlayer, _.WhiteRatingDiff)),
            withRating.so(ratingDiffTag(game.blackPlayer, _.BlackRatingDiff)),
            wu.flatMap(_.title).map(Tag(_.WhiteTitle, _)),
            bu.flatMap(_.title).map(Tag(_.BlackTitle, _)),
            teams.map(t => Tag("WhiteTeam", t.white)),
            teams.map(t => Tag("BlackTeam", t.black)),
            Tag(_.Variant, game.variant.name.capitalize).some,
            Tag.timeControl(game.clock.map(_.config)).some,
            Tag(_.ECO, game.opening.fold("?")(_.opening.eco)).some,
            withOpening.option(Tag(_.Opening, game.opening.fold("?")(_.opening.name))),
            Tag(
              _.Termination, {
                import chess.Status.*
                game.status match
                  case Created | Started                             => "Unterminated"
                  case Aborted | NoStart                             => "Abandoned"
                  case Timeout | Outoftime                           => "Time forfeit"
                  case Resign | Draw | Stalemate | Mate | VariantEnd => "Normal"
                  case Cheat                                         => "Rules infraction"
                  case UnknownFinish                                 => "Unknown"
              }
            ).some
          ).flatten ::: customStartPosition(game.variant)
            .so(initialFen)
            .so(fen => List(Tag(_.FEN, fen.value), Tag("SetUp", "1")))

object PgnDump:

  export lila.core.game.PgnDump.*

  private val delayMovesBy         = 3
  private val delayKeepsFirstMoves = 5

  private[game] def makeTree(
      moves: Seq[SanStr],
      clocks: Vector[Centis],
      startColor: Color
  ): Option[PgnTree] =
    val clockOffset = startColor.fold(0, 1)
    def f(san: SanStr, index: Int) = chessPgn.Move(
      san = san,
      secondsLeft = clocks.lift(index - clockOffset).map(_.roundSeconds)
    )
    Tree.buildWithIndex(moves, f)

  def applyDelay[M](moves: Seq[M], flags: WithFlags): Seq[M] =
    if !flags.delayMoves then moves
    else moves.take((moves.size - delayMovesBy).atLeast(delayKeepsFirstMoves))

  def result(game: Game) =
    Outcome.showResult(game.finished.option(Outcome(game.winnerColor)))
