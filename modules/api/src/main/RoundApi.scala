package lila.api

import play.api.libs.json._

import lila.analyse.{ JsonView => analysisJson, Analysis, Info }
import lila.common.PimpedJson._
import lila.common.{ LightUser, ApiVersion }
import lila.game.{ Pov, Game, GameRepo }
import lila.pref.Pref
import lila.round.{ JsonView, Forecast }
import lila.security.Granter
import lila.simul.Simul
import lila.tournament.{ Tournament, SecondsToDoFirstMove, TourAndRanks }
import lila.tree.Node.partitionTreeJsonWriter
import lila.user.User

private[api] final class RoundApi(
    jsonView: JsonView,
    noteApi: lila.round.NoteApi,
    forecastApi: lila.round.ForecastApi,
    bookmarkApi: lila.bookmark.BookmarkApi,
    getTourAndRanks: Game => Fu[Option[TourAndRanks]],
    getSimul: Simul.ID => Fu[Option[Simul]],
    lightUser: String => Option[LightUser]) {

  def player(pov: Pov, apiVersion: ApiVersion)(implicit ctx: Context): Fu[JsObject] =
    GameRepo.initialFen(pov.game) flatMap { initialFen =>
      jsonView.playerJson(pov, ctx.pref, apiVersion, ctx.me,
        withBlurs = ctx.me ?? Granter(_.ViewBlurs),
        initialFen = initialFen) zip
        getTourAndRanks(pov.game) zip
        (pov.game.simulId ?? getSimul) zip
        (ctx.me.ifTrue(ctx.isMobileApi) ?? (me => noteApi.get(pov.gameId, me.id))) zip
        forecastApi.loadForDisplay(pov) zip
        bookmarkApi.exists(pov.game, ctx.me) map {
          case (((((json, tourOption), simulOption), note), forecast), bookmarked) => (
            blindMode _ compose
            withTournament(pov, tourOption)_ compose
            withSimul(pov, simulOption)_ compose
            withSteps(pov, initialFen)_ compose
            withNote(note)_ compose
            withBookmark(bookmarked)_ compose
            withForecastCount(forecast.map(_.steps.size))_
          )(json)
        }
    }

  def watcher(pov: Pov, apiVersion: ApiVersion, tv: Option[lila.round.OnTv],
    initialFenO: Option[Option[String]] = None)(implicit ctx: Context): Fu[JsObject] =
    initialFenO.fold(GameRepo initialFen pov.game)(fuccess) flatMap { initialFen =>
      jsonView.watcherJson(pov, ctx.pref, apiVersion, ctx.me, tv,
        withBlurs = ctx.me ?? Granter(_.ViewBlurs),
        initialFen = initialFen,
        withMoveTimes = false,
        withDivision = false) zip
        getTourAndRanks(pov.game) zip
        (pov.game.simulId ?? getSimul) zip
        (ctx.me.ifTrue(ctx.isMobileApi) ?? (me => noteApi.get(pov.gameId, me.id))) zip
        bookmarkApi.exists(pov.game, ctx.me) map {
          case ((((json, tourOption), simulOption), note), bookmarked) => (
            blindMode _ compose
            withTournament(pov, tourOption)_ compose
            withSimul(pov, simulOption)_ compose
            withNote(note)_ compose
            withBookmark(bookmarked)_ compose
            withSteps(pov, initialFen)_
          )(json)
        }
    }

  def review(pov: Pov, apiVersion: ApiVersion,
    tv: Option[lila.round.OnTv] = None,
    analysis: Option[Analysis] = None,
    initialFenO: Option[Option[String]] = None,
    withMoveTimes: Boolean = false,
    withDivision: Boolean = false,
    withOpening: Boolean = false)(implicit ctx: Context): Fu[JsObject] =
    initialFenO.fold(GameRepo initialFen pov.game)(fuccess) flatMap { initialFen =>
      jsonView.watcherJson(pov, ctx.pref, apiVersion, ctx.me, tv,
        withBlurs = ctx.me ?? Granter(_.ViewBlurs),
        initialFen = initialFen,
        withMoveTimes = withMoveTimes,
        withDivision = withDivision) zip
        getTourAndRanks(pov.game) zip
        (pov.game.simulId ?? getSimul) zip
        (ctx.me.ifTrue(ctx.isMobileApi) ?? (me => noteApi.get(pov.gameId, me.id))) zip
        bookmarkApi.exists(pov.game, ctx.me) map {
          case ((((json, tourOption), simulOption), note), bookmarked) => (
            blindMode _ compose
            withTournament(pov, tourOption)_ compose
            withSimul(pov, simulOption)_ compose
            withNote(note)_ compose
            withBookmark(bookmarked)_ compose
            withTree(pov, analysis, initialFen, withOpening = withOpening)_ compose
            withAnalysis(pov.game, analysis)_
          )(json)
        }
    }

  def userAnalysisJson(pov: Pov, pref: Pref, initialFen: Option[String], orientation: chess.Color, owner: Boolean) =
    owner.??(forecastApi loadForDisplay pov).flatMap { fco =>
      jsonView.userAnalysisJson(pov, pref, orientation, owner = owner) map
        withTree(pov, analysis = none, initialFen, withOpening = true)_ map
        withForecast(pov, owner, fco)_
    }

  def freeStudyJson(pov: Pov, pref: Pref, initialFen: Option[String], orientation: chess.Color) =
    jsonView.userAnalysisJson(pov, pref, orientation, owner = false) map
      withTree(pov, analysis = none, initialFen, withOpening = true)_

  private def withTree(pov: Pov, analysis: Option[Analysis], initialFen: Option[String], withOpening: Boolean)(obj: JsObject) =
    obj + ("treeParts" -> partitionTreeJsonWriter.writes(lila.round.TreeBuilder(
      id = pov.game.id,
      pgnMoves = pov.game.pgnMoves,
      variant = pov.game.variant,
      analysis = analysis,
      initialFen = initialFen | pov.game.variant.initialFen,
      withOpening = withOpening)))

  private def withSteps(pov: Pov, initialFen: Option[String])(obj: JsObject) =
    obj + ("steps" -> lila.round.StepBuilder(
      id = pov.game.id,
      pgnMoves = pov.game.pgnMoves,
      variant = pov.game.variant,
      initialFen = initialFen | pov.game.variant.initialFen))

  private def withNote(note: String)(json: JsObject) =
    if (note.isEmpty) json else json + ("note" -> JsString(note))

  private def withBookmark(v: Boolean)(json: JsObject) =
    if (v) json + ("bookmarked" -> JsBoolean(true)) else json

  private def withForecastCount(count: Option[Int])(json: JsObject) =
    count.filter(0 !=).fold(json) { c =>
      json + ("forecastCount" -> JsNumber(c))
    }

  private def withForecast(pov: Pov, owner: Boolean, fco: Option[Forecast])(json: JsObject) =
    if (pov.game.forecastable && owner) json + (
      "forecast" -> {
        if (pov.forecastable) fco.fold[JsValue](Json.obj("none" -> true)) { fc =>
          import Forecast.forecastJsonWriter
          Json toJson fc
        }
        else Json.obj("onMyTurn" -> true)
      })
    else json

  private def withAnalysis(g: Game, o: Option[Analysis])(json: JsObject) = o.fold(json) { a =>
    json + ("analysis" -> analysisJson.bothPlayers(g, a))
  }

  private def withTournament(pov: Pov, tourOption: Option[TourAndRanks])(json: JsObject) =
    tourOption.fold(json) { data =>
      json + ("tournament" -> Json.obj(
        "id" -> data.tour.id,
        "name" -> data.tour.name,
        "running" -> data.tour.isStarted,
        "secondsToFinish" -> data.tour.isStarted.option(data.tour.secondsToFinish),
        "berserkable" -> data.tour.isStarted.option(data.tour.berserkable),
        "nbSecondsForFirstMove" -> data.tour.isStarted.option {
          SecondsToDoFirstMove.secondsToMoveFor(data.tour)
        },
        "ranks" -> data.tour.isStarted.option(Json.obj(
          "white" -> data.whiteRank,
          "black" -> data.blackRank))
      ).noNull)
    }

  private def withSimul(pov: Pov, simulOption: Option[Simul])(json: JsObject) =
    simulOption.fold(json) { simul =>
      json + ("simul" -> Json.obj(
        "id" -> simul.id,
        "hostId" -> simul.hostId,
        "name" -> simul.name,
        "nbPlaying" -> simul.playingPairings.size
      ))
    }

  private def blindMode(js: JsObject)(implicit ctx: Context) =
    ctx.blindMode.fold(js + ("blind" -> JsBoolean(true)), js)
}
