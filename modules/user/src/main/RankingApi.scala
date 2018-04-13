package lila.user

import org.joda.time.DateTime
import reactivemongo.api.collections.bson.BSONBatchCommands.AggregationFramework.{ Match, Project, GroupField, SumValue }
import reactivemongo.api.ReadPreference
import reactivemongo.bson._
import scala.concurrent.duration._

import lila.db.dsl._
import lila.rating.{ Perf, PerfType }

final class RankingApi(
    coll: Coll,
    mongoCache: lila.memo.MongoCache.Builder,
    asyncCache: lila.memo.AsyncCache.Builder,
    lightUser: lila.common.LightUser.Getter
) {

  import RankingApi._
  private implicit val rankingBSONHandler = Macros.handler[Ranking]

  def save(userId: User.ID, perfType: Option[PerfType], perfs: Perfs): Funit =
    perfType ?? { pt =>
      save(userId, pt, perfs(pt))
    }

  def save(userId: User.ID, perfType: PerfType, perf: Perf): Funit =
    (perf.nb >= 2) ?? coll.update($id(makeId(userId, perfType)), $doc(
      "perf" -> perfType.id,
      "rating" -> perf.intRating,
      "prog" -> perf.progress,
      "stable" -> perf.established,
      "expiresAt" -> DateTime.now.plusDays(7)
    ),
      upsert = true).void

  def remove(userId: User.ID): Funit = UserRepo byId userId flatMap {
    _ ?? { user =>
      coll.remove($inIds(
        PerfType.leaderboardable.filter { pt =>
          user.perfs(pt).nonEmpty
        }.map { makeId(user.id, _) }
      )).void
    }
  }

  private def makeId(userId: User.ID, perfType: PerfType) =
    s"${userId}:${perfType.id}"

  private[user] def topPerf(perfId: Perf.ID, nb: Int): Fu[List[User.LightPerf]] =
    PerfType.id2key(perfId) ?? { perfKey =>
      coll.find($doc("perf" -> perfId, "stable" -> true))
        .sort($doc("rating" -> -1))
        .cursor[Ranking](readPreference = ReadPreference.secondaryPreferred)
        .gather[List](nb) flatMap {
          _.map { r =>
            lightUser(r.user).map {
              _ map { light =>
                User.LightPerf(
                  user = light,
                  perfKey = perfKey,
                  rating = r.rating,
                  progress = ~r.prog
                )
              }
            }
          }.sequenceFu.map(_.flatten)
        }
    }

  object weeklyStableRanking {

    private type Rank = Int

    def of(userId: User.ID): Fu[Map[Perf.Key, Rank]] =
      lila.common.Future.traverseSequentially(PerfType.leaderboardable) { perf =>
        cache.get(perf.id) map { _ get userId map (perf.key -> _) }
      } map (_.flatten.toMap) nevermind

    private val cache = asyncCache.multi[Perf.ID, Map[User.ID, Rank]](
      name = "rankingApi.weeklyStableRanking",
      f = compute,
      expireAfter = _.ExpireAfterWrite(15 minutes),
      resultTimeout = 10 seconds
    )

    private def compute(perfId: Perf.ID): Fu[Map[User.ID, Rank]] =
      coll.find(
        $doc("perf" -> perfId, "stable" -> true),
        $doc("_id" -> true)
      ).sort($doc("rating" -> -1)).cursor[Bdoc](readPreference = ReadPreference.secondaryPreferred)
        .fold(1 -> Map.newBuilder[User.ID, Rank]) {
          case (state @ (rank, b), doc) =>
            doc.getAs[String]("_id").fold(state) { id =>
              val user = id takeWhile (':' !=)
              b += (user -> rank)
              (rank + 1) -> b
            }
        }.map(_._2.result())
  }

  object weeklyRatingDistribution {

    private type NbUsers = Int

    def apply(perf: PerfType) = cache(perf.id)

    private val cache = mongoCache[Perf.ID, List[NbUsers]](
      prefix = "user:rating:distribution",
      f = compute,
      timeToLive = 3 hour,
      keyToString = _.toString
    )

    // from 800 to 2500 by Stat.group
    private def compute(perfId: Perf.ID): Fu[List[NbUsers]] =
      lila.rating.PerfType(perfId).exists(lila.rating.PerfType.leaderboardable.contains) ?? {
        coll.aggregateList(
          Match($doc("perf" -> perfId)),
          List(
            Project($doc(
              "_id" -> false,
              "r" -> $doc(
                "$subtract" -> $arr(
                  "$rating",
                  $doc("$mod" -> $arr("$rating", Stat.group))
                )
              )
            )),
            GroupField("r")("nb" -> SumValue(1))
          ),
          maxDocs = Int.MaxValue,
          ReadPreference.secondaryPreferred
        ).map { res =>
            val hash: Map[Int, NbUsers] = res.flatMap { obj =>
              for {
                rating <- obj.getAs[Int]("_id")
                nb <- obj.getAs[NbUsers]("nb")
              } yield rating -> nb
            }(scala.collection.breakOut)
            (800 to 2800 by Stat.group).map { r =>
              hash.getOrElse(r, 0)
            }(scala.collection.breakOut)
          }
      }
  }
}

object RankingApi {

  private case class Ranking(_id: String, rating: Int, prog: Option[Int]) {
    def user = _id.takeWhile(':' !=)
  }
}
