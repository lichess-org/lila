package lila.game

import shogi.Replay
import shogi.format.Forsyth
import shogi.format.pgn.{ KifParser, ParsedPgn, Pgn, Tag, TagType, Tags, Kif }
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
  ): Fu[Kif] = {
    val imported = game.pgnImport.flatMap { kifi =>
      KifParser.full(kifi.kif).toOption
    }
    val tagsFuture =
      if (flags.tags) tags(game, initialFen, imported, withOpening = flags.opening, teams = teams)
      else fuccess(Tags(Nil))
    tagsFuture map { ts =>
      val moves = flags.moves ?? {
        val clocksSpent: Vector[Centis] = (flags.clocks && !game.isCorrespondence) ?? {
          val movetimes = ~game.moveTimes
          if(movetimes.forall(_==Centis(0))) Vector[Centis]() else movetimes
        } 
        val clocksTotal = clocksSpent.foldLeft(Vector[Centis]())((acc, cur) => acc :+ (acc.takeRight(2).headOption.getOrElse(Centis(0)) + cur))
        val clockOffset = game.startColor.fold(0, 1)
        val extendedMoves = Replay.gameMoveWhileValid(
          game.pgnMoves,
          initialFen.map(_.value) | game.variant.initialFen,
          game.variant
        ) match {
          case (_, games, _) =>
            games map { case (_, uciSan) =>
              (uciSan.san, uciSan.uci.uci)
            }
        }
        extendedMoves.zipWithIndex.map { case ((san, uci), index) =>
          shogiPgn.KifMove(
            ply = index + 1,
            san = san,
            uci = uci,
            secondsSpent = clocksSpent lift (index - clockOffset) map (_.roundSeconds),
            secondsTotal = clocksTotal lift (index - clockOffset) map (_.roundSeconds)
          )
        }
      }
      Kif(ts, moves)
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
          // For now we don't display the game termination
          Tag(
            _.Termination, {
              import shogi.Status._
              game.status match {
                case Created | Started   => ""
                case Aborted | NoStart   => "中断"
                case Timeout | Outoftime => "切れ負け"
                case Resign => "" // "投了", how to display resigning during opponent's turn?
                case Mate | Stalemate => "詰み"
                case Draw           => "千日手"
                case TryRule        => "トライルール"
                case Impasse27      => "入玉勝ち"
                case PerpetualCheck => "反則勝ち"
                case Cheat          => "反則"
                case UnknownFinish | VariantEnd | _  => ""
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
