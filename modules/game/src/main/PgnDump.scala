package lila.game

import shogi.format.Forsyth
import shogi.format.pgn.{ ParsedPgn, KifParser, Pgn, Tag, TagType, Tags }
import shogi.format.{ FEN, pgn => shogiPgn }
import shogi.{ Centis, Color }

import lila.common.config.BaseUrl
import lila.common.LightUser

final class PgnDump(
    baseUrl: BaseUrl,
    lightUserApi: lila.user.LightUserApi
)(implicit ec: scala.concurrent.ExecutionContext) {

  import PgnDump._

  def apply(
      game: Game,
      initialFen: Option[FEN],
      flags: WithFlags,
      teams: Option[Color.Map[String]] = None
  ): Fu[Pgn] = {
    val imported = game.pgnImport.flatMap { pgni =>
      KifParser.full(pgni.pgn).toOption
    }
    val tagsFuture =
      if (flags.tags) tags(game, initialFen, imported, withOpening = flags.opening, teams = teams)
      else fuccess(Tags(Nil))
    tagsFuture map { ts =>
      val turns = flags.moves ?? {
        val fenSituation = ts.fen.map(_.value) flatMap Forsyth.<<<
        val moves2 =
          if (fenSituation.exists(_.situation.color.gote)) ".." +: game.pgnMoves
          else game.pgnMoves
        val moves3 =
          if (flags.delayMoves > 0) moves2 dropRight flags.delayMoves
          else moves2
        makeTurns(
          moves3,
          fenSituation.map(_.fullMoveNumber) | 1,
          flags.clocks ?? ~game.bothClockStates,
          game.startColor
        )
      }
      Pgn(ts, turns)
    }
  }

  private def gameUrl(id: String) = s"$baseUrl/$id"

  private def gameLightUsers(game: Game): Fu[(Option[LightUser], Option[LightUser])] =
    (game.sentePlayer.userId ?? lightUserApi.async) zip (game.gotePlayer.userId ?? lightUserApi.async)

  private def rating(p: Player) = p.rating.fold("?")(_.toString)

  def player(p: Player, u: Option[LightUser]) =
    p.aiLevel.fold(u.fold(p.name | lila.user.User.anonymous)(_.name))("lishogi AI level " + _)

  private val customStartPosition: Set[shogi.variant.Variant] =
    Set(shogi.variant.FromPosition)

  private def eventOf(game: Game) = {
    val perf = game.perfType.fold("Standard")(_.trans(lila.i18n.defaultLang))
    game.tournamentId.map { id =>
      s"${game.mode} $perf tournament https://lishogi.org/tournament/$id"
    } orElse game.simulId.map { id =>
      s"$perf simul https://lishogi.org/simul/$id"
    } getOrElse {
      s"${game.mode} $perf game"
    }
  }

  private def ratingDiffTag(p: Player, tag: Tag.type => TagType) =
    p.ratingDiff.map { rd =>
      Tag(tag(Tag), s"${if (rd >= 0) "+" else ""}$rd")
    }

  def tags(
      game: Game,
      initialFen: Option[FEN],
      imported: Option[ParsedPgn],
      withOpening: Boolean,
      teams: Option[Color.Map[String]] = None
  ): Fu[Tags] =
    gameLightUsers(game) map { case (wu, bu) =>
      Tags {
        val importedDate = imported.flatMap(_.tags(_.Date))
        List[Option[Tag]](
          Tag(
            _.Event,
            imported.flatMap(_.tags(_.Event)) | { if (game.imported) "Import" else eventOf(game) }
          ).some,
          Tag(_.Site, gameUrl(game.id)).some,
          Tag(_.Date, importedDate | Tag.UTCDate.format.print(game.createdAt)).some,
          imported.flatMap(_.tags(_.Round)).map(Tag(_.Round, _)),
          Tag(_.Sente, player(game.sentePlayer, wu)).some,
          Tag(_.Gote, player(game.gotePlayer, bu)).some,
          Tag(_.Result, result(game)).some,
          importedDate.isEmpty option Tag(
            _.UTCDate,
            imported.flatMap(_.tags(_.UTCDate)) | Tag.UTCDate.format.print(game.createdAt)
          ),
          importedDate.isEmpty option Tag(
            _.UTCTime,
            imported.flatMap(_.tags(_.UTCTime)) | Tag.UTCTime.format.print(game.createdAt)
          ),
          Tag(_.SenteElo, rating(game.sentePlayer)).some,
          Tag(_.GoteElo, rating(game.gotePlayer)).some,
          ratingDiffTag(game.sentePlayer, _.SenteRatingDiff),
          ratingDiffTag(game.gotePlayer, _.GoteRatingDiff),
          wu.flatMap(_.title).map { t =>
            Tag(_.SenteTitle, t)
          },
          bu.flatMap(_.title).map { t =>
            Tag(_.GoteTitle, t)
          },
          teams.map { t => Tag("SenteTeam", t.sente) },
          teams.map { t => Tag("GoteTeam", t.gote) },
          Tag(_.Variant, game.variant.name.capitalize).some,
          Tag.timeControl(game.clock.map(_.config)).some,
          Tag(_.ECO, game.opening.fold("?")(_.opening.eco)).some,
          withOpening option Tag(_.Opening, game.opening.fold("?")(_.opening.name)),
          Tag(
            _.Termination, {
              import shogi.Status._
              game.status match {
                case Created | Started   => "Unterminated"
                case Aborted | NoStart   => "Abandoned"
                case Timeout | Outoftime => "Time forfeit"
                case Resign |
                  Draw |
                  Stalemate |
                  Mate |
                  VariantEnd |
                  TryRule |
                  PerpetualCheck |
                  Impasse27         => "Normal"
                case Cheat              => "Rules infraction"
                case UnknownFinish      => "Unknown"
              }
            }
          ).some
        ).flatten ::: customStartPosition(game.variant).??(
          List(
            Tag(_.FEN, initialFen.fold(Forsyth.initial)(_.value)),
            Tag("SetUp", "1")
          )
        )
      }
    }

  private def makeTurns(
      moves: Seq[String],
      from: Int,
      clocks: Vector[Centis],
      startColor: Color
  ): List[shogiPgn.Turn] =
    (moves grouped 2).zipWithIndex.toList map { case (moves, index) =>
      val clockOffset = startColor.fold(0, 1)
      shogiPgn.Turn(
        number = index + from,
        sente = moves.headOption filter (".." !=) map { san =>
          shogiPgn.Move(
            san = san,
            secondsLeft = clocks lift (index * 2 - clockOffset) map (_.roundSeconds)
          )
        },
        gote = moves lift 1 map { san =>
          shogiPgn.Move(
            san = san,
            secondsLeft = clocks lift (index * 2 + 1 - clockOffset) map (_.roundSeconds)
          )
        }
      )
    } filterNot (_.isEmpty)
}

object PgnDump {

  case class WithFlags(
      clocks: Boolean = true,
      moves: Boolean = true,
      tags: Boolean = true,
      evals: Boolean = true,
      opening: Boolean = true,
      literate: Boolean = false,
      pgnInJson: Boolean = false,
      delayMoves: Int = 0
  )

  def result(game: Game) =
    if (game.finished) Color.showResult(game.winnerColor)
    else "*"
}
