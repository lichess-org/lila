package lila.game

import shogi.Replay
import shogi.format.kif.Kif
import shogi.format.csa.Csa
import shogi.format.usi.Usi
import shogi.format.{ Notation, NotationStep, Tag, Tags }
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
      flags: WithFlags,
      teams: Option[Color.Map[String]] = None
  ): Fu[Notation] = {
    val tagsFuture =
      if (flags.tags)
        tags(
          game,
          if (flags.csa && game.variant.standard)
            Tag.timeControlCsa(game.clock.map(_.config))
          else
            Tag.timeControlKif(game.clock.map(_.config)),
          teams = teams
        )
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
          game.usis,
          game.initialSfen,
          game.variant
        )
        makeMoveList(
          flags keepDelayIf game.playable applyDelay extendedMoves,
          game.shogi.startedAtStep,
          clockOffset,
          clocksSpent,
          clocksTotal
        )
      }
      val terminationMove =
        if (flags.csa && game.variant.standard)
          Csa.createTerminationStep(
            game.status,
            game.winnerColor.fold(false)(_ == game.turnColor),
            game.winnerColor
          )
        else if (game.drawn && game.variant.chushogi) "引き分け".some
        else
          Kif.createTerminationStep(game.status, game.winnerColor.fold(false)(_ == game.turnColor))
      val notation =
        if (flags.csa && game.variant.standard)
          Csa(moves, game.initialSfen, shogi.format.Initial.empty, ts)
        else Kif(moves, game.initialSfen, game.variant, shogi.format.Initial.empty, ts)
      if (game.finished) {
        terminationMove.fold(
          notation.updateLastPly(
            _.copy(comments = List(lila.game.StatusText(game.status, game.winnerColor, game.variant)))
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
    p.engineConfig.fold(u.fold(p.name | lila.user.User.anonymous)(_.name))(ec =>
      s"${ec.engine.fullName} level ${ec.level}"
    )

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
      timeControlTag: Tag,
      @scala.annotation.unused teams: Option[Color.Map[String]] = None
  ): Fu[Tags] =
    gameLightUsers(game) map { case (sente, gote) =>
      Tags {
        val imported = game.notationImport flatMap { _.parseNotation }

        List(
          Tag(_.Site, gameUrl(game.id)),
          Tag(_.Sente, player(game.sentePlayer, sente)),
          Tag(_.Gote, player(game.gotePlayer, gote)),
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
              timeControlTag
            )
          }
        }
      }
    }

  private def makeMoveList(
      extendedMoves: List[Usi.WithRole],
      startedAtStep: Int,
      clockOffset: Int,
      clocksSpent: Vector[Centis],
      clocksTotal: Vector[Centis]
  ): List[NotationStep] = extendedMoves.zipWithIndex.map { case (usiWithRole, index) =>
    NotationStep(
      stepNumber = index + startedAtStep,
      usiWithRole = usiWithRole,
      secondsSpent = clocksSpent lift (index - clockOffset) map (_.roundSeconds),
      secondsTotal = clocksTotal lift (index - clockOffset) map (_.roundSeconds)
    )
  }
}

object NotationDump {

  private val delayMovesBy         = 6
  private val delayKeepsFirstMoves = 10

  case class WithFlags(
      csa: Boolean = false,
      clocks: Boolean = true,
      moves: Boolean = true,
      tags: Boolean = true,
      evals: Boolean = true,
      literate: Boolean = false,
      shiftJis: Boolean = false,
      notationInJson: Boolean = false,
      delayMoves: Boolean = false
  ) {
    def applyDelay[M](moves: List[M]): List[M] =
      if (!delayMoves) moves
      else moves.take((moves.size - delayMovesBy) atLeast delayKeepsFirstMoves)

    def applyDelay[M](moves: Seq[M]): Seq[M] =
      if (!delayMoves) moves
      else moves.take((moves.size - delayMovesBy) atLeast delayKeepsFirstMoves)

    def keepDelayIf(cond: Boolean) = copy(delayMoves = delayMoves && cond)
  }

  def result(game: Game) =
    if (game.finished) Color.showResult(game.winnerColor)
    else "*"
}
