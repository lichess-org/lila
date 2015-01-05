package controllers

import chess.format.Forsyth
import chess.{ Situation, Variant }
import play.api.libs.json.Json

import lila.app._
import lila.game.{ GameRepo, Pov }
import views._

object UserAnalysis extends LilaController with BaseGame {

  def index = load("")

  def load(urlFen: String) = Open { implicit ctx =>
    val fenStr = Some(urlFen.trim.replace("_", " ")).filter(_.nonEmpty) orElse get("fen")
    val decodedFen = fenStr.map { java.net.URLDecoder.decode(_, "UTF-8").trim }.filter(_.nonEmpty)
    val situation = (decodedFen flatMap Forsyth.<<< map (_.situation)) | Situation(Variant.Standard)
    val pov = makePov(situation)
    val data = Env.round.jsonView.userAnalysisJson(pov, ctx.pref)
    makeListMenu map { listMenu =>
      Ok(html.board.userAnalysis(listMenu, data, none))
    }
  }

  private def makePov(situation: Situation) = lila.game.Pov(
    lila.game.Game.make(
      game = chess.Game(situation.board, situation.color),
      whitePlayer = lila.game.Player.white,
      blackPlayer = lila.game.Player.black,
      mode = chess.Mode.Casual,
      variant = chess.Variant.Standard,
      source = lila.game.Source.Api,
      pgnImport = None,
      castles = situation.board.history.castles),
    situation.color)

  def game(id: String, color: String) = Open { implicit ctx =>
    OptionFuOk(GameRepo game id) { game =>
      val pov = Pov(game, chess.Color(color == "white"))
      val data = Env.round.jsonView.userAnalysisJson(pov, ctx.pref)
      makeListMenu map { listMenu =>
        html.board.userAnalysis(listMenu, data, pov.some)
      }
    }
  }
}
