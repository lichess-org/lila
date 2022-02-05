package lila.game

import shogi.Replay
import shogi.format.forsyth.Sfen
import shogi.format.kif.Kif
import shogi.format.csa.Csa
import shogi.format.{ Notation, NotationMove, Tag, Tags }
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
      initialSfen: Option[Sfen],
      flags: WithFlags,
      teams: Option[Color.Map[String]] = None
  ): Fu[Notation] = {
    val tagsFuture =
      if (flags.tags)
        tags(game, initialSfen, withOpening = flags.opening, csa = flags.csa, teams = teams)
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
        val extendedMoves = Replay.usiWithRoleWhilePossible(
          game.usiMoves,
          initialSfen,
          game.variant
        )
        extendedMoves.zipWithIndex.map { case (usiWithRole, index) =>
          NotationMove(
            moveNumber = index + 1 + game.shogi.startedAtMove,
            usiWithRole = usiWithRole,
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
      if (game.finished) {
        terminationMove.fold(
          notation.updateLastPly(
            _.copy(comments =
              List(lila.game.StatusText(game.status, game.winnerColor, shogi.variant.Standard))
            )
          )
        ) { t =>
          notation.updateLastPly(_.copy(result = t.some))
        }
      } else notation
    }
  }

  private def gameUrl(id: String) = s"$baseUrl/$id"

  private def gameLightUsers(game: Game): Fu[(Option[LightUser], Option[LightUser])] =
    (game.sentePlayer.userId ?? lightUserApi.async) zip (game.gotePlayer.userId ?? lightUserApi.async)

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
      initialSfen: Option[Sfen],
      withOpening: Boolean,
      csa: Boolean,
      teams: Option[Color.Map[String]] = None
  ): Fu[Tags] =
    gameLightUsers(game) map { case (wu, bu) =>
      Tags {
        val imported = game.notationImport flatMap { _.parseNotation }

        List(
          Tag(_.Site, gameUrl(game.id)),
          Tag(_.Sente, player(game.sentePlayer, wu)),
          Tag(_.Gote, player(game.gotePlayer, bu)),
          Tag(_.Variant, game.variant.name.capitalize),
          Tag(
            _.Event,
            imported.flatMap(_.tags(_.Event)) | { if (game.imported) "Import" else eventOf(game) }
          )
        ) ::: {
          imported map { p =>
            List(
              p.tags.value.find(_.name == Tag.Start),
              p.tags.value.find(_.name == Tag.End),
              p.tags.value.find(_.name == Tag.TimeControl),
              p.tags.value.find(_.name == Tag.Byoyomi), // not used in CSA
              p.tags.value.find(_.name == Tag.Opening)
            ).flatten
          } getOrElse {
            List(
              Tag(_.Start, dateAndTime(game.createdAt)),
              Tag(_.End, dateAndTime(game.movedAt)),
              if (csa)
                Tag.timeControlCsa(game.clock.map(_.config))
              else
                Tag.timeControlKif(game.clock.map(_.config))
            )
          }
        } ::: customStartPosition(game.variant).??(
          List(
            Tag(_.Sfen, (initialSfen | game.variant.initialSfen).value)
          )
        ) ::: (withOpening && game.opening.isDefined) ?? (
          List(
            Tag(_.Opening, game.opening.fold("")(_.opening.japanese))
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
