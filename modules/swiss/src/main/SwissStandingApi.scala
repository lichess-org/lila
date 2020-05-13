package lila.swiss

import play.api.libs.json._
import scala.concurrent.duration._

import lila.common.LightUser
import lila.db.dsl._

/*
 * Getting a standing page of a tournament can be very expensive
 * because it can iterate through thousands of mongodb documents.
 * Try to cache the stuff, and limit concurrent access to prevent
 * overloading mongodb.
 */
final class SwissStandingApi(
    colls: SwissColls,
    cached: SwissCache,
    rankingApi: SwissRankingApi,
    cacheApi: lila.memo.CacheApi,
    lightUserApi: lila.user.LightUserApi
)(implicit ec: scala.concurrent.ExecutionContext) {

  import BsonHandlers._

  private val pageCache = cacheApi.scaffeine
    .expireAfterWrite(60 minutes)
    .build[(Swiss.Id, Int), JsObject]

  def apply(swiss: Swiss, page: Int): Fu[JsObject] =
    fuccess(pageCache.getIfPresent(swiss.id -> page)) getOrElse {
      if (page == 1) first get swiss.id
      else compute(swiss, page)
    }

  def update(res: SwissScoring.Result): Funit =
    rankingApi(res.swiss) flatMap { ranking =>
      lightUserApi
        .asyncMany(res.players.map(_.userId))
        .map(res.players.zip)
        .map(res.sheets.zip)
        .map(_ grouped 10)
        .map(_.zipWithIndex)
        .map(_.toList)
        .map {
          _ map {
            case (pagePlayers, i) =>
              val page = i + 1
              pageCache.put(
                res.swiss.id -> page,
                Json.obj(
                  "page" -> page,
                  "players" -> pagePlayers
                    .map {
                      case (sheet, (player, user)) =>
                        SwissJson.playerJson(
                          res.swiss,
                          SwissPlayer.View(
                            player = player,
                            rank = ranking.getOrElse(player.number, 0),
                            user = user | LightUser.fallback(player.userId),
                            ~res.pairings.get(player.number),
                            sheet
                          )
                        )
                    }
                )
              )
          }
        }
    }

  private val first = cacheApi[Swiss.Id, JsObject](16, "swiss.page.first") {
    _.expireAfterWrite(1 minute)
      .buildAsyncFuture { compute(_, 1) }
  }

  private def compute(id: Swiss.Id, page: Int): Fu[JsObject] =
    colls.swiss.byId[Swiss](id.value) orFail s"No such tournament: $id" flatMap { compute(_, page) }

  private def compute(swiss: Swiss, page: Int): Fu[JsObject] =
    for {
      rankedPlayers <- bestWithRankByPage(swiss.id, 10, page atLeast 1)
      pairings <- SwissPairing.fields { f =>
        colls.pairing.ext
          .find($doc(f.swissId -> swiss.id, f.players $in rankedPlayers.map(_.player.number)))
          .sort($sort asc f.round)
          .list[SwissPairing]()
          .map(SwissPairing.toMap)
      }
      sheets = SwissSheet.many(swiss, rankedPlayers.map(_.player), pairings)
      users <- lightUserApi asyncMany rankedPlayers.map(_.player.userId)
    } yield Json.obj(
      "page" -> page,
      "players" -> rankedPlayers
        .zip(users)
        .zip(sheets)
        .map {
          case SwissPlayer.Ranked(rank, player) ~ user ~ sheet =>
            SwissJson.playerJson(
              swiss,
              SwissPlayer.View(
                player,
                rank,
                user | LightUser.fallback(player.userId),
                ~pairings.get(player.number),
                sheet
              )
            )
        }
    )

  private[swiss] def bestWithRank(id: Swiss.Id, nb: Int, skip: Int = 0): Fu[List[SwissPlayer.Ranked]] =
    best(id, nb, skip).map { res =>
      res
        .foldRight(List.empty[SwissPlayer.Ranked] -> (res.size + skip)) {
          case (p, (res, rank)) => (SwissPlayer.Ranked(rank, p) :: res, rank - 1)
        }
        ._1
    }

  private[swiss] def bestWithRankByPage(id: Swiss.Id, nb: Int, page: Int): Fu[List[SwissPlayer.Ranked]] =
    bestWithRank(id, nb, (page - 1) * nb)

  private[swiss] def best(id: Swiss.Id, nb: Int, skip: Int = 0): Fu[List[SwissPlayer]] =
    SwissPlayer.fields { f =>
      colls.player.ext.find($doc(f.swissId -> id)).sort($sort desc f.score).skip(skip).list[SwissPlayer](nb)
    }
}
