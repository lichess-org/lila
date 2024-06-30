package lila.round

import shogi.format.usi.Usi
import shogi.{ Centis, LagMetrics, Status }

import actorApi.round.{ DrawNo, ForecastPlay, HumanPlay, PauseNo, TakebackNo, TooManyPlies }
import lila.game.actorApi.{ MoveGameEvent, PauseGame }
import lila.common.Bus
import lila.game.{ Game, GameRepo, Pov, Progress }
import lila.game.Game.PlayerId
import cats.data.Validated

final private class Player(
    fishnetPlayer: lila.fishnet.Player,
    gameRepo: GameRepo,
    finisher: Finisher,
    scheduleExpiration: ScheduleExpiration
)(implicit ec: scala.concurrent.ExecutionContext) {

  sealed private trait UsiResult
  private case object Flagged                       extends UsiResult
  private case class UsiApplied(progress: Progress) extends UsiResult

  private[round] def human(play: HumanPlay, round: RoundDuct)(
      pov: Pov
  )(implicit proxy: GameProxy): Fu[Events] =
    play match {
      case HumanPlay(_, usi, blur, lag, _) =>
        pov match {
          case Pov(game, _) if game.playedPlies > Game.maxPlies(game.variant) =>
            round ! TooManyPlies
            fuccess(Nil)
          case Pov(game, color) if game playableBy color =>
            applyUsi(game, usi, blur, lag)
              .leftMap(e => s"$pov $e")
              .fold(errs => fufail(ClientError(errs.toString)), fuccess)
              .flatMap {
                case Flagged => finisher.outOfTime(game)
                case UsiApplied(progress) =>
                  proxy.save(progress) >>
                    postHumanOrBotPlay(round, pov, progress, usi)
              }
          case Pov(game, _) if game.finished           => fufail(ClientError(s"$pov game is finished"))
          case Pov(game, _) if game.paused             => fufail(ClientError(s"$pov game is paused"))
          case Pov(game, _) if game.aborted            => fufail(ClientError(s"$pov game is aborted"))
          case Pov(game, color) if !game.turnOf(color) => fufail(ClientError(s"$pov not your turn"))
          case _ => fufail(ClientError(s"$pov move refused for some reason"))
        }
    }

  private[round] def bot(usi: Usi, round: RoundDuct)(pov: Pov)(implicit proxy: GameProxy): Fu[Events] =
    pov match {
      case Pov(game, _) if game.playedPlies > Game.maxPlies(game.variant) =>
        round ! TooManyPlies
        fuccess(Nil)
      case Pov(game, color) if game playableBy color =>
        applyUsi(game, usi, false, botLag)
          .fold(errs => fufail(ClientError(errs.toString)), fuccess)
          .flatMap {
            case Flagged => finisher.outOfTime(game)
            case UsiApplied(progress) =>
              proxy.save(progress) >> postHumanOrBotPlay(round, pov, progress, usi)
          }
      case Pov(game, _) if game.finished           => fufail(ClientError(s"$pov game is finished"))
      case Pov(game, _) if game.aborted            => fufail(ClientError(s"$pov game is aborted"))
      case Pov(game, color) if !game.turnOf(color) => fufail(ClientError(s"$pov not your turn"))
      case _ => fufail(ClientError(s"$pov move refused for some reason"))
    }

  private def postHumanOrBotPlay(
      round: RoundDuct,
      pov: Pov,
      progress: Progress,
      usi: Usi
  )(implicit proxy: GameProxy): Fu[Events] = {
    if (progress.game.paused) notifyOfPausedGame(usi, progress.game)
    else notifyUsi(usi, progress.game)

    if (progress.game.finished) usiFinish(progress.game) dmap { progress.events ::: _ }
    else {
      if (progress.game.playableByAi) requestFishnet(progress.game, round)
      if (pov.opponent.isOfferingDraw) round ! DrawNo(PlayerId(pov.player.id))
      if (pov.player.isProposingTakeback) round ! TakebackNo(PlayerId(pov.player.id))
      if (pov.player.isOfferingPause) round ! PauseNo(PlayerId(pov.player.id))
      if (progress.game.forecastable) round ! ForecastPlay(usi)
      scheduleExpiration(progress.game)
      fuccess(progress.events)
    }
  }

  private[round] def fishnet(game: Game, ply: Int, usi: Usi)(implicit proxy: GameProxy): Fu[Events] = {
    if (game.playable && game.player.isAi && game.plies == ply) {
      applyUsi(game, usi, blur = false, metrics = fishnetLag)
        .fold(errs => fufail(ClientError(errs.toString)), fuccess)
        .flatMap {
          case Flagged => finisher.outOfTime(game)
          case UsiApplied(progress) =>
            proxy.save(progress) >>-
              notifyUsi(usi, progress.game) >> {
                if (progress.game.finished) usiFinish(progress.game) dmap { progress.events ::: _ }
                else
                  fuccess(progress.events)
              }
        }
    } else
      fufail(
        FishnetError(
          s"Not AI turn move: ${usi} id: ${game.id} playable: ${game.playable} player: ${game.player} plies: ${game.playedPlies}, $ply"
        )
      )
  }

  private[round] def requestFishnet(game: Game, round: RoundDuct): Funit =
    game.playableByAi ?? {
      if (game.playedPlies <= fishnetPlayer.maxPlies) fishnetPlayer(game)
      else fuccess(round ! actorApi.round.ResignAi)
    }

  private val fishnetLag = LagMetrics(clientLag = Centis(5).some)
  private val botLag     = LagMetrics(clientLag = Centis(10).some)

  private def applyUsi(
      game: Game,
      usi: Usi,
      blur: Boolean,
      metrics: LagMetrics
  ): Validated[String, UsiResult] =
    game.shogi(usi, metrics) map { nsg =>
      if (nsg.clock.exists(_.outOfTime(game.turnColor, withGrace = false))) Flagged
      else if (game.prePaused && !nsg.situation.end(withImpasse = true))
        UsiApplied(game.pauseAndSealUsi(usi, nsg, blur))
      else UsiApplied(game.applyGame(nsg, blur))
    }

  private def notifyOfPausedGame(usi: Usi, game: Game): Funit = {
    gameRepo.pause(game.id, usi).void >>- Bus.publish(PauseGame(game), "pauseGame")
  }

  private[round] def notifyUsi(usi: Usi, game: Game): Unit = {
    import lila.hub.actorApi.round.{ CorresMoveEvent, MoveEvent, SimulMoveEvent }
    val color = !game.situation.color
    val moveEvent = MoveEvent(
      gameId = game.id,
      sfen = game.situation.toSfen.value,
      usi = usi.usi
    )

    // I checked and the bus doesn't do much if there's no subscriber for a classifier,
    // so we should be good here.
    // also used for targeted TvBroadcast subscription
    Bus.publish(MoveGameEvent(game, moveEvent.sfen, moveEvent.usi), MoveGameEvent makeChan game.id)

    // publish correspondence moves
    if (game.isCorrespondence && game.nonAi)
      Bus.publish(
        CorresMoveEvent(
          move = moveEvent,
          playerUserId = game.player(color).userId,
          mobilePushable = game.mobilePushable,
          alarmable = game.alarmable,
          unlimited = game.isUnlimited
        ),
        "moveEventCorres"
      )

    // publish simul moves
    for {
      simulId        <- game.simulId
      opponentUserId <- game.player(!color).userId
    } Bus.publish(
      SimulMoveEvent(move = moveEvent, simulId = simulId, opponentUserId = opponentUserId),
      "moveEventSimul"
    )
  }

  private def usiFinish(game: Game)(implicit proxy: GameProxy): Fu[Events] = {
    game.status match {
      case Status.Mate              => finisher.other(game, _.Mate, game.situation.winner)
      case Status.Stalemate         => finisher.other(game, _.Stalemate, game.situation.winner)
      case Status.Impasse27         => finisher.other(game, _.Impasse27, game.situation.winner)
      case Status.Repetition        => finisher.other(game, _.Repetition, game.situation.winner)
      case Status.PerpetualCheck    => finisher.other(game, _.PerpetualCheck, game.situation.winner)
      case Status.RoyalsLost        => finisher.other(game, _.RoyalsLost, game.situation.winner)
      case Status.BareKing          => finisher.other(game, _.BareKing, game.situation.winner)
      case Status.SpecialVariantEnd => finisher.other(game, _.SpecialVariantEnd, game.situation.winner)
      case Status.Draw              => finisher.other(game, _.Draw, None)
      case _                        => fuccess(Nil)
    }
  }
}
