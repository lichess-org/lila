package lila.api

import play.api.libs.json._
import reactivemongo.bson._

import chess.format.pgn.Pgn
import lila.analyse.{ AnalysisRepo, Analysis }
import lila.common.paginator.{ Paginator, PaginatorJson }
import lila.common.PimpedJson._
import lila.db.api._
import lila.db.Implicits._
import lila.db.paginator.{ BSONAdapter, CachedAdapter }
import lila.game.BSONHandlers._
import lila.game.Game.{ BSONFields => G }
import lila.game.tube.gameTube
import lila.game.{ Game, GameRepo, PerfPicker }
import lila.hub.actorApi.{ router => R }
import lila.user.User
import makeTimeout.short

private[api] final class GameApi(
    netBaseUrl: String,
    apiToken: String,
    pgnDump: PgnDump,
    analysisApi: AnalysisApi) {

  import lila.round.JsonView.openingWriter

  def byUser(
    user: User,
    rated: Option[Boolean],
    analysed: Option[Boolean],
    withAnalysis: Boolean,
    withMoves: Boolean,
    withOpening: Boolean,
    withMoveTimes: Boolean,
    token: Option[String],
    nb: Option[Int],
    page: Option[Int]): Fu[JsObject] = Paginator(
    adapter = new CachedAdapter(
      adapter = new BSONAdapter[Game](
        collection = gameTube.coll,
        selector = BSONDocument(
          G.playerUids -> user.id,
          G.status -> BSONDocument("$gte" -> chess.Status.Mate.id),
          G.rated -> rated.map(_.fold[BSONValue](BSONBoolean(true), BSONDocument("$exists" -> false))),
          G.analysed -> analysed.map(_.fold[BSONValue](BSONBoolean(true), BSONDocument("$exists" -> false)))
        ),
        projection = BSONDocument(),
        sort = BSONDocument(G.createdAt -> -1)
      ),
      nbResults = fuccess {
        rated.fold(user.count.game)(_.fold(user.count.rated, user.count.casual))
      }
    ),
    currentPage = math.max(0, page | 1),
    maxPerPage = math.max(1, math.min(100, nb | 10))) flatMap { pag =>
      gamesJson(
        withAnalysis = withAnalysis,
        withMoves = withMoves,
        withOpening = withOpening,
        withFens = false,
        withMoveTimes = withMoveTimes,
        token = token)(pag.currentPageResults) map { games =>
          PaginatorJson(pag withCurrentPageResults games)
        }
    }

  def one(
    id: String,
    withAnalysis: Boolean,
    withMoves: Boolean,
    withOpening: Boolean,
    withFens: Boolean,
    withMoveTimes: Boolean,
    token: Option[String]): Fu[Option[JsObject]] =
    $find byId id flatMap {
      _ ?? { g =>
        gamesJson(
          withAnalysis = withAnalysis,
          withMoves = withMoves,
          withOpening = withOpening,
          withFens = withFens && g.finished,
          withMoveTimes = withMoveTimes,
          token = token
        )(List(g)) map (_.headOption)
      }
    }

  private def makeUrl(game: Game) = s"$netBaseUrl/${game.id}/${game.firstPlayer.color.name}"

  private def gamesJson(
    withAnalysis: Boolean,
    withMoves: Boolean,
    withOpening: Boolean,
    withFens: Boolean,
    withMoveTimes: Boolean,
    token: Option[String])(games: Seq[Game]): Fu[Seq[JsObject]] =
    AnalysisRepo doneByIds games.map(_.id) flatMap { analysisOptions =>
      (games map GameRepo.initialFen).sequenceFu map { initialFens =>
        val validToken = check(token)
        games zip analysisOptions zip initialFens map {
          case ((g, analysisOption), initialFen) =>
            val pgnOption = withAnalysis option pgnDump(g, initialFen)
            gameToJson(g, makeUrl(g), analysisOption, pgnOption, initialFen,
              withAnalysis = withAnalysis,
              withMoves = withMoves,
              withOpening = withOpening,
              withFens = withFens,
              withBlurs = validToken,
              withHold = validToken,
              withMoveTimes = withMoveTimes)
        }
      }
    }

  private def check(token: Option[String]) = token ?? (apiToken==)

  private def gameToJson(
    g: Game,
    url: String,
    analysisOption: Option[Analysis],
    pgnOption: Option[Pgn],
    initialFen: Option[String],
    withAnalysis: Boolean,
    withMoves: Boolean,
    withOpening: Boolean,
    withFens: Boolean,
    withBlurs: Boolean = false,
    withHold: Boolean = false,
    withMoveTimes: Boolean = false) = Json.obj(
    "id" -> g.id,
    "initialFen" -> initialFen,
    "rated" -> g.rated,
    "variant" -> g.variant.key,
    "speed" -> g.speed.key,
    "perf" -> PerfPicker.key(g),
    "timestamp" -> g.createdAt.getDate,
    "turns" -> g.turns,
    "status" -> g.status.name,
    "clock" -> g.clock.map { clock =>
      Json.obj(
        "initial" -> clock.limit,
        "increment" -> clock.increment,
        "totalTime" -> clock.estimateTotalTime
      )
    },
    "players" -> JsObject(g.players.zipWithIndex map {
      case (p, i) => p.color.name -> Json.obj(
        "userId" -> p.userId,
        "name" -> p.name,
        "rating" -> p.rating,
        "ratingDiff" -> p.ratingDiff,
        "provisional" -> p.provisional.option(true),
        "moveTimes" -> withMoveTimes.fold(
          g.moveTimes.zipWithIndex.filter(_._2 % 2 == i).map(_._1),
          JsNull),
        "blurs" -> withBlurs.option(p.blurs),
        "hold" -> p.holdAlert.ifTrue(withHold).fold[JsValue](JsNull) { h =>
          Json.obj(
            "ply" -> h.ply,
            "mean" -> h.mean,
            "sd" -> h.sd
          )
        },
        "analysis" -> analysisOption.flatMap(analysisApi.player(p.color))
      ).noNull
    }),
    "analysis" -> analysisOption.ifTrue(withAnalysis).|@|(pgnOption).apply(analysisApi.game),
    "moves" -> withMoves.option(g.pgnMoves mkString " "),
    "opening" -> withOpening.??(g.opening),
    "fens" -> withFens ?? {
      chess.Replay.boards(g.pgnMoves, initialFen, g.variant).toOption map { boards =>
        JsArray(boards map chess.format.Forsyth.exportBoard map JsString.apply)
      }
    },
    "winner" -> g.winnerColor.map(_.name),
    "url" -> url
  ).noNull
}
