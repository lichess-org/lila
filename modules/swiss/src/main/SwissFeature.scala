package lila.swiss

import scalalib.HeapSort

import lila.core.swiss.FeaturedIdNames
import lila.db.dsl.{ *, given }
import lila.memo.CacheApi
import lila.memo.CacheApi.*

final class SwissFeature(
    mongo: SwissMongo,
    cacheApi: CacheApi,
    swissCache: SwissCache
)(using Executor)
    extends lila.core.swiss.SwissFeatureApi:

  import BsonHandlers.given

  val onHomepage = cacheApi.unit[Option[Swiss]]:
    _.refreshAfterWrite(30.seconds)
      .buildAsyncFuture: _ =>
        mongo.swiss
          .find:
            $doc(
              "teamId" -> lichessTeamId,
              "startsAt".$gt(nowInstant.minusMinutes(5)).$lt(nowInstant.plusMinutes(10))
            )
          .sort($sort.asc("startsAt"))
          .one[Swiss]

  def get(teams: Seq[TeamId]): Fu[FeaturedSwisses] =
    cache.getUnit
      .zip(getForTeams((teams :+ lichessTeamId).distinct))
      .map: (cached, teamed) =>
        FeaturedSwisses(
          created = (teamed.created ::: cached.created).distinctBy(_.id),
          started = (teamed.started ::: cached.started).distinctBy(_.id)
        )

  def idNames: Fu[FeaturedIdNames] = get(Nil).map: f =>
    FeaturedIdNames(f.created.map(_.idName), f.started.map(_.idName))

  private val startsAtOrdering = Ordering.by[Swiss, Long](_.startsAt.toMillis)

  private def getForTeams(teams: Seq[TeamId]): Fu[FeaturedSwisses] =
    teams
      .traverse(swissCache.featuredInTeam.get)
      .dmap(_.flatten)
      .flatMap: ids =>
        mongo.swiss.byIds[Swiss, SwissId](ids, _.sec)
      .map: tours =>
        val (created, started) = tours.filter(_.isNotFinished).partition(_.isCreated)
        FeaturedSwisses(
          created = HeapSort.topN(created, 10)(using startsAtOrdering.reverse),
          started = HeapSort.topN(started, 10)(using startsAtOrdering)
        )

  private val cache = cacheApi.unit[FeaturedSwisses]:
    _.refreshAfterWrite(10.seconds).buildAsyncFuture: _ =>
      val now = nowInstant
      cacheCompute($doc("$gt" -> now, "$lt" -> now.plusHours(1)))
        .zip(cacheCompute($doc("$gt" -> now.minusHours(3), "$lt" -> now)))
        .map(FeaturedSwisses.apply)

  // causes heavy team reads
  private def cacheCompute(startsAtRange: Bdoc, nb: Int = 5): Fu[List[Swiss]] =
    mongo.swiss
      .aggregateList(nb, _.sec): framework =>
        import framework.*
        Match(
          $doc(
            "featurable" -> true,
            "settings.i".$lte(600), // hits the partial index
            "settings.o.playYourGames" -> true,
            "startsAt" -> startsAtRange,
            "garbage".$ne(true)
          )
        ) -> List(
          Sort(Descending("nbPlayers")),
          Limit(nb * 50),
          PipelineOperator(
            $lookup.pipeline(
              from = "team",
              as = "team",
              local = "teamId",
              foreign = "_id",
              pipe = List(
                $doc("$match" -> $doc("open" -> true, "password".$exists(false))),
                $doc("$project" -> $id(true))
              )
            )
          ),
          UnwindField("team"),
          Limit(nb)
        )
      .map { _.flatMap(_.asOpt[Swiss]) }
