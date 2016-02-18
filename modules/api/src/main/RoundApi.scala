package lila.api

import play.api.libs.json._

import chess.format.pgn.Pgn
import lila.analyse.{ Analysis, Info }
import lila.common.LightUser
import lila.common.PimpedJson._
import lila.game.{ Pov, Game, GameRepo }
import lila.pref.Pref
import lila.round.{ JsonView, Forecast }
import lila.security.Granter
import lila.simul.Simul
import lila.tournament.{ Tournament, SecondsToDoFirstMove, TourAndRanks }
import lila.user.User

private[api] final class RoundApi(
    jsonView: JsonView,
    noteApi: lila.round.NoteApi,
    forecastApi: lila.round.ForecastApi,
    analysisApi: AnalysisApi,
    bookmarkApi: lila.bookmark.BookmarkApi,
    getTourAndRanks: Game => Fu[Option[TourAndRanks]],
    getSimul: Simul.ID => Fu[Option[Simul]],
    lightUser: String => Option[LightUser]) {

  def player(pov: Pov, apiVersion: Int)(implicit ctx: Context): Fu[JsObject] =
    GameRepo.initialFen(pov.game) flatMap { initialFen =>
      jsonView.playerJson(pov, ctx.pref, apiVersion, ctx.me,
        initialFen = initialFen,
        withBlurs = ctx.me ?? Granter(_.ViewBlurs)) zip
        getTourAndRanks(pov.game) zip
        (pov.game.simulId ?? getSimul) zip
        (ctx.me ?? (me => noteApi.get(pov.gameId, me.id))) zip
        forecastApi.loadForDisplay(pov) map {
          case ((((json, tourOption), simulOption), note), forecast) => (
            blindMode _ compose
            withTournament(pov, tourOption)_ compose
            withSimul(pov, simulOption)_ compose
            withSteps(pov, none, initialFen)_ compose
            withNote(note)_ compose
            withBookmark(ctx.me ?? { bookmarkApi.bookmarked(pov.game, _) })_ compose
            withForecastCount(forecast.map(_.steps.size))_
          )(json)
        }
    }

  def watcher(pov: Pov, apiVersion: Int, tv: Option[lila.round.OnTv],
    analysis: Option[(Pgn, Analysis)] = None,
    initialFenO: Option[Option[String]] = None,
    withMoveTimes: Boolean = false)(implicit ctx: Context): Fu[JsObject] =
    initialFenO.fold(GameRepo initialFen pov.game)(fuccess) flatMap { initialFen =>
      jsonView.watcherJson(pov, ctx.pref, apiVersion, ctx.me, tv,
        withBlurs = ctx.me ?? Granter(_.ViewBlurs),
        initialFen = initialFen,
        withMoveTimes = withMoveTimes) zip
        getTourAndRanks(pov.game) zip
        (pov.game.simulId ?? getSimul) zip
        (ctx.me ?? (me => noteApi.get(pov.gameId, me.id))) map {
          case (((json, tourOption), simulOption), note) => (
            blindMode _ compose
            withTournament(pov, tourOption)_ compose
            withSimul(pov, simulOption)_ compose
            withNote(note)_ compose
            withBookmark(ctx.me ?? { bookmarkApi.bookmarked(pov.game, _) })_ compose
            withSteps(pov, analysis, initialFen)_ compose
            withAnalysis(analysis)_
          )(json)
        }
    }

  def userAnalysisJson(pov: Pov, pref: Pref, initialFen: Option[String], orientation: chess.Color, owner: Boolean) =
    owner.??(forecastApi loadForDisplay pov).flatMap { fco =>
      jsonView.userAnalysisJson(pov, pref, orientation, owner = owner) map
        withSteps(pov, none, initialFen)_ map
        withForecast(pov, owner, fco)_
    }

  private def withSteps(pov: Pov, a: Option[(Pgn, Analysis)], initialFen: Option[String])(obj: JsObject) =
    obj + ("steps" -> lila.round.StepBuilder(
      id = pov.game.id,
      pgnMoves = pov.game.pgnMoves,
      variant = pov.game.variant,
      a = a,
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

  private def withAnalysis(a: Option[(Pgn, Analysis)])(json: JsObject) = a.fold(json) {
    case (pgn, analysis) => json + ("analysis" -> Json.obj(
      "white" -> analysisApi.player(chess.Color.White)(analysis),
      "black" -> analysisApi.player(chess.Color.Black)(analysis)
    ))
  }

  private def withTournament(pov: Pov, tourOption: Option[TourAndRanks])(json: JsObject) =
    tourOption.fold(json) { data =>
      json + ("tournament" -> Json.obj(
        "id" -> data.tour.id,
        "name" -> data.tour.name,
        "running" -> data.tour.isStarted,
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
