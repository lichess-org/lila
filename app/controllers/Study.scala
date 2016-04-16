package controllers

import play.api.i18n.Messages.Implicits._
import play.api.mvc._
import scala.concurrent.duration._
import chess.variant.Variant

import lila.app._
import views._

object Study extends LilaController {

  private def env = Env.study

  def show(id: String) = Open { implicit ctx =>
    OptionResult(env.api byId id) { study =>
      val data = env.jsonView.study(study)
      Ok(html.study.show(study, data))
    } map NoCache
  }

  def load(urlFen: String, variant: Variant) = Open { implicit ctx =>
    val fenStr = Some(urlFen.trim.replace("_", " ")).filter(_.nonEmpty) orElse get("fen")
    val decodedFen = fenStr.map { java.net.URLDecoder.decode(_, "UTF-8").trim }.filter(_.nonEmpty)
    val situation = decodedFen.flatMap {
      Forsyth.<<<@(variant, _)
    } | SituationPlus(Situation(variant), 1)
    val pov = makePov(situation)
    val orientation = get("color").flatMap(chess.Color.apply) | pov.color
    Env.api.roundApi.userAnalysisJson(pov, ctx.pref, decodedFen, orientation, owner = false) map { data =>
      Ok(html.board.userAnalysis(data, pov))
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

  def create = AuthBody { implicit ctx =>
    me =>
      env.api.create(me) map { study =>
        Redirect(routes.Study.show(study.id))
      }
  }
}
