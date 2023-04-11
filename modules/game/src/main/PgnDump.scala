package lila.game

import chess.format.Fen
import chess.format.pgn.{ ParsedPgn, Parser, Pgn, Tag, TagType, Tags, SanStr }
import chess.format.{ pgn as chessPgn }
import chess.{ Centis, Color, ByColor, Outcome }

import lila.common.config.BaseUrl
import lila.common.LightUser

final class PgnDump(
    baseUrl: BaseUrl,
    lightUserApi: lila.user.LightUserApi
)(using Executor):

  import PgnDump.*

  def apply(
      game: Game,
      initialFen: Option[Fen.Epd],
      flags: WithFlags,
      teams: Option[ByColor[TeamId]] = None
  ): Fu[Pgn] =
    val imported = game.pgnImport.flatMap { pgni =>
      Parser.full(pgni.pgn).toOption
    }
    val tagsFuture =
      if (flags.tags)
        tags(
          game,
          initialFen,
          imported,
          withOpening = flags.opening,
          withRating = flags.rating,
          teams = teams
        )
      else fuccess(Tags(Nil))
    tagsFuture map { ts =>
      val turns = flags.moves ?? {
        val fenSituation = ts.fen flatMap Fen.readWithMoveNumber
        makeTurns(
          flags keepDelayIf game.playable applyDelay {
            if (fenSituation.exists(_.situation.color.black)) SanStr("..") +: game.sans
            else game.sans
          },
          fenSituation.fold(1)(_.fullMoveNumber.value),
          flags.clocks ?? ~game.bothClockStates,
          game.startColor
        )
      }
      Pgn(ts, turns)
    }

  private def gameUrl(id: GameId) = s"$baseUrl/$id"

  private def gameLightUsers(game: Game): Fu[(Option[LightUser], Option[LightUser])] =
    (game.whitePlayer.userId ?? lightUserApi.async) zip (game.blackPlayer.userId ?? lightUserApi.async)

  private def rating(p: Player) = p.rating.orElse(p.nameSplit.flatMap(_._2)).fold("?")(_.toString)

  def player(p: Player, u: Option[LightUser]) =
    p.aiLevel.fold(u.fold(p.nameSplit.map(_._1).orElse(p.name) | lila.user.User.anonymous)(_.name))(
      "lichess AI level " + _
    )

  private val customStartPosition: Set[chess.variant.Variant] =
    Set(chess.variant.Chess960, chess.variant.FromPosition, chess.variant.Horde, chess.variant.RacingKings)

  private def eventOf(game: Game) =
    val perf = game.perfType.fold("Standard")(_.trans(using lila.i18n.defaultLang))
    game.tournamentId.map { id =>
      s"${game.mode} $perf tournament https://lichess.org/tournament/$id"
    } orElse game.simulId.map { id =>
      s"$perf simul https://lichess.org/simul/$id"
    } getOrElse {
      s"${game.mode} $perf game"
    }

  private def ratingDiffTag(p: Player, tag: Tag.type => TagType) =
    p.ratingDiff.map { rd =>
      Tag(tag(Tag), s"${if (rd >= 0) "+" else ""}$rd")
    }

  def tags(
      game: Game,
      initialFen: Option[Fen.Epd],
      imported: Option[ParsedPgn],
      withOpening: Boolean,
      withRating: Boolean,
      teams: Option[ByColor[TeamId]] = None
  ): Fu[Tags] =
    gameLightUsers(game) map { case (wu, bu) =>
      Tags {
        val importedDate = imported.flatMap(_.tags(_.Date))
        List[Option[Tag]](
          Tag(
            _.Event,
            imported.flatMap(_.tags(_.Event)) | { if (game.imported) "Import" else eventOf(game) }
          ).some,
          Tag(_.Site, imported.flatMap(_.tags(_.Site)) | gameUrl(game.id)).some,
          Tag(_.Date, importedDate | Tag.UTCDate.format.print(game.createdAt)).some,
          imported.flatMap(_.tags(_.Round)).map(Tag(_.Round, _)),
          Tag(_.White, player(game.whitePlayer, wu)).some,
          Tag(_.Black, player(game.blackPlayer, bu)).some,
          Tag(_.Result, result(game)).some,
          importedDate.isEmpty option Tag(
            _.UTCDate,
            imported.flatMap(_.tags(_.UTCDate)) | Tag.UTCDate.format.print(game.createdAt)
          ),
          importedDate.isEmpty option Tag(
            _.UTCTime,
            imported.flatMap(_.tags(_.UTCTime)) | Tag.UTCTime.format.print(game.createdAt)
          ),
          withRating option Tag(_.WhiteElo, rating(game.whitePlayer)),
          withRating option Tag(_.BlackElo, rating(game.blackPlayer)),
          withRating ?? ratingDiffTag(game.whitePlayer, _.WhiteRatingDiff),
          withRating ?? ratingDiffTag(game.blackPlayer, _.BlackRatingDiff),
          wu.flatMap(_.title).map { t =>
            Tag(_.WhiteTitle, t)
          },
          bu.flatMap(_.title).map { t =>
            Tag(_.BlackTitle, t)
          },
          teams.map { t => Tag("WhiteTeam", t.white) },
          teams.map { t => Tag("BlackTeam", t.black) },
          Tag(_.Variant, game.variant.name.capitalize).some,
          Tag.timeControl(game.clock.map(_.config)).some,
          Tag(_.ECO, game.opening.fold("?")(_.opening.eco)).some,
          withOpening option Tag(_.Opening, game.opening.fold("?")(_.opening.name)),
          Tag(
            _.Termination, {
              import chess.Status.*
              game.status match {
                case Created | Started                             => "Unterminated"
                case Aborted | NoStart                             => "Abandoned"
                case Timeout | Outoftime                           => "Time forfeit"
                case Resign | Draw | Stalemate | Mate | VariantEnd => "Normal"
                case Cheat                                         => "Rules infraction"
                case UnknownFinish                                 => "Unknown"
              }
            }
          ).some
        ).flatten ::: customStartPosition(game.variant).??(
          initialFen.??(fen =>
            List(
              Tag(_.FEN, fen.value),
              Tag("SetUp", "1")
            )
          )
        )
      }
    }

  private def makeTurns(
      moves: Seq[SanStr],
      from: Int,
      clocks: Vector[Centis],
      startColor: Color
  ): List[chessPgn.Turn] =
    (moves grouped 2).zipWithIndex.toList map { case (moves, index) =>
      val clockOffset = startColor.fold(0, 1)
      chessPgn.Turn(
        number = index + from,
        white = moves.headOption.filter(SanStr("..") != _) map { san =>
          chessPgn.Move(
            san = san,
            secondsLeft = clocks lift (index * 2 - clockOffset) map (_.roundSeconds)
          )
        },
        black = moves lift 1 map { san =>
          chessPgn.Move(
            san = san,
            secondsLeft = clocks lift (index * 2 + 1 - clockOffset) map (_.roundSeconds)
          )
        }
      )
    } filterNot (_.isEmpty)

object PgnDump:

  private val delayMovesBy         = 3
  private val delayKeepsFirstMoves = 5

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
      accuracy: Boolean = false
  ):
    def applyDelay[M](moves: Seq[M]): Seq[M] =
      if !delayMoves then moves
      else moves.take((moves.size - delayMovesBy) atLeast delayKeepsFirstMoves)

    def keepDelayIf(cond: Boolean) = copy(delayMoves = delayMoves && cond)

    def requiresAnalysis = evals || accuracy

  def result(game: Game) =
    Outcome.showResult(game.finished option Outcome(game.winnerColor))
