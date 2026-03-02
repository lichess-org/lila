package lila.analyse

import chess.{ ByColor, Ply, Division }
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
              "comment" -> a.makeComment(false)
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
      ._2F
      .map { s =>
        JsObject(s.map { (nag, nb) =>
          nag.toString.toLowerCase -> JsNumber(nb)
        })
          .add("acpl", lila.analyse.AccuracyCP.mean(pov, analysis))
          .add("accuracy", accuracy.map(_(pov.color).toInt))
      }

  def bothPlayers(startedAtPly: Ply, analysis: Analysis, withAccuracy: Boolean = true) =
    val accuracy = withAccuracy.so(AccuracyPercent.gameAccuracy(startedAtPly.turn, analysis))
    val both = ByColor[Option[JsObject]]: color =>
      player(SideAndStart(color, startedAtPly))(analysis, accuracy)
    Json.obj("id" -> analysis.id.value) ++ Json.toJsObject(both)

  def mobile(game: Game, analysis: Analysis) =
    Json.obj(
      "summary" -> bothPlayers(game.startedAtPly, analysis, withAccuracy = false),
      "moves" -> moves(analysis)
    )

  def analysisHeader(root: lila.tree.Root, division: Division, analysis: Analysis) =
    Json.obj(
      "division" -> division,
      "summary" -> bothPlayers(root.ply, analysis)
    )
