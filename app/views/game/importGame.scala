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
    moreCss = cssTags("form3.css", "import.css"),
    moreJs = jsTag("importer.js"),
    openGraph = lidraughts.app.ui.OpenGraph(
      title = "Paste PDN draughts game",
      url = s"$netBaseUrl${routes.Importer.importGame.url}",
      description = "When pasting a game PDN, you get a browsable replay, a computer analysis, a game chat and a sharable URL"
    ).some
  ) {
      div(id := "import_game", cls := "content_box")(
        h1(dataIcon := "/", cls := "title text")(trans.importGame()),
        p(cls := "explanation")(trans.importGameExplanation()),
        st.form(cls := "form3 import", action := routes.Importer.sendGame(), method := "post")(
          form3.group(form("pdn"), trans.pasteThePdnStringHere.frag())(form3.textarea(_)()),
          form("pdn").value.flatMap { pdn =>
            lidraughts.importer.ImportData(pdn, none).preprocess(none).fold(
              err => div(cls := "error")(err.toList mkString "\n").some,
              _ => none
            )
          },
          form3.group(form("pdnFile"), raw("Or upload a PDN file"), klass = "upload") { f =>
            frag(" ", form3.file.pdn(f.name))
          },
          form3.checkbox(form("analyse"), trans.requestAComputerAnalysis.frag(), help = Some(analyseHelp), disabled = ctx.isAnon),
          form3.actionHtml(form3.submit(trans.importGame.frag()))
        )
      )
    }.toHtml
}
