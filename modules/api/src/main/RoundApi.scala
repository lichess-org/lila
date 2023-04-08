package lila.api

import chess.format.Fen
import play.api.i18n.Lang
import play.api.libs.json.*

import lila.analyse.{ Analysis, JsonView as analysisJson }
import lila.common.ApiVersion
import lila.common.Json.given
import lila.game.{ Game, Pov }
import lila.pref.Pref
import lila.puzzle.PuzzleOpening
import lila.round.JsonView.WithFlags
import lila.round.{ Forecast, JsonView }
import lila.security.Granter
import lila.simul.Simul
import lila.swiss.{ GameView as SwissView }
import lila.tournament.{ GameView as TourView }
import lila.tree.Node.partitionTreeJsonWriter
import lila.user.User

final private[api] class RoundApi(
    jsonView: JsonView,
    noteApi: lila.round.NoteApi,
    forecastApi: lila.round.ForecastApi,
    bookmarkApi: lila.bookmark.BookmarkApi,
    gameRepo: lila.game.GameRepo,
    tourApi: lila.tournament.TournamentApi,
    swissApi: lila.swiss.SwissApi,
    simulApi: lila.simul.SimulApi,
    puzzleOpeningApi: lila.puzzle.PuzzleOpeningApi,
    externalEngineApi: lila.analyse.ExternalEngineApi,
    getTeamName: lila.team.GetTeamNameSync,
    getLightUser: lila.common.LightUser.GetterSync
)(using Executor):

  def player(pov: Pov, tour: Option[TourView], apiVersion: ApiVersion)(using
      ctx: Context
  ): Fu[JsObject] =
    gameRepo
      .initialFen(pov.game)
      .flatMap { initialFen =>
        given play.api.i18n.Lang = ctx.lang
        jsonView.playerJson(
          pov,
          ctx.pref,
          apiVersion,
          ctx.me.map(Right.apply),
          withFlags = ctxFlags,
          initialFen = initialFen,
          nvui = ctx.blind
        ) zip
          (pov.game.simulId ?? simulApi.find) zip
          swissApi.gameView(pov) zip
          (ctx.userId.ifTrue(ctx.isMobileApi) ?? (noteApi.get(pov.gameId, _))) zip
          forecastApi.loadForDisplay(pov) zip
          bookmarkApi.exists(pov.game, ctx.me) map {
            case (((((json, simul), swiss), note), forecast), bookmarked) =>
              (
                withTournament(pov, tour) compose
                  withSwiss(swiss) compose
                  withSimul(simul) compose
                  withSteps(pov, initialFen) compose
                  withNote(note) compose
                  withBookmark(bookmarked) compose
                  withForecastCount(forecast.map(_.steps.size))
              )(json)
          }
      }
      .mon(_.round.api.player)

  def watcher(
      pov: Pov,
      tour: Option[TourView],
      apiVersion: ApiVersion,
      tv: Option[lila.round.OnTv],
      initialFenO: Option[Option[Fen.Epd]] = None
  )(using ctx: Context): Fu[JsObject] =
    initialFenO
      .fold(gameRepo initialFen pov.game)(fuccess)
      .flatMap { initialFen =>
        given play.api.i18n.Lang = ctx.lang
        jsonView.watcherJson(
          pov,
          ctx.pref,
          apiVersion,
          ctx.me,
          tv,
          initialFen = initialFen,
          withFlags = ctxFlags
        ) zip
          (pov.game.simulId ?? simulApi.find) zip
          swissApi.gameView(pov) zip
          (ctx.me.ifTrue(ctx.isMobileApi) ?? (me => noteApi.get(pov.gameId, me.id))) zip
          bookmarkApi.exists(pov.game, ctx.me) map { case ((((json, simul), swiss), note), bookmarked) =>
            (
              withTournament(pov, tour) compose
                withSwiss(swiss) compose
                withSimul(simul) compose
                withNote(note) compose
                withBookmark(bookmarked) compose
                withSteps(pov, initialFen)
            )(json)
          }
      }
      .mon(_.round.api.watcher)

  private def ctxFlags(using ctx: Context) =
    WithFlags(blurs = ctx.me ?? Granter(_.ViewBlurs), rating = ctx.pref.showRatings)

  def review(
      pov: Pov,
      apiVersion: ApiVersion,
      tv: Option[lila.round.OnTv] = None,
      analysis: Option[Analysis] = None,
      initialFen: Option[Fen.Epd],
      withFlags: WithFlags,
      owner: Boolean = false
  )(using ctx: Context): Fu[JsObject] = withExternalEngines(ctx.me) {
    given play.api.i18n.Lang = ctx.lang
    jsonView.watcherJson(
      pov,
      ctx.pref,
      apiVersion,
      ctx.me,
      tv,
      initialFen = initialFen,
      withFlags = withFlags.copy(blurs = ctx.me ?? Granter(_.ViewBlurs))
    ) zip
      tourApi.gameView.analysis(pov.game) zip
      (pov.game.simulId ?? simulApi.find) zip
      swissApi.gameView(pov) zip
      ctx.userId.ifTrue(ctx.isMobileApi).?? {
        noteApi.get(pov.gameId, _)
      } zip
      owner.??(forecastApi loadForDisplay pov) zip
      withFlags.puzzles.??(pov.game.opening.map(_.opening)).??(puzzleOpeningApi.getClosestTo) zip
      bookmarkApi.exists(pov.game, ctx.me) map {
        case (((((((json, tour), simul), swiss), note), fco), puzzleOpening), bookmarked) =>
          (
            withTournament(pov, tour) compose
              withSwiss(swiss) compose
              withSimul(simul) compose
              withNote(note) compose
              withBookmark(bookmarked) compose
              withTree(pov, analysis, initialFen, withFlags) compose
              withAnalysis(pov.game, analysis) compose
              withForecast(pov, owner, fco) compose
              withPuzzleOpening(puzzleOpening)
          )(json)
      }
  }
    .mon(_.round.api.watcher)

  def userAnalysisJson(
      pov: Pov,
      pref: Pref,
      initialFen: Option[Fen.Epd],
      orientation: chess.Color,
      owner: Boolean,
      me: Option[User]
  ) =
    withExternalEngines(me) {
      owner.??(forecastApi loadForDisplay pov).map { fco =>
        withForecast(pov, owner, fco) {
          withTree(pov, analysis = none, initialFen, WithFlags(opening = true)) {
            jsonView.userAnalysisJson(
              pov,
              pref,
              initialFen,
              orientation,
              owner = owner
            )
          }
        }
      }
    }

  private def withTree(
      pov: Pov,
      analysis: Option[Analysis],
      initialFen: Option[Fen.Epd],
      withFlags: WithFlags
  )(
      obj: JsObject
  ) =
    obj + ("treeParts" -> partitionTreeJsonWriter.writes(
      lila.round.TreeBuilder(pov.game, analysis, initialFen | pov.game.variant.initialFen, withFlags)
    ))

  private def withSteps(pov: Pov, initialFen: Option[Fen.Epd])(obj: JsObject) =
    obj + ("steps" -> lila.round.StepBuilder(
      id = pov.gameId,
      sans = pov.game.sans,
      variant = pov.game.variant,
      initialFen = initialFen | pov.game.variant.initialFen
    ))

  private def withNote(note: String)(json: JsObject) =
    if (note.isEmpty) json else json + ("note" -> JsString(note))

  private def withBookmark(v: Boolean)(json: JsObject) =
    json.add("bookmarked" -> v)

  private def withForecastCount(count: Option[Int])(json: JsObject) =
    count.filter(0 !=).fold(json) { c =>
      json + ("forecastCount" -> JsNumber(c))
    }

  private def withPuzzleOpening(
      opening: Option[Either[PuzzleOpening.FamilyWithCount, PuzzleOpening.WithCount]]
  )(json: JsObject) =
    json.add(
      "puzzle" -> opening
        .map {
          case Left(p)  => (p.family.key.toString, p.family.name.value, p.count)
          case Right(p) => (p.opening.key.toString, p.opening.name.value, p.count)
        }
        .map { case (key, name, count) =>
          Json.obj("key" -> key, "name" -> name, "count" -> count)
        }
    )

  private def withForecast(pov: Pov, owner: Boolean, fco: Option[Forecast])(json: JsObject) =
    if (pov.game.forecastable && owner)
      json + (
        "forecast" -> {
          if (pov.forecastable) fco.fold[JsValue](Json.obj("none" -> true)) { fc =>
            import Forecast.given
            Json toJson fc
          }
          else Json.obj("onMyTurn" -> true)
        }
      )
    else json

  private def withAnalysis(g: Game, o: Option[Analysis])(json: JsObject) =
    json.add(
      "analysis",
      o.map { analysisJson.bothPlayers(g.startedAtPly, _) }
    )

  private def withExternalEngines(me: Option[User])(jsonFu: Fu[JsObject]): Fu[JsObject] =
    jsonFu flatMap { withExternalEngines(me, _) }

  def withExternalEngines(me: Option[User], json: JsObject): Fu[JsObject] =
    (me ?? externalEngineApi.list) map { engines =>
      json.add("externalEngines", engines.nonEmpty.option(engines))
    }

  def withTournament(pov: Pov, viewO: Option[TourView])(json: JsObject)(using lang: Lang) =
    json.add("tournament" -> viewO.map { v =>
      Json
        .obj(
          "id"      -> v.tour.id,
          "name"    -> v.tour.name(full = false),
          "running" -> v.tour.isStarted
        )
        .add("secondsToFinish" -> v.tour.isStarted.option(v.tour.secondsToFinish))
        .add("berserkable" -> v.tour.isStarted.option(v.tour.berserkable))
        // mobile app API BC / should use game.expiration instead
        .add("nbSecondsForFirstMove" -> v.tour.isStarted.option {
          pov.game.timeForFirstMove.toSeconds
        })
        .add("ranks" -> v.ranks.map { r =>
          Json.obj(
            "white" -> r.whiteRank,
            "black" -> r.blackRank
          )
        })
        .add(
          "top",
          v.top.map {
            lila.tournament.JsonView.top(_, getLightUser)
          }
        )
        .add(
          "team",
          v.teamVs.map(_.teams(pov.color)) map { id =>
            Json.obj("name" -> getTeamName(id))
          }
        )
    })

  def withSwiss(sv: Option[SwissView])(json: JsObject) =
    json.add("swiss" -> sv.map { s =>
      Json
        .obj(
          "id"      -> s.swiss.id,
          "running" -> s.swiss.isStarted
        )
        .add("ranks" -> s.ranks.map { r =>
          Json.obj(
            "white" -> r.whiteRank,
            "black" -> r.blackRank
          )
        })
    })

  private def withSimul(simulOption: Option[Simul])(json: JsObject) =
    json.add(
      "simul",
      simulOption.map { simul =>
        Json.obj(
          "id"        -> simul.id,
          "hostId"    -> simul.hostId,
          "name"      -> simul.name,
          "nbPlaying" -> simul.playingPairings.size
        )
      }
    )
