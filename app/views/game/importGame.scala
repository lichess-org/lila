package views.html
package game

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

import controllers.routes

object importGame {

  private def analyseHelp(implicit ctx: Context) =
    ctx.isAnon option a(cls := "blue", href := routes.Auth.signup)(trans.youNeedAnAccountToDoThat())

  def apply(form: play.api.data.Form[_])(implicit ctx: Context) =
    views.html.base.layout(
      title = trans.importGame.txt(),
      moreCss = cssTag("importer"),
      moreJs = jsTag("importer.js"),
      openGraph = lila.app.ui
        .OpenGraph(
          title = "Paste PGN chess game",
          url = s"$netBaseUrl${routes.Importer.importGame.url}",
          description = trans.importGameExplanation.txt()
        )
        .some
    ) {
      main(cls := "importer page-small box box-pad")(
        h1(trans.importGame()),
        p(cls := "explanation")(trans.importGameExplanation()),
        postForm(cls := "form3 import", action := routes.Importer.sendGame)(
          form3.group(form("pgn"), trans.pasteThePgnStringHere())(form3.textarea(_)()),
          form("pgn").value flatMap { pgn =>
            lila.importer
              .ImportData(pgn, none)
              .preprocess(none)
              .fold(
                err =>
                  frag(
                    pre(cls := "error")(err),
                    br,
                    br
                  ).some,
                _ => none
              )
          },
          form3.group(form("pgnFile"), raw("Or upload a PGN file"), klass = "upload") { f =>
            form3.file.pgn(f.name)
          },
          form3.checkbox(
            form("analyse"),
            trans.requestAComputerAnalysis(),
            help = Some(analyseHelp),
            disabled = ctx.isAnon
          ),
          form3.action(form3.submit(trans.importGame(), "/".some))
        )
      )
    }
}
