package controllers

import chess.format.Forsyth
import chess.format.Forsyth.SituationPlus
import chess.Situation
import play.api.libs.json.Json
import play.api.mvc._

import lila.app._
import lila.game.{ GameRepo, Pov }
import views._

object UserAnalysis extends LilaController with TheftPrevention {

  def index = load("")

  def load(urlFen: String) = Open { implicit ctx =>
    val fenStr = Some(urlFen.trim.replace("_", " ")).filter(_.nonEmpty) orElse get("fen")
    val decodedFen = fenStr.map { java.net.URLDecoder.decode(_, "UTF-8").trim }.filter(_.nonEmpty)
    val situation = (decodedFen flatMap Forsyth.<<<) | SituationPlus(Situation(chess.variant.Standard), 1)
    val pov = makePov(situation)
    val orientation = get("color").flatMap(chess.Color.apply) | pov.color
    Env.api.roundApi.userAnalysisJson(pov, ctx.pref, decodedFen, orientation, owner = false) map { data =>
      Ok(html.board.userAnalysis(data, none))
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
      variant = chess.variant.Standard,
      source = lila.game.Source.Api,
      pgnImport = None,
      castles = from.situation.board.history.castles),
    from.situation.color)

  def game(id: String, color: String) = Open { implicit ctx =>
    OptionFuOk(GameRepo game id) { game =>
      GameRepo initialFen game.id flatMap { initialFen =>
        val pov = Pov(game, chess.Color(color == "white"))
        Env.api.roundApi.userAnalysisJson(pov, ctx.pref, initialFen, pov.color, owner = isMyPov(pov)) map { data =>
          html.board.userAnalysis(data, pov.some)
        }
      }
    }
  }

  def forecasts(fullId: String) = AuthBody(BodyParsers.parse.json) { implicit ctx =>
    me =>
      import lila.round.Forecast
      OptionFuResult(GameRepo pov fullId) { pov =>
        import lila.round.Forecast.forecastStepJsonFormat
        import lila.round.Forecast.forecastJsonWriter
        if (isTheft(pov)) fuccess(theftResponse)
        else ctx.body.body.validate[Forecast.Steps].fold(
          err => BadRequest(err.toString).fuccess,
          forecasts => Env.round.forecastApi.save(pov, forecasts) >>
            Env.round.forecastApi.load(pov) map {
              case None     => Ok(Json.obj("none" -> true))
              case Some(fc) => Ok(Json toJson fc) as JSON
            } recover {
              case Forecast.OutOfSync => Ok(Json.obj("reload" -> true))
            })
      }
  }
}
