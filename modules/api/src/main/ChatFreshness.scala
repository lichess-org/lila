package lila.api

import lila.core.shutup.PublicSource
import lila.swiss.{ Swiss, SwissCache }
import lila.tournament.{ Tournament, TournamentCache }

/* Checks that a chat can still be posted to */
final class ChatFreshness(tourCache: TournamentCache, swissCache: SwissCache)(using Executor):

  def of: PublicSource => Fu[Boolean] =
    case PublicSource.Tournament(id) => tourCache.tourCache.byId(id).mapz(of)
    case PublicSource.Swiss(id)      => swissCache.swissCache.byId(id).mapz(of)
    case _                           => fuTrue

  def of(tour: Tournament) =
    tour.finishedSinceSeconds.forall:
      _ < (tour.nbPlayers + 120) * 30

  def of(swiss: Swiss) =
    swiss.finishedSinceSeconds.forall:
      _ < (swiss.nbPlayers + 60) * 60
