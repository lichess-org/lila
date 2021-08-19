package views.html
package game

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

import controllers.routes

object importGame {

  private def analyseHelp(implicit ctx: Context) =
    ctx.isAnon option a(cls := "blue", href := routes.Auth.signup())(trans.youNeedAnAccountToDoThat())

  def apply(form: play.api.data.Form[_])(implicit ctx: Context) =
    views.html.base.layout(
      title = trans.importGame.txt(),
      moreCss = cssTag("importer"),
      moreJs = jsTag("importer.js"),
      openGraph = lila.app.ui
        .OpenGraph(
          title = "Paste KIF shogi game",
          url = s"$netBaseUrl${routes.Importer.importGame().url}",
          description = trans.importGameKifuExplanation.txt()
        )
        .some
    ) {
      main(cls := "importer page-small box box-pad")(
        h1(trans.importGame()),
        p(cls := "explanation")(trans.importGameKifuExplanation()),
        postForm(cls := "form3 import", action := routes.Importer.sendGame())(
          div(cls := "import left")(
            form3.group(form("kif"), trans.pasteTheKifStringHere())(form3.textarea(_)()),
            form("kif").value flatMap { kif =>
              lila.importer
                .ImportData(kif, none)
                .preprocess(none)
                .fold(
                  err =>
                    frag(
                      pre(cls := "error")(err.toList mkString "\n"),
                      br,
                      br
                    ).some,
                  _ => none
                )
            }
          ),
          div(cls := "import right")(
            form3.group(form("kifFile"), raw("Or upload a KIF file"), klass = "upload") { f =>
              form3.file.kif(f.name)
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
      )
    }
}
