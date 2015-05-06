package lila.api

import play.api.libs.json._

import chess.format.Forsyth
import chess.format.pgn.Pgn
import chess.variant.Variant
import lila.analyse.{ Analysis, Info }
import lila.common.LightUser
import lila.common.PimpedJson._
import lila.game.Step.stepJsonWriter
import lila.game.{ Pov, Game, Step, GameRepo }
import lila.pref.Pref
import lila.round.JsonView
import lila.security.Granter
import lila.simul.Simul
import lila.tournament.{ Tournament, TournamentRepo }
import lila.user.User

private[api] final class RoundApi(
    jsonView: JsonView,
    noteApi: lila.round.NoteApi,
    analysisApi: AnalysisApi,
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
            withSteps(pov.game, none, initialFen, false)_ compose
            withNote(note)_
          )(json)
        }
    }

  def watcher(pov: Pov, apiVersion: Int, tv: Option[Boolean],
    analysis: Option[(Pgn, Analysis)] = None,
    initialFenO: Option[Option[String]] = None,
    withMoveTimes: Boolean = false,
    withPossibleMoves: Boolean = false)(implicit ctx: Context): Fu[JsObject] =
    initialFenO.fold(GameRepo initialFen pov.game)(fuccess) flatMap { initialFen =>
      jsonView.watcherJson(pov, ctx.pref, apiVersion, ctx.me, tv,
        withBlurs = ctx.me ?? Granter(_.ViewBlurs),
        initialFen = initialFen,
        withMoveTimes = withMoveTimes) zip
        (pov.game.tournamentId ?? TournamentRepo.byId) zip
        (pov.game.simulId ?? getSimul) zip
        (ctx.me ?? (me => noteApi.get(pov.gameId, me.id))) map {
          case (((json, tourOption), simulOption), note) => (
            blindMode _ compose
            withTournament(pov, tourOption)_ compose
            withSimul(pov, simulOption)_ compose
            withNote(note)_ compose
            withSteps(pov.game, analysis, initialFen, withPossibleMoves)_ compose
            withAnalysis(analysis)_
          )(json)
        }
    }

  private def withSteps(game: Game, a: Option[(Pgn, Analysis)], initialFen: Option[String], possibleMoves: Boolean)(json: JsObject) =
    json ++ Json.obj("steps" -> {
      val steps = chess.Replay.games(game.pgnMoves, initialFen, game.variant).err.map { g =>
        Step(
          ply = g.turns,
          move = for {
            pos <- g.board.history.lastMove
            san <- g.pgnMoves.lastOption
          } yield Step.Move(pos._1, pos._2, san),
          fen = Forsyth >> g,
          check = g.situation.check,
          dests = (possibleMoves && !g.situation.end) ?? g.situation.destinations)
      }
      a.fold(steps) {
        case (pgn, analysis) => applyAnalysis(steps, pgn, analysis, game.variant, possibleMoves)
      }
    })

  private def applyAnalysis(
    steps: List[Step],
    pgn: Pgn,
    analysis: Analysis,
    variant: Variant,
    possibleMoves: Boolean): List[Step] =
    analysis.advices.foldLeft(steps) {
      case (steps, ad) => (for {
        before <- steps lift (ad.ply - 1)
        after <- steps lift ad.ply
      } yield steps.updated(ad.ply, after.copy(
        nag = ad.nag.symbol.some,
        comments = ad.makeComment(false, true) :: after.comments,
        variations = if (ad.info.variation.isEmpty) after.variations
        else makeVariation(before, ad.info, variant, possibleMoves) :: after.variations))
      ) | steps
    }

  private def makeVariation(fromStep: Step, info: Info, variant: Variant, possibleMoves: Boolean): List[Step] =
    chess.Replay.games(
      info.variation take 20,
      fromStep.fen.some,
      variant
    ).err.drop(1).map { g =>
        Step(
          ply = g.turns,
          move = for {
            pos <- g.board.history.lastMove
            (orig, dest) = pos
            san <- g.pgnMoves.lastOption
          } yield Step.Move(orig, dest, san),
          fen = Forsyth >> g,
          check = g.situation.check,
          dests = (possibleMoves && !g.situation.end) ?? g.situation.destinations)
      }

  private def withNote(note: String)(json: JsObject) =
    if (note.isEmpty) json else json + ("note" -> JsString(note))

  private def withAnalysis(a: Option[(Pgn, Analysis)])(json: JsObject) = a.fold(json) {
    case (pgn, analysis) => json + ("analysis" -> Json.obj(
      "moves" -> analysisApi.game(analysis, pgn),
      "white" -> analysisApi.player(chess.Color.White)(analysis),
      "black" -> analysisApi.player(chess.Color.Black)(analysis)
    ))
  }

  private def withTournament(pov: Pov, tourOption: Option[Tournament])(json: JsObject) =
    tourOption.fold(json) { tour =>
      val pairing = tour.pairingOfGameId(pov.gameId)
      json + ("tournament" -> Json.obj(
        "id" -> tour.id,
        "name" -> tour.name,
        "running" -> tour.isRunning,
        "berserkable" -> tour.berserkable,
        "berserk1" -> pairing.map(_.berserk1).filter(0!=),
        "berserk2" -> pairing.map(_.berserk2).filter(0!=)
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
