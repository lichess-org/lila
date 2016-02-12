package controllers

import chess.format.Forsyth
import chess.format.Forsyth.SituationPlus
import chess.Situation
import chess.variant.Standard
import chess.variant.Variant
import play.api.libs.json.Json
import play.api.mvc._
import scala.concurrent.duration._

import lila.app._
import lila.game.{ GameRepo, Pov }
import lila.round.Forecast.{ forecastStepJsonFormat, forecastJsonWriter }
import views._

object UserAnalysis extends LilaController with TheftPrevention {

  def index = load("", Standard)

  def variantOrLoad(something: String) =
    Variant.byKey get something match {
      case Some(variant) => load("", variant)
      case None          => load(something, Standard)
    }

  def load(urlFen: String, variant: Variant) = Open { implicit ctx =>
    val fenStr = Some(urlFen.trim.replace("_", " ")).filter(_.nonEmpty) orElse get("fen")
    val decodedFen = fenStr.map { java.net.URLDecoder.decode(_, "UTF-8").trim }.filter(_.nonEmpty)
    val situation = (decodedFen flatMap Forsyth.<<<) | SituationPlus(Situation(variant), 1)
    val pov = makePov(situation)
    val orientation = get("color").flatMap(chess.Color.apply) | pov.color
    Env.api.roundApi.userAnalysisJson(pov, ctx.pref, decodedFen, orientation, owner = false) map { data =>
      Ok(html.board.userAnalysis(data, variant))
    }
  }

  private def makePov(from: SituationPlus) = lila.game.Pov(
    lila.game.Game.make(
      game = chess.Game(
        board = from.situation.board,
        player = from.situation.color,
        turns = from.turns),
      whitePlayer = lila.game.Player.white,
      blackPlayer = lila.game.Player.black,
      mode = chess.Mode.Casual,
      variant = from.situation.board.variant,
      source = lila.game.Source.Api,
      pgnImport = None).copy(id = "synthetic"),
    from.situation.color)

  def game(id: String, color: String) = Open { implicit ctx =>
    OptionFuResult(GameRepo game id) { game =>
      GameRepo initialFen game.id flatMap { initialFen =>
        val pov = Pov(game, chess.Color(color == "white"))
        Env.api.roundApi.userAnalysisJson(pov, ctx.pref, initialFen, pov.color, owner = isMyPov(pov)) map { data =>
          Ok(html.board.userAnalysis(data, pov.game.variant))
        }
      } map NoCache
    }
  }

  def forecasts(fullId: String) = AuthBody(BodyParsers.parse.json) { implicit ctx =>
    me =>
      import lila.round.Forecast
      OptionFuResult(GameRepo pov fullId) { pov =>
        if (isTheft(pov)) fuccess(theftResponse)
        else ctx.body.body.validate[Forecast.Steps].fold(
          err => BadRequest(err.toString).fuccess,
          forecasts => Env.round.forecastApi.save(pov, forecasts) >>
            Env.round.forecastApi.loadForDisplay(pov) map {
              case None     => Ok(Json.obj("none" -> true))
              case Some(fc) => Ok(Json toJson fc) as JSON
            } recover {
              case Forecast.OutOfSync => Ok(Json.obj("reload" -> true))
            })
      }
  }

  def forecastsOnMyTurn(fullId: String, uci: String) = AuthBody(BodyParsers.parse.json) { implicit ctx =>
    me =>
      import lila.round.Forecast
      OptionFuResult(GameRepo pov fullId) { pov =>
        if (isTheft(pov)) fuccess(theftResponse)
        else {
          ctx.body.body.validate[Forecast.Steps].fold(
            err => BadRequest(err.toString).fuccess,
            forecasts => {
              def wait = 50 + (Forecast maxPlies forecasts min 10) * 50
              Env.round.forecastApi.playAndSave(pov, uci, forecasts) >>
                Env.current.scheduler.after(wait.millis) {
                  Ok(Json.obj("reload" -> true))
                }
            }
          )
        }
      }
  }
}
