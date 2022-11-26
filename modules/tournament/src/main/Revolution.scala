package lila.tournament

import org.joda.time.DateTime
import scala.concurrent.duration.*
import reactivemongo.api.ReadPreference

import chess.variant.Variant
import lila.db.dsl.{ *, given }
import lila.user.User
import lila.memo.CacheApi.*

final class RevolutionApi(
    tournamentRepo: TournamentRepo,
    cacheApi: lila.memo.CacheApi
)(using ec: scala.concurrent.ExecutionContext):

  import Revolution.*
  import BSONHandlers.given

  def active(u: User): Fu[List[Award]] = cache.getUnit dmap { ~_.get(u.id) }

  private[tournament] def clear() = cache.invalidateUnit()

  private val cache = cacheApi.unit[PerOwner] {
    _.refreshAfterWrite(1 day)
      .buildAsyncFuture { _ =>
        tournamentRepo.coll
          .find(
            $doc(
              "schedule.freq" -> (Schedule.Freq.Unique: Schedule.Freq),
              "startsAt" $lt DateTime.now $gt DateTime.now.minusYears(1).minusDays(1),
              "name" $regex Revolution.namePattern,
              "status" -> (Status.Finished: Status)
            ),
            $doc("winner" -> true, "variant" -> true).some
          )
          .cursor[Bdoc](ReadPreference.secondaryPreferred)
          .list(300) map { docOpt =>
          val awards =
            for {
              doc     <- docOpt
              winner  <- doc.getAsOpt[User.ID]("winner")
              variant <- doc.int("variant") flatMap Variant.apply
              id      <- doc.getAsOpt[Tournament.ID]("_id")
            } yield Award(
              owner = winner,
              variant = variant,
              tourId = id
            )
          awards.groupBy(_.owner)
        }
      }
  }

object Revolution:

  val namePattern = """ Revolution #\d+$"""
  val nameRegex   = namePattern.r

  def is(tour: Tournament) = tour.isUnique && nameRegex.pattern.matcher(tour.name).find

  case class Award(
      owner: User.ID,
      variant: Variant,
      tourId: Tournament.ID
  ):
    val iconChar = lila.rating.PerfType iconByVariant variant

  type PerOwner = Map[User.ID, List[Award]]
