package lila.analyse

import chess.{ ByColor, Color, Ply }
import play.api.libs.json.*

import lila.common.Json.given
import lila.core.game.SideAndStart
import lila.tree.Analysis

object JsonView extends lila.tree.AnalysisJson:

  def moves(analysis: Analysis, withGlyph: Boolean = true) =
    JsArray(analysis.infoAdvices.map { (info, adviceOption) =>
      Json
        .obj()
        .add("eval" -> info.cp)
        .add("mate" -> info.mate)
        .add("best" -> info.best.map(_.uci))
        .add("variation" -> info.variation.nonEmpty.option(info.variation.mkString(" ")))
        .add("judgment" -> adviceOption.map { a =>
          Json
            .obj(
              "name" -> a.judgment.name,
              "comment" -> a.makeComment(withEval = false, withBestMove = true)
            )
            .add(
              "glyph" -> withGlyph.option(
                Json.obj(
                  "name" -> a.judgment.glyph.name,
                  "symbol" -> a.judgment.glyph.symbol
                )
              )
            )
        })
    })

  def player(pov: SideAndStart)(analysis: Analysis, accuracy: Option[ByColor[AccuracyPercent]]) =
    analysis.summary
      .find(_._1 == pov.color)
      .map(_._2)
      .map { s =>
        JsObject(s.map { (nag, nb) =>
          nag.toString.toLowerCase -> JsNumber(nb)
        })
          .add("acpl", lila.analyse.AccuracyCP.mean(pov, analysis))
          .add("accuracy", accuracy.map(_(pov.color).toInt))
      }

  def bothPlayers(startedAtPly: Ply, analysis: Analysis, withAccuracy: Boolean = true) =
    val accuracy = withAccuracy.so(AccuracyPercent.gameAccuracy(startedAtPly.turn, analysis))
    Json.obj(
      "id" -> analysis.id.value,
      "white" -> player(SideAndStart(Color.white, startedAtPly))(analysis, accuracy),
      "black" -> player(SideAndStart(Color.black, startedAtPly))(analysis, accuracy)
    )

//   def bothPlayers(pov: Game.SideAndStart, analysis: Analysis, accuracy: Option[ByColor[AccuracyPercent]]) =
//     Json.obj(
//       "id"    -> analysis.id,
//       "white" -> player(pov.copy(color = chess.White))(analysis, accuracy),
//       "black" -> player(pov.copy(color = chess.Black))(analysis, accuracy)
//     )

  def mobile(game: Game, analysis: Analysis) =
    Json.obj(
      "summary" -> bothPlayers(game.startedAtPly, analysis, withAccuracy = false),
      "moves" -> moves(analysis)
    )
