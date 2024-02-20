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
          title = trans.importGame.txt(),
          url = s"$netBaseUrl${routes.Importer.importGame.url}",
          description = trans.importGameKifCsaExplanation.txt()
        )
        .some,
      withHrefLangs = lila.i18n.LangList.All.some
    ) {
      main(cls := "importer page-small box box-pad")(
        h1(trans.importGame()),
        p(cls := "explanation")(trans.importGameKifCsaExplanation()),
        postForm(cls := "form3 import", action := routes.Importer.sendGame)(
          div(cls := "import-top")(
            div(cls := "left")(
              form3.group(form("notation"), trans.pasteTheKifCsaStringHere())(form3.textarea(_)())
            ),
            div(cls := "right")(
              form3.group(form("notationFile"), raw("Or upload a KIF/CSA file"), klass = "upload") { f =>
                form3.file.notation(f)
              },
              form3.checkbox(
                form("analyse"),
                trans.requestAComputerAnalysis(),
                help = Some(analyseHelp),
                disabled = ctx.isAnon
              ),
              form3.action(form3.submit(trans.importGame(), "/".some))
            )
          ),
          form("notation").value.filterNot(_.isEmpty) flatMap { notation =>
            lila.importer
              .ImportData(notation, none)
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
          }
        )
      )
    }
}
