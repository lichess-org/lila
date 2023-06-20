package lila.api

import lila.chat.UserLine
import lila.hub.actorApi.shutup.PublicSource
import lila.tournament.{ Tournament, TournamentCache }
import lila.swiss.{ Swiss, SwissCache }

/* Checks that a chat can still be posted to */
final class ChatFreshness(tourCache: TournamentCache, swissCache: SwissCache)(using Executor):

  def of: PublicSource => Fu[Boolean] =
    case PublicSource.Tournament(id) => tourCache.tourCache.byId(id).mapz(of)
    case PublicSource.Swiss(id)      => swissCache.swissCache.byId(id).mapz(of)
    case _                           => fuTrue

  def of(tour: Tournament) =
    tour.finishedSinceSeconds.fold(true):
      _ < (tour.nbPlayers + 120) * 30

  def of(swiss: Swiss) =
    swiss.finishedSinceSeconds.fold(true):
      _ < (swiss.nbPlayers + 60) * 60
