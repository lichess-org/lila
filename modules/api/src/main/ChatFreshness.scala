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
    tour.finishedSinceSeconds match
      case Some(finishedSinceSeconds) => finishedSinceSeconds < (tour.nbPlayers + 120) * 30
      case None                       => tour.startsAt.isBefore(nowInstant.plusWeeks(1))

  def of(swiss: Swiss) =
    swiss.finishedSinceSeconds match
      case Some(finishedSinceSeconds) => finishedSinceSeconds < (swiss.nbPlayers + 60) * 60
      case None                       => swiss.startsAt.isBefore(nowInstant.plusWeeks(1))
