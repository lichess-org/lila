package lila.api

import org.joda.time.DateTime
import play.api.libs.json._
import reactivemongo.api.bson._
import reactivemongo.api.ReadPreference

import lila.analyse.{ Analysis, JsonView => analysisJson }
import lila.common.config._
import lila.common.Json.jodaWrites
import lila.common.paginator.{ Paginator, PaginatorJson }
import lila.db.dsl._
import lila.db.paginator.{ Adapter, CachedAdapter }
import lila.game.BSONHandlers._
import lila.game.Game.{ BSONFields => G }
import lila.game.JsonView._
import lila.game.{ CrosstableApi, Game, PerfPicker }
import lila.user.User

final private[api] class GameApi(
    net: NetConfig,
    apiToken: Secret,
    gameRepo: lila.game.GameRepo,
    gameCache: lila.game.Cached,
    analysisRepo: lila.analyse.AnalysisRepo,
    crosstableApi: CrosstableApi
)(implicit ec: scala.concurrent.ExecutionContext) {

  import GameApi.WithFlags

  def byUser(
      user: User,
      rated: Option[Boolean],
      playing: Option[Boolean],
      analysed: Option[Boolean],
      withFlags: WithFlags,
      nb: MaxPerPage,
      page: Int
  ): Fu[JsObject] =
    Paginator(
      adapter = new CachedAdapter(
        adapter = new Adapter[Game](
          collection = gameRepo.coll,
          selector = {
            if (~playing) lila.game.Query.nowPlaying(user.id)
            else
              $doc(
                G.playerUids -> user.id,
                G.status $gte shogi.Status.Mate.id,
                G.analysed -> analysed.map[BSONValue] {
                  case true => BSONBoolean(true)
                  case _    => $doc("$exists" -> false)
                }
              )
          } ++ $doc(
            G.rated -> rated.map[BSONValue] {
              case true => BSONBoolean(true)
              case _    => $doc("$exists" -> false)
            }
          ),
          projection = none,
          sort = $doc(G.createdAt -> -1),
          readPreference = ReadPreference.secondaryPreferred
        ),
        nbResults =
          if (~playing) gameCache.nbPlaying(user.id)
          else
            fuccess {
              rated.fold(user.count.game) {
                case true => user.count.rated
                case _    => user.count.casual
              }
            }
      ),
      currentPage = page,
      maxPerPage = nb
    ) flatMap { pag =>
      gamesJson(withFlags = withFlags)(pag.currentPageResults) map { games =>
        PaginatorJson(pag withCurrentPageResults games)
      }
    }

  def one(id: String, withFlags: WithFlags): Fu[Option[JsObject]] =
    gameRepo game id flatMap {
      _ ?? { g =>
        gamesJson(withFlags)(List(g)) map (_.headOption)
      }
    }

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
      adapter = new CachedAdapter(
        adapter = new Adapter[Game](
          collection = gameRepo.coll,
          selector = {
            if (~playing) lila.game.Query.nowPlayingVs(users._1.id, users._2.id)
            else
              lila.game.Query.opponents(users._1, users._2) ++ $doc(
                G.status $gte shogi.Status.Mate.id,
                G.analysed -> analysed.map[BSONValue] {
                  case true => BSONBoolean(true)
                  case _    => $doc("$exists" -> false)
                }
              )
          } ++ $doc(
            G.rated -> rated.map[BSONValue] {
              case true => BSONBoolean(true)
              case _    => $doc("$exists" -> false)
            }
          ),
          projection = none,
          sort = $doc(G.createdAt -> -1),
          readPreference = ReadPreference.secondaryPreferred
        ),
        nbResults =
          if (~playing) gameCache.nbPlaying(users._1.id)
          else crosstableApi(users._1.id, users._2.id).dmap(_.nbGames)
      ),
      currentPage = page,
      maxPerPage = nb
    ) flatMap { pag =>
      gamesJson(withFlags.copy(sfens = false))(pag.currentPageResults) map { games =>
        PaginatorJson(pag withCurrentPageResults games)
      }
    }

  def byUsersVs(
      userIds: Iterable[User.ID],
      rated: Option[Boolean],
      playing: Option[Boolean],
      analysed: Option[Boolean],
      withFlags: WithFlags,
      since: DateTime,
      nb: MaxPerPage,
      page: Int
  ): Fu[JsObject] =
    Paginator(
      adapter = new Adapter[Game](
        collection = gameRepo.coll,
        selector = {
          if (~playing) lila.game.Query.nowPlayingVs(userIds)
          else
            lila.game.Query.opponents(userIds) ++ $doc(
              G.status $gte shogi.Status.Mate.id,
              G.analysed -> analysed.map[BSONValue] {
                case true => BSONBoolean(true)
                case _    => $doc("$exists" -> false)
              }
            )
        } ++ $doc(
          G.rated -> rated.map[BSONValue] {
            case true => BSONBoolean(true)
            case _    => $doc("$exists" -> false)
          },
          G.createdAt $gte since
        ),
        projection = none,
        sort = $doc(G.createdAt -> -1),
        readPreference = ReadPreference.secondaryPreferred
      ),
      currentPage = page,
      maxPerPage = nb
    ) flatMap { pag =>
      gamesJson(withFlags.copy(sfens = false))(pag.currentPageResults) map { games =>
        PaginatorJson(pag withCurrentPageResults games)
      }
    }

  private def makeUrl(game: Game) = s"${net.baseUrl}/${game.id}/${game.firstPlayer.color.name}"

  private def gamesJson(withFlags: WithFlags)(games: Seq[Game]): Fu[Seq[JsObject]] = {
    val allAnalysis =
      if (withFlags.analysis) analysisRepo byIds games.map(_.id)
      else fuccess(List.fill(games.size)(none[Analysis]))
    allAnalysis map { analysisOptions =>
      (games zip analysisOptions) map { case (g, analysisOption) =>
        gameToJson(g, analysisOption, checkToken(withFlags))
      }
    }
  }

  private def checkToken(withFlags: WithFlags) = withFlags applyToken apiToken.value

  private def gameToJson(
      g: Game,
      analysisOption: Option[Analysis],
      withFlags: WithFlags
  ) =
    Json
      .obj(
        "id"          -> g.id,
        "initialSfen" -> g.initialSfen,
        "rated"       -> g.rated,
        "variant"     -> g.variant.key,
        "speed"       -> g.speed.key,
        "perf"        -> PerfPicker.key(g),
        "createdAt"   -> g.createdAt,
        "lastMoveAt"  -> g.movedAt,
        "plies"       -> g.plies,
        "color"       -> g.turnColor.name,
        "status"      -> g.status.name,
        "clock" -> g.clock.map { clock =>
          Json.obj(
            "initial"   -> clock.limitSeconds,
            "increment" -> clock.incrementSeconds,
            "byoyomi"   -> clock.byoyomiSeconds,
            "periods"   -> clock.periodsTotal,
            "totalTime" -> clock.estimateTotalSeconds
          )
        },
        "daysPerTurn" -> g.daysPerTurn,
        "players" -> JsObject(g.players map { p =>
          p.color.name -> Json
            .obj(
              "userId"     -> p.userId,
              "rating"     -> p.rating,
              "ratingDiff" -> p.ratingDiff
            )
            .add("name", p.name)
            .add("provisional" -> p.provisional)
            .add("moveCentis" -> withFlags.moveTimes ?? g.moveTimes(p.color).map(_.map(_.centis)))
            .add("blurs" -> withFlags.blurs.option(p.blurs.nb))
            .add("analysis" -> analysisOption.flatMap(analysisJson.player(g pov p.color)))
        }),
        "analysis" -> analysisOption.ifTrue(withFlags.analysis).map(analysisJson.moves(_)),
        "moves"    -> withFlags.moves.option(g.usis.map(_.usi) mkString " "),
        "sfens" -> (withFlags.sfens && g.finished) ?? {
          shogi.Replay
            .situations(
              usis = g.usis,
              initialSfen = g.initialSfen,
              variant = g.variant
            )
            .toOption map { sits =>
            JsArray(sits.toList.map(_.toSfen.value) map JsString.apply)
          }
        },
        "winner" -> g.winnerColor.map(_.name),
        "url"    -> makeUrl(g)
      )
      .noNull
}

object GameApi {

  case class WithFlags(
      analysis: Boolean = false,
      moves: Boolean = false,
      sfens: Boolean = false,
      moveTimes: Boolean = false,
      blurs: Boolean = false,
      token: Option[String] = none
  ) {

    def applyToken(validToken: String) =
      copy(
        blurs = token has validToken
      )
  }
}
