package lila.game

import shogi.Replay
import shogi.format.Forsyth
import shogi.format.kif.{ Kif, KifParser }
import shogi.format.csa.Csa
import shogi.format.{ FEN, Notation, NotationMove, ParsedNotation, Tag, TagType, Tags }
import shogi.{ Centis, Color }

import lila.common.config.BaseUrl
import lila.common.LightUser

import org.joda.time.DateTime

final class NotationDump(
    baseUrl: BaseUrl,
    lightUserApi: lila.user.LightUserApi
)(implicit ec: scala.concurrent.ExecutionContext) {

  import NotationDump._

  def apply(
      game: Game,
      initialFen: Option[FEN],
      flags: WithFlags,
      teams: Option[Color.Map[String]] = None
  ): Fu[Notation] = {
    val imported = game.pgnImport.flatMap { kifi =>
      KifParser.full(kifi.kif).toOption
    }
    val tagsFuture =
      if (flags.tags)
        tags(game, initialFen, imported, withOpening = flags.opening, csa = flags.csa, teams = teams)
      else fuccess(Tags(Nil))
    tagsFuture map { ts =>
      val moves = flags.moves ?? {
        val clocksSpent: Vector[Centis] = (flags.clocks && !game.isCorrespondence) ?? {
          val movetimes = ~game.moveTimes
          if (movetimes.forall(_ == Centis(0))) Vector[Centis]() else movetimes
        }
        val clocksTotal = clocksSpent.foldLeft(Vector[Centis]())((acc, cur) =>
          acc :+ (acc.takeRight(2).headOption.getOrElse(Centis(0)) + cur)
        )
        val clockOffset = game.startColor.fold(0, 1)
        val extendedMoves = Replay.gameMoveWhileValid(
          game.pgnMoves,
          initialFen.map(_.value) | game.variant.initialFen,
          game.variant
        ) match {
          case (_, games, _) =>
            games map { case (_, uciSan) =>
              (uciSan.san, uciSan.uci)
            }
        }
        extendedMoves.zipWithIndex.map { case ((san, uci), index) =>
          NotationMove(
            ply = index + 1,
            san = san,
            uci = uci,
            secondsSpent = clocksSpent lift (index - clockOffset) map (_.roundSeconds),
            secondsTotal = clocksTotal lift (index - clockOffset) map (_.roundSeconds)
          )
        }
      }
      val terminationMove =
        if (flags.csa)
          Csa.createTerminationMove(
            game.status,
            game.winnerColor.fold(false)(_ == game.turnColor),
            game.winnerColor
          )
        else
          Kif.createTerminationMove(game.status, game.winnerColor.fold(false)(_ == game.turnColor))
      val notation = if (flags.csa) Csa(ts, moves) else Kif(ts, moves)
      terminationMove.fold(
        notation.updateLastPly(
          _.copy(comments = List(lila.game.StatusText(game.status, game.winnerColor, shogi.variant.Standard)))
        )
      ) { t =>
        notation.updateLastPly(_.copy(result = t.some))
      }
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

  def dateAndTime(dateTime: DateTime): String =
    s"${Tag.UTCDate.format.print(dateTime)} ${Tag.UTCTime.format.print(dateTime)}"

  def tags(
      game: Game,
      initialFen: Option[FEN],
      imported: Option[ParsedNotation],
      withOpening: Boolean,
      csa: Boolean,
      teams: Option[Color.Map[String]] = None
  ): Fu[Tags] =
    gameLightUsers(game) map { case (wu, bu) =>
      Tags {
        // ugly
        val importedOrNormal: List[Option[Tag]] = imported map { p: ParsedNotation =>
          List(
            Tag(_.Event, imported.flatMap(_.tags(_.Event)) | "Import").some,
            p.tags.value.find(_.name == Tag.Start),
            p.tags.value.find(_.name == Tag.End),
            p.tags.value.find(_.name == Tag.TimeControl),
            p.tags.value.find(_.name == Tag.Byoyomi)
          )
        } getOrElse {
          List(
            Tag(_.Event, eventOf(game)).some,
            Tag(_.Start, dateAndTime(game.createdAt)).some,
            Tag(_.End, dateAndTime(game.movedAt)).some,
            if (csa)
              Tag.timeControlCsa(game.clock.map(_.config)).some
            else
              Tag.timeControlKif(game.clock.map(_.config)).some
          )
        }
        (List[Option[Tag]](
          Tag(_.Site, gameUrl(game.id)).some,
          Tag(_.Sente, player(game.sentePlayer, wu)).some,
          Tag(_.Gote, player(game.gotePlayer, bu)).some,
          teams.map { t => Tag("SenteTeam", t.sente) },
          teams.map { t => Tag("GoteTeam", t.gote) },
          Tag(_.Variant, game.variant.name.capitalize).some,
          withOpening option Tag(_.Opening, game.opening.fold("?")(_.opening.eco))
        ) ::: importedOrNormal).flatten ::: customStartPosition(game.variant).??(
          List(
            Tag(_.FEN, initialFen.fold(Forsyth.initial)(_.value))
          )
        )
      }
    }
}

object NotationDump {

  case class WithFlags(
      csa: Boolean = false,
      clocks: Boolean = true,
      moves: Boolean = true,
      tags: Boolean = true,
      evals: Boolean = true,
      opening: Boolean = true,
      literate: Boolean = false,
      notationInJson: Boolean = false,
      delayMoves: Int = 0
  )

  def result(game: Game) =
    if (game.finished) Color.showResult(game.winnerColor)
    else "*"
}
