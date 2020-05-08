package lila.swiss

import scala.concurrent.duration._
import play.api.libs.json._

import lila.common.LightUser
import lila.game.Game

private case class SwissBoard(
    gameId: Game.ID,
    p1: SwissBoard.Player,
    p2: SwissBoard.Player
)

private object SwissBoard {
  case class Player(user: LightUser, rank: Int, rating: Int)
}

final private class SwissBoardApi(
    colls: SwissColls,
    rankingApi: SwissRankingApi,
    cacheApi: lila.memo.CacheApi,
    lightUserApi: lila.user.LightUserApi
)(implicit ec: scala.concurrent.ExecutionContext) {

  private val displayBoards = 5

  private val boardsCache = cacheApi.scaffeine
    .expireAfterWrite(60 minutes)
    .build[Swiss.Id, JsArray]

  def apply(id: Swiss.Id): JsArray = boardsCache getIfPresent id getOrElse Json.arr()

  def update(data: SwissScoring.Result): Funit =
    data match {
      case SwissScoring.Result(swiss, players, pairings) =>
        rankingApi(swiss) map { ranks =>
          boardsCache
            .put(
              swiss.id,
              JsArray {
                players.values.toList
                  .filter(_.present)
                  .sortBy(-_.score.value)
                  .flatMap(player => pairings get player.number)
                  .distinct
                  .take(displayBoards)
                  .flatMap { pairing =>
                    for {
                      p1 <- players get pairing.white
                      p2 <- players get pairing.black
                      u1 <- lightUserApi sync p1.userId
                      u2 <- lightUserApi sync p2.userId
                      r1 <- ranks get p1.number
                      r2 <- ranks get p2.number
                    } yield SwissJson.boardJson(
                      SwissBoard(
                        pairing.gameId,
                        p1 = SwissBoard.Player(u1, r1, p1.rating),
                        p2 = SwissBoard.Player(u2, r2, p2.rating)
                      )
                    )
                  }
              }
            )
        }
    }

  // def roundStart(swiss: Swiss) = cache.get(swiss.id)

  // private def compute(id: Swiss.Id): Fu[List[SwissBoard]] =
  //   colls.pairing
  //     .aggregateList(maxDocs = maxBoards, readPreference = ReadPreference.primary) { implicit framework =>
  //       import framework._
  //       Match(
  //         SwissPairing.fields { f =>
  //           $doc(f.swissId -> id, f.status -> SwissPairing.ongoing)
  //         }
  //       ) -> List(
  //         PipelineOperator(
  //           $doc(
  //             "$lookup" -> $doc(
  //               "from" -> colls.player.name,
  //               "let"  -> $doc("p" -> "$p"),
  //               "pipeline" -> $arr(
  //                 $doc(
  //                   "$match" -> $doc(
  //                     "$expr" -> $doc(
  //                       "$and" -> $arr(
  //                         $doc("$eq" -> $arr("$s", id))
  //                           $doc ("$in" -> $arr("$n", "$$p"))
  //                       )
  //                     )
  //                   )
  //                 ),
  //                 $doc("$project" -> SwissPlayer.fields { f =>
  //                   $doc(f.id -> false, f.userId -> true, f.rating -> true)
  //                 })
  //               ),
  //               "as" -> "players"
  //             )
  //           )
  //         ),
  //         Project(
  //           $doc("_id" -> false, "g" -> true, "players" -> true, "rating" -> $doc("$average" -> "$players.r"))
  //         ),
  //         Sort(Descending("rating")),
  //         Project($doc("rating" -> false)),
  //         Limit(maxBoards)
  //       )
  //     }
  //     .map(_.flatMap { doc =>
  //       for {
  //         gameId     <- doc.str("g")
  //         playerDocs <- doc.getAsOpt[List[Bdoc]]("players")
  //         players <-
  //           playerDocs
  //             .flatMap { doc =>
  //               for {
  //                 userId <- doc.str("u")
  //                 rating <- doc.int("r")
  //               } yield (userId, rating)
  //             }
  //             .some
  //             .filter(_.size == 2)
  //       } yield (g, players)
  //     })
}
