package controllers

import chess.format.Forsyth
import chess.{ Situation, Variant }

import lila.app._
import lila.game.GameRepo
import views._

object Editor extends LilaController with BaseGame {

  private def env = Env.importer

  def index = load("")

  def load(urlFen: String) = Open { implicit ctx =>
    val fenStr = Some(urlFen.trim.replace("_", " ")).filter(_.nonEmpty) orElse get("fen")
    makeListMenu map { listMenu =>
      val decodedFen = fenStr.map { java.net.URLDecoder.decode(_, "UTF-8").trim }.filter(_.nonEmpty)
      val situation = (decodedFen flatMap Forsyth.<<< map (_.situation)) | Situation(Variant.Standard)
      val fen = Forsyth >> situation
      Ok(html.board.editor(listMenu, situation, fen))
    }
  }

  def game(id: String) = Open { implicit ctx =>
    OptionResult(GameRepo game id) { game =>
      Redirect(routes.Editor.load(
        get("fen") | (chess.format.Forsyth >> game.toChess)
      ))
    }
  }
}
