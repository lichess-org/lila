package views.html
package game

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import chess.format.pgn.PgnStr

import controllers.routes

object importGame:

  private def analyseHelp(using ctx: PageContext) =
    ctx.isAnon option a(cls := "blue", href := routes.Auth.signup)(trans.youNeedAnAccountToDoThat())

  def apply(form: play.api.data.Form[?])(using ctx: PageContext) =
    views.html.base.layout(
      title = trans.importGame.txt(),
      moreCss = cssTag("importer"),
      moreJs = iifeModule("javascripts/importer.js"),
      openGraph = lila.app.ui
        .OpenGraph(
          title = "Paste PGN chess game",
          url = s"$netBaseUrl${routes.Importer.importGame.url}",
          description = trans.importGameExplanation.txt()
        )
        .some
    ):
      main(cls := "importer page-small box box-pad")(
        h1(cls := "box__top")(trans.importGame()),
        p(cls := "explanation")(trans.importGameExplanation()),
        p:
          a(cls := "text", dataIcon := licon.InfoCircle, href := routes.Study.allDefault(1)):
            trans.importGameCaveat()
        ,
        standardFlash,
        postForm(cls := "form3 import", action := routes.Importer.sendGame)(
          form3.group(form("pgn"), trans.pasteThePgnStringHere())(form3.textarea(_)()),
          form("pgn").value flatMap { pgn =>
            lila.importer
              .ImportData(PgnStr(pgn), none)
              .preprocess(none)
              .fold(
                err => frag(pre(cls := "error")(err), br, br).some,
                _ => none
              )
          },
          form3.group(form("pgnFile"), trans.orUploadPgnFile(), klass = "upload"): f =>
            form3.file.pgn(f.name),
          form3.checkbox(
            form("analyse"),
            trans.requestAComputerAnalysis(),
            help = Some(analyseHelp),
            disabled = ctx.isAnon
          ),
          form3.action(form3.submit(trans.importGame(), licon.UploadCloud.some))
        )
      )
