package lila.api

import play.api.libs.json._

import chess.format.pgn.Pgn
import lila.analyse.{ Analysis, Info }
import lila.common.LightUser
import lila.common.PimpedJson._
import lila.game.{ Pov, Game, GameRepo }
import lila.pref.Pref
import lila.relay.Relay
import lila.round.JsonView
import lila.security.Granter
import lila.simul.Simul
import lila.tournament.{ TournamentRepo, Tournament }
import lila.user.User

private[api] final class RoundApi(
    jsonView: JsonView,
    noteApi: lila.round.NoteApi,
    analysisApi: AnalysisApi,
    getRelay: String => Fu[Option[Relay]],
    getSimul: Simul.ID => Fu[Option[Simul]],
    lightUser: String => Option[LightUser]) {

  def player(pov: Pov, apiVersion: Int)(implicit ctx: Context): Fu[JsObject] =
    GameRepo.initialFen(pov.game) flatMap { initialFen =>
      jsonView.playerJson(pov, ctx.pref, apiVersion, ctx.me,
        initialFen = initialFen,
        withBlurs = ctx.me ?? Granter(_.ViewBlurs)) zip
        (pov.game.tournamentId ?? TournamentRepo.byId) zip
        (pov.game.simulId ?? getSimul) zip
        (ctx.me ?? (me => noteApi.get(pov.gameId, me.id))) map {
          case (((json, tourOption), simulOption), note) => (
            blindMode _ compose
            withTournament(pov, tourOption)_ compose
            withSimul(pov, simulOption)_ compose
            withSteps(pov, none, initialFen)_ compose
            withNote(note)_
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
        (pov.game.tournamentId ?? TournamentRepo.byId) zip
        (pov.game.simulId ?? getSimul) zip
        (pov.game.relayId ?? getRelay) zip
        (ctx.me ?? (me => noteApi.get(pov.gameId, me.id))) map {
          case ((((json, tourOption), simulOption), relayOption), note) => (
            blindMode _ compose
            withTournament(pov, tourOption)_ compose
            withSimul(pov, simulOption)_ compose
            withRelay(pov, relayOption)_ compose
            withNote(note)_ compose
            withSteps(pov, analysis, initialFen)_ compose
            withAnalysis(analysis)_
          )(json)
        }
    }

  def userAnalysisJson(pov: Pov, pref: Pref, initialFen: Option[String], orientation: chess.Color) =
    jsonView.userAnalysisJson(pov, pref, orientation) map withSteps(pov, none, initialFen)_

  private def withSteps(pov: Pov, a: Option[(Pgn, Analysis)], initialFen: Option[String])(obj: JsObject) =
    obj + ("steps" -> lila.round.StepBuilder(
      id = pov.game.id,
      pgnMoves = pov.game.pgnMoves,
      variant = pov.game.variant,
      a = a,
      initialFen = initialFen | chess.format.Forsyth.initial))

  private def withNote(note: String)(json: JsObject) =
    if (note.isEmpty) json else json + ("note" -> JsString(note))

  private def withAnalysis(a: Option[(Pgn, Analysis)])(json: JsObject) = a.fold(json) {
    case (pgn, analysis) => json + ("analysis" -> Json.obj(
      "white" -> analysisApi.player(chess.Color.White)(analysis),
      "black" -> analysisApi.player(chess.Color.Black)(analysis)
    ))
  }

  private def withTournament(pov: Pov, tourOption: Option[Tournament])(json: JsObject) =
    tourOption.fold(json) { tour =>
      json + ("tournament" -> Json.obj(
        "id" -> tour.id,
        "name" -> tour.name,
        "running" -> tour.isStarted,
        "berserkable" -> tour.berserkable
      // "berserk1" -> false, //pairing.map(_.berserk1).filter(0!=),
      // "berserk2" -> false //pairing.map(_.berserk2).filter(0!=)
      ))
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

  private def relayClock(game: Game, r: lila.game.Relay, running: Boolean) = {
    val tickingColor = game.playerHasMoved(chess.White) option game.turnColor
    Json.obj(
      "running" -> running,
      "white" -> r.remainingSecondsOf(chess.White, tickingColor),
      "black" -> r.remainingSecondsOf(chess.Black, tickingColor))
  }

  private def relayPlayer(p: lila.game.Relay.Player) = Json.obj(
    "name" -> p.name,
    "title" -> p.title,
    "rating" -> p.rating).noNull

  private def withRelay(pov: Pov, relayOption: Option[Relay])(json: JsObject) =
    (pov.game.relay |@| relayOption).tupled.fold(json) {
      case (meta, relay) => json + ("relay" -> Json.obj(
        "id" -> relay.id,
        "name" -> relay.name,
        "status" -> relay.status.id,
        "white" -> relayPlayer(meta.white),
        "black" -> relayPlayer(meta.black),
        "clock" -> relayClock(pov.game, meta, true)
      ))
    }

  private def blindMode(js: JsObject)(implicit ctx: Context) =
    ctx.blindMode.fold(js + ("blind" -> JsBoolean(true)), js)
}
