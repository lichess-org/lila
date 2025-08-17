package lila.api

import chess.format.Fen
import play.api.libs.json.*
import reactivemongo.api.bson.*
import scalalib.Json.given
import scalalib.paginator.Paginator

import lila.analyse.{ Analysis, JsonView as analysisJson }
import lila.common.Json.given
import lila.core.config.*
import lila.db.dsl.{ *, given }
import lila.db.paginator.Adapter
import lila.game.BSONHandlers.given
import lila.game.Game.BSONFields as G

final private[api] class GameApi(
    net: NetConfig,
    apiToken: Secret,
    gameRepo: lila.game.GameRepo,
    gameCache: lila.game.Cached,
    analysisRepo: lila.analyse.AnalysisRepo,
    crosstableApi: lila.game.CrosstableApi
)(using Executor):

  import GameApi.WithFlags

  def one(id: GameId, withFlags: WithFlags): Fu[Option[JsObject]] =
    gameRepo
      .game(id)
      .flatMapz: g =>
        gamesJson(withFlags)(List(g)).map(_.headOption)

  def byUsersVs(
      users: (User, User),
      rated: Option[Boolean],
      playing: Option[Boolean],
      analysed: Option[Boolean],
      withFlags: WithFlags,
      nb: MaxPerPage,
      page: Int
  ): Fu[JsObject] =
    Paginator(
      adapter = Adapter[Game](
        collection = gameRepo.coll,
        selector = {
          if ~playing then lila.game.Query.nowPlayingVs(users._1.id, users._2.id)
          else
            lila.game.Query.opponents(users._1, users._2) ++ $doc(
              G.status.$gte(chess.Status.Mate.id),
              G.analysed -> analysed.map[BSONValue] {
                if _ then BSONBoolean(true)
                else $doc("$exists" -> false)
              }
            )
        } ++ $doc(
          G.rated -> rated.map[BSONValue] {
            if _ then BSONBoolean(true)
            else $doc("$exists" -> false)
          }
        ),
        projection = none,
        sort = $doc(G.createdAt -> -1),
        _.sec
      ).withNbResults(
        if ~playing then gameCache.nbPlaying(users._1.id)
        else crosstableApi(users._1.id, users._2.id).dmap(_.nbGames)
      ),
      currentPage = page,
      maxPerPage = nb
    ).flatMap { pag =>
      gamesJson(withFlags.copy(fens = false))(pag.currentPageResults).map { games =>
        Json.toJsObject(pag.withCurrentPageResults(games))
      }
    }

  def byUsersVs(
      userIds: Iterable[UserId],
      rated: Option[Boolean],
      playing: Option[Boolean],
      analysed: Option[Boolean],
      withFlags: WithFlags,
      since: Instant,
      nb: MaxPerPage,
      page: Int
  ): Fu[JsObject] =
    Paginator(
      adapter = Adapter[Game](
        collection = gameRepo.coll,
        selector = {
          if ~playing then lila.game.Query.nowPlayingVs(userIds)
          else
            lila.game.Query.opponents(userIds) ++ $doc(
              G.status.$gte(chess.Status.Mate.id),
              G.analysed -> analysed.map[BSONValue] {
                if _ then BSONBoolean(true)
                else $doc("$exists" -> false)
              }
            )
        } ++ $doc(
          G.rated -> rated.map[BSONValue] {
            if _ then BSONBoolean(true)
            else $doc("$exists" -> false)
          },
          G.createdAt.$gte(since)
        ),
        projection = none,
        sort = $doc(G.createdAt -> -1),
        _.sec
      ),
      currentPage = page,
      maxPerPage = nb
    ).flatMap { pag =>
      gamesJson(withFlags.copy(fens = false))(pag.currentPageResults).map { games =>
        Json.toJsObject(pag.withCurrentPageResults(games))
      }
    }

  private def makeUrl(game: Game) = s"${net.baseUrl}/${game.id}/${game.naturalOrientation.name}"

  private def gamesJson(withFlags: WithFlags)(games: Seq[Game]): Fu[Seq[JsObject]] =
    val allAnalysis =
      if withFlags.analysis then analysisRepo.byIds(games.map(g => Analysis.Id(g.id)))
      else fuccess(List.fill(games.size)(none[Analysis]))
    allAnalysis.flatMap { analysisOptions =>
      (games.map(gameRepo.initialFen)).parallel.map { initialFens =>
        games.zip(analysisOptions).zip(initialFens).map { case ((g, analysisOption), initialFen) =>
          gameToJson(g, analysisOption, initialFen, checkToken(withFlags))
        }
      }
    }

  private def checkToken(withFlags: WithFlags) = withFlags.applyToken(apiToken.value)

  private def gameToJson(
      g: Game,
      analysisOption: Option[Analysis],
      initialFen: Option[Fen.Full],
      withFlags: WithFlags
  ) =
    Json
      .obj(
        "id" -> g.id,
        "initialFen" -> initialFen,
        "rated" -> g.rated,
        "variant" -> g.variant.key,
        "speed" -> g.speed.key,
        "perf" -> g.perfKey,
        "createdAt" -> g.createdAt,
        "lastMoveAt" -> g.movedAt,
        "turns" -> g.ply,
        "color" -> g.turnColor.name,
        "status" -> g.status.name,
        "clock" -> g.clock.map { clock =>
          Json.obj(
            "initial" -> clock.limitSeconds,
            "increment" -> clock.incrementSeconds,
            "totalTime" -> clock.estimateTotalSeconds
          )
        },
        "daysPerTurn" -> g.daysPerTurn,
        "players" -> JsObject(g.players.mapList { p =>
          p.color.name -> Json
            .obj(
              "userId" -> p.userId,
              "rating" -> p.rating,
              "ratingDiff" -> p.ratingDiff
            )
            .add("name", p.name)
            .add("provisional" -> p.provisional)
            .add("moveCentis" -> withFlags.moveTimes.so:
              lila.game.GameExt.computeMoveTimes(g, p.color).map(_.map(_.centis)))
            .add("blurs" -> withFlags.blurs.option(p.blurs.nb))
            .add(
              "analysis" -> analysisOption
                .flatMap(analysisJson.player(g.pov(p.color).sideAndStart)(_, accuracy = none))
            )
        }),
        "analysis" -> analysisOption.ifTrue(withFlags.analysis).map(analysisJson.moves(_)),
        "moves" -> withFlags.moves.option(g.sans.mkString(" ")),
        "opening" -> (withFlags.opening.so(g.opening): Option[chess.opening.Opening.AtPly]),
        "fens" -> ((withFlags.fens && g.finished).so {
          chess
            .Position(g.variant, initialFen)
            .playPositions(g.sans)
            .toOption
            .map(boards => JsArray(boards.map(chess.format.Fen.writeBoard).map(Json.toJson)))
        }: Option[JsArray]),
        "winner" -> g.winnerColor.map(_.name),
        "url" -> makeUrl(g)
      )
      .noNull

object GameApi:

  case class WithFlags(
      analysis: Boolean = false,
      moves: Boolean = false,
      fens: Boolean = false,
      opening: Boolean = false,
      moveTimes: Boolean = false,
      blurs: Boolean = false,
      token: Option[String] = none
  ):
    def applyToken(validToken: String) = copy(blurs = token.has(validToken))
