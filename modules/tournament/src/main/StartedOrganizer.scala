package lila.tournament

import akka.stream.scaladsl.*

import lila.common.{ LilaScheduler, LilaStream }

final private class StartedOrganizer(
    api: TournamentApi,
    tournamentRepo: TournamentRepo,
    playerRepo: PlayerRepo,
    pairingRepo: PairingRepo,
    waitingUsersApi: WaitingUsersApi,
    socket: TournamentSocket
)(using Executor, Scheduler, akka.stream.Materializer):

  var runCounter = 0

  LilaScheduler("Tournament.StartedOrganizer", _.Every(1.seconds), _.AtMost(30.seconds), _.Delay(26.seconds)):

    val doAllTournaments = runCounter % 15 == 0

    tournamentRepo
      .startedCursorWithNbPlayersGte:
        if doAllTournaments then none // every 15s, do all tournaments
        else if runCounter % 2 == 0 then 50.some // every 2s, do all decent tournaments
        else 1000.some // always do massive tournaments
      .documentSource()
      .mapAsyncUnordered(4): tour =>
        processTour(tour).recover { case e: Exception =>
          logger.error(s"StartedOrganizer $tour", e)
          0
        }
      .toMat(LilaStream.sinkCount)(Keep.right)
      .run()
      .addEffect: nb =>
        if doAllTournaments then lila.mon.tournament.started.update(nb)
        runCounter = runCounter + 1
      .monSuccess(_.tournament.startedOrganizer.tick)
      .void

  private def processTour(tour: Tournament): Funit =
    if tour.secondsToFinish <= 0 then api.finish(tour)
    else if api.killSchedule contains tour.id then
      api.killSchedule.remove(tour.id)
      api.finish(tour)
    else if tour.nbPlayers < 2 then funit
    else if tour.nbPlayers < 30 then
      playerRepo.nbActivePlayers(tour.id).flatMap { nb =>
        (nb >= 2).so(startPairing(tour, nb.some))
      }
    else startPairing(tour)

  private def startPairing(tour: Tournament, smallTourNbActivePlayers: Option[Int] = None): Funit =
    tour.pairingsClosed.not.so:
      for
        waitingBots <- fetchWaitingBots(tour)
        _ = if waitingBots.nonEmpty then waitingUsersApi.addApiUsers(tour, waitingBots)
        waiting <- socket.getWaitingUsers(tour).monSuccess(_.tournament.startedOrganizer.waitingUsers)
        _ = waiting.hash
        _ = lila.mon.tournament.waitingPlayers.record(waiting.size)
        _ <- api.makePairings(tour, waiting, smallTourNbActivePlayers)
      yield ()

  private def fetchWaitingBots(tour: Tournament): Fu[Set[UserId]] =
    tour.conditions.allowsBots.so:
      for
        activeBots <- playerRepo.activeBotIds(tour.id)
        playingUsers <- activeBots.nonEmpty.so(pairingRepo.playingUserIds(tour.id))
      yield activeBots.diff(playingUsers)
