package views.html
package game

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

import controllers.routes

object importGame {

  private def analyseHelp(implicit ctx: Context) =
    ctx.isAnon option a(cls := "blue", href := routes.Auth.signup)(trans.youNeedAnAccountToDoThat())

  def apply(form: play.api.data.Form[_])(implicit ctx: Context) = views.html.base.layout(
    title = trans.importGame.txt(),
    moreCss = cssTags("form3.css", "import.css"),
    moreJs = jsTag("importer.js"),
    openGraph = lila.app.ui.OpenGraph(
      title = "Paste PGN chess game",
      url = s"$netBaseUrl${routes.Importer.importGame.url}",
      description = "When pasting a game PGN, you get a browsable replay, a computer analysis, a game chat and a sharable URL"
    ).some
  ) {
      div(id := "import_game", cls := "content_box")(
        h1(dataIcon := "/", cls := "title text")(trans.importGame()),
        p(cls := "explanation")(trans.importGameExplanation()),
        st.form(cls := "form3 import", action := routes.Importer.sendGame(), method := "post")(
          form3.group(form("pgn"), trans.pasteThePgnStringHere.frag())(form3.textarea(_)()),
          form3.group(form("pgnFile"), raw("Or upload a PGN file"), klass = "upload") { f =>
            form3.file.pgn(f.name)
          },
          form3.checkbox(form("analyse"), trans.requestAComputerAnalysis.frag(), help = Some(analyseHelp), disabled = ctx.isAnon),
          form3.action(form3.submit(trans.importGame.frag()))
        )
      )
    }.toHtml
}
