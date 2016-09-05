package lila.api

import play.api.libs.json._
import reactivemongo.api.ReadPreference
import reactivemongo.bson._

import chess.format.pgn.Pgn
import lila.analyse.{ JsonView => analysisJson, AnalysisRepo, Analysis }
import lila.common.paginator.{ Paginator, PaginatorJson }
import lila.common.PimpedJson._
import lila.db.dsl._
import lila.db.paginator.{ Adapter, CachedAdapter }
import lila.game.BSONHandlers._
import lila.game.Game.{ BSONFields => G }
import lila.game.{ Game, Pov, GameRepo, PerfPicker }
import lila.user.User
import makeTimeout.short

private[api] final class GameApi(
    netBaseUrl: String,
    apiToken: String,
    pgnDump: PgnDump,
    gameCache: lila.game.Cached) {

  import lila.round.JsonView.openingWriter

  def byUser(
    user: User,
    rated: Option[Boolean],
    playing: Option[Boolean],
    analysed: Option[Boolean],
    withAnalysis: Boolean,
    withMoves: Boolean,
    withOpening: Boolean,
    withMoveTimes: Boolean,
    token: Option[String],
    nb: Int,
    page: Int): Fu[JsObject] = Paginator(
    adapter = new CachedAdapter(
      adapter = new Adapter[Game](
        collection = GameRepo.coll,
        selector = {
          if (~playing) lila.game.Query.nowPlaying(user.id)
          else $doc(
            G.playerUids -> user.id,
            G.status $gte chess.Status.Mate.id,
            G.analysed -> analysed.map(_.fold[BSONValue](BSONBoolean(true), $doc("$exists" -> false)))
          )
        } ++ $doc(
          G.rated -> rated.map(_.fold[BSONValue](BSONBoolean(true), $doc("$exists" -> false)))
        ),
        projection = $empty,
        sort = $doc(G.createdAt -> -1),
        readPreference = ReadPreference.secondaryPreferred
      ),
      nbResults =
        if (~playing) gameCache.nbPlaying(user.id)
        else fuccess {
          rated.fold(user.count.game)(_.fold(user.count.rated, user.count.casual))
        }
    ),
    currentPage = page,
    maxPerPage = nb) flatMap { pag =>
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
    GameRepo game id flatMap {
      _ ?? { g =>
        gamesJson(
          withAnalysis = withAnalysis,
          withMoves = withMoves,
          withOpening = withOpening,
          withFens = withFens,
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
    AnalysisRepo byIds games.map(_.id) flatMap { analysisOptions =>
      (games map GameRepo.initialFen).sequenceFu map { initialFens =>
        val validToken = check(token)
        games zip analysisOptions zip initialFens map {
          case ((g, analysisOption), initialFen) =>
            gameToJson(g, makeUrl(g), analysisOption, initialFen,
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

  private def check(token: Option[String]) = token contains apiToken

  private def gameToJson(
    g: Game,
    url: String,
    analysisOption: Option[Analysis],
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
    "createdAt" -> g.createdAt.getDate,
    "lastMoveAt" -> (g.lastMoveDateTime | g.createdAt).getDate,
    "turns" -> g.turns,
    "color" -> g.turnColor.name,
    "status" -> g.status.name,
    "clock" -> g.clock.map { clock =>
      Json.obj(
        "initial" -> clock.limit,
        "increment" -> clock.increment,
        "totalTime" -> clock.estimateTotalTime
      )
    },
    "daysPerTurn" -> g.daysPerTurn,
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
        "analysis" -> analysisOption.flatMap(analysisJson.player(g pov p.color))
      ).noNull
    }),
    "analysis" -> analysisOption.ifTrue(withAnalysis).map(analysisJson.moves),
    "moves" -> withMoves.option(g.pgnMoves mkString " "),
    "opening" -> withOpening.??(g.opening),
    "fens" -> (withFens && g.finished) ?? {
      chess.Replay.boards(g.pgnMoves, initialFen, g.variant).toOption map { boards =>
        JsArray(boards map chess.format.Forsyth.exportBoard map JsString.apply)
      }
    },
    "winner" -> g.winnerColor.map(_.name),
    "url" -> url
  ).noNull
}
