package views.html
package game

import lidraughts.api.Context
import lidraughts.app.templating.Environment._
import lidraughts.app.ui.ScalatagsTemplate._

import controllers.routes

object importGame {

  private def analyseHelp(implicit ctx: Context) =
    ctx.isAnon option a(cls := "blue", href := routes.Auth.signup)(trans.youNeedAnAccountToDoThat())

  def apply(form: play.api.data.Form[_])(implicit ctx: Context) = views.html.base.layout(
    title = trans.importGame.txt(),
    moreCss = cssTag("importer"),
    moreJs = jsTag("importer.js"),
    openGraph = lidraughts.app.ui.OpenGraph(
      title = "Paste PDN draughts game",
      url = s"$netBaseUrl${routes.Importer.importGame.url}",
      description = trans.importGameExplanation.txt()
    ).some
  ) {
      main(cls := "importer page-small box box-pad")(
        h1(trans.importGame()),
        p(cls := "explanation")(trans.importGameExplanation()),
        postForm(cls := "form3 import", action := routes.Importer.sendGame())(
          form3.group(form("pdn"), trans.pasteThePdnStringHere())(form3.textarea(_)()),
          form("pdn").value.flatMap { pdn =>
            lidraughts.importer.ImportData(pdn, none).preprocess(none).fold(
              err => frag(
                pre(cls := "error")(err.toList mkString "\n"),
                br, br
              ).some,
              _ => none
            )
          },
          form3.group(form("pdnFile"), raw("Or upload a PDN file"), klass = "upload") { f =>
            frag(" ", form3.file.pdn(f.name))
          },
          form3.checkbox(form("analyse"), trans.requestAComputerAnalysis(), help = Some(analyseHelp), disabled = ctx.isAnon),
          form3.action(form3.submit(trans.importGame(), "/".some))
        )
      )
    }
}
