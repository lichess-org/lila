package lila.game

import chess.format.pgn.{ InitialComments, ParsedPgn, Parser, Pgn, PgnTree, SanStr, Tag, TagType, Tags }
import chess.format.{ Fen, pgn as chessPgn }
import chess.{ ByColor, Centis, Color, Outcome, Ply, Tree }

import lila.core.LightUser
import lila.core.config.BaseUrl
import lila.core.i18n.Translate

final class PgnDump(baseUrl: BaseUrl, lightUserApi: lila.core.user.LightUserApiMinimal)(using Executor):

  import PgnDump.*

  def apply(
      game: Game,
      initialFen: Option[Fen.Full],
      flags: WithFlags,
      teams: Option[ByColor[TeamId]] = None
  )(using Translate): Fu[Pgn] =
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
      val tree = flags.moves.so:
        val fenSituation = ts.fen.flatMap(Fen.readWithMoveNumber)
        makeTree(
          flags.keepDelayIf(game.playable).applyDelay(game.sans),
          fenSituation.fold(Ply.initial)(_.ply),
          flags.clocks.so(~game.bothClockStates),
          game.startColor
        )
      Pgn(ts, InitialComments.empty, tree)

  private def gameUrl(id: GameId) = s"$baseUrl/$id"

  private def gameLightUsers(game: Game): Fu[ByColor[Option[LightUser]]] =
    game.players.traverse(_.userId.so(lightUserApi.async))

  private def rating(p: Player) = p.rating.orElse(p.nameSplit.flatMap(_._2)).fold("?")(_.toString)

  def player(p: Player, u: Option[LightUser]): String | UserName =
    p.aiLevel.fold(
      u.fold(p.nameSplit.map(_._1.value).orElse(p.name.map(_.value)) | lila.core.UserName.anonymous)(_.name)
    )("lichess AI level " + _)

  private val customStartPosition: Set[chess.variant.Variant] =
    Set(chess.variant.Chess960, chess.variant.FromPosition, chess.variant.Horde, chess.variant.RacingKings)

  private def eventOf(game: Game)(using lila.core.i18n.Translate) =
    val perf = game.perfType.nameKey
    game.tournamentId
      .map(id => s"${game.mode} $perf tournament https://lichess.org/tournament/$id")
      .orElse(game.simulId.map(id => s"$perf simul https://lichess.org/simul/$id"))
      .getOrElse(s"${game.mode} $perf game")

  private def ratingDiffTag(p: Player, tag: Tag.type => TagType) =
    p.ratingDiff.map(rd => Tag(tag(Tag), s"${if rd >= 0 then "+" else ""}$rd"))

  def tags(
      game: Game,
      initialFen: Option[Fen.Full],
      imported: Option[ParsedPgn],
      withOpening: Boolean,
      withRating: Boolean,
      teams: Option[ByColor[TeamId]] = None
  )(using lila.core.i18n.Translate): Fu[Tags] =
    gameLightUsers(game).map:
      case ByColor(wu, bu) =>
        Tags:
          val importedDate = imported.flatMap(_.tags(_.Date))
          List[Option[Tag]](
            Tag(
              _.Event,
              imported.flatMap(_.tags(_.Event)) | { if game.imported then "Import" else eventOf(game) }
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

  private val delayMovesBy         = 3
  private val delayKeepsFirstMoves = 5

  private[game] def makeTree(
      moves: Seq[SanStr],
      from: Ply,
      clocks: Vector[Centis],
      startColor: Color
  ): Option[PgnTree] =
    val clockOffset = startColor.fold(0, 1)
    def f(san: SanStr, index: Int) = chessPgn.Move(
      ply = from + index + 1,
      san = san,
      secondsLeft = clocks.lift(index - clockOffset).map(_.roundSeconds)
    )
    Tree.buildWithIndex(moves, f)

  case class WithFlags(
      clocks: Boolean = true,
      moves: Boolean = true,
      tags: Boolean = true,
      evals: Boolean = true,
      opening: Boolean = true,
      rating: Boolean = true,
      literate: Boolean = false,
      pgnInJson: Boolean = false,
      delayMoves: Boolean = false,
      lastFen: Boolean = false,
      accuracy: Boolean = false,
      division: Boolean = false
  ):
    def applyDelay[M](moves: Seq[M]): Seq[M] =
      if !delayMoves then moves
      else moves.take((moves.size - delayMovesBy).atLeast(delayKeepsFirstMoves))

    def keepDelayIf(cond: Boolean) = copy(delayMoves = delayMoves && cond)

    def requiresAnalysis = evals || accuracy

  def result(game: Game) =
    Outcome.showResult(game.finished.option(Outcome(game.winnerColor)))
