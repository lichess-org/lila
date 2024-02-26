package controllers

import shogi.format.forsyth.Sfen
import shogi.variant.{ Standard, Variant }
import shogi.Situation

import lila.app._
import views._

final class Editor(env: Env) extends LilaController(env) {

  def index = load("", Standard)

  def parseArg(arg: String) =
    arg.split("/", 2) match {
      case Array(key) => load("", Variant orDefault key)
      case Array(key, sfen) =>
        Variant.byKey get key match {
          case Some(variant) => load(sfen, variant)
          case _             => load(arg, Standard)
        }
      case _ => load("", Standard)
    }

  def load(urlSfen: String, variant: Variant) =
    Open { implicit ctx =>
      val decodedSfen: Option[Sfen] = lila.common.String
        .decodeUriPath(urlSfen)
        .filter(_.trim.nonEmpty)
        .orElse(get("sfen")) map Sfen.clean
      val color = ctx.req.getQueryString("orientation").flatMap(shogi.Color.fromName).getOrElse(shogi.Sente)
      fuccess {
        val situation = readSfen(decodedSfen, variant.some)
        Ok(
          html.board.editor(
            situation,
            color
          )
        )
      }
    }

  def data =
    Open { implicit ctx =>
      fuccess {
        val sfen        = get("sfen") map Sfen.clean
        val variant     = get("variant").flatMap(Variant.byKey get _)
        val sit         = readSfen(sfen, variant)
        val orientation = get("orientation").flatMap(shogi.Color.fromName).getOrElse(shogi.Sente)
        Ok(
          html.board.editor.jsData(sit, orientation)
        ) as JSON
      }
    }

  private def readSfen(sfenO: Option[Sfen], variantO: Option[Variant]): Situation = {
    val variant = variantO | Standard
    sfenO.filter(_.value.nonEmpty).flatMap(_.toSituation(variant)) | Situation(variant)
  }

  def game(id: String) =
    Open { implicit ctx =>
      OptionResult(env.game.gameRepo game id) { game =>
        Redirect {
          if (game.playableEvenPaused) routes.Round.watcher(game.id, "sente")
          else routes.Editor.parseArg(s"${game.variant.key}/${get("sfen") | (game.shogi.toSfen).value}")
        }
      }
    }
}
