package lila.tournament

import chess.variant.Variant

import lila.core.tournament.Status
import lila.db.dsl.{ *, given }
import lila.memo.CacheApi.*

final class RevolutionApi(
    tournamentRepo: TournamentRepo,
    cacheApi: lila.memo.CacheApi
)(using Executor):

  import Revolution.*
  import BSONHandlers.given

  def active(u: User): Fu[List[Award]] = cache.getUnit.dmap { ~_.get(u.id) }

  private[tournament] def clear() = cache.invalidateUnit()

  private val cache = cacheApi.unit[PerOwner]:
    _.refreshAfterWrite(1.day)
      .buildAsyncFuture { _ =>
        tournamentRepo.coll
          .find(
            $doc(
              "schedule.freq" -> (Schedule.Freq.Unique: Schedule.Freq),
              "startsAt".$lt(nowInstant).$gt(nowInstant.minusYears(1).minusDays(1)),
              "name".$regex(Revolution.namePattern),
              "status" -> (Status.Finished: Status)
            ),
            $doc("winner" -> true, "variant" -> true).some
          )
          .cursor[Bdoc](ReadPref.sec)
          .list(300)
          .map { docOpt =>
            val awards =
              for
                doc     <- docOpt
                winner  <- doc.getAsOpt[UserId]("winner")
                variant <- doc.getAsOpt[Variant.Id]("variant").map(Variant.orDefault)
                id      <- doc.getAsOpt[TourId]("_id")
              yield Award(
                owner = winner,
                variant = variant,
                tourId = id
              )
            awards.groupBy(_.owner)
          }
      }

object Revolution:

  val namePattern = """ Revolution #\d+$"""
  val nameRegex   = namePattern.r

  def is(tour: Tournament) = tour.isUnique && nameRegex.pattern.matcher(tour.name).find

  case class Award(
      owner: UserId,
      variant: Variant,
      tourId: TourId
  ):
    val iconChar = lila.rating.PerfType.iconByVariant(variant)

  type PerOwner = Map[UserId, List[Award]]
