package views.game

import chess.format.pgn.PgnStr

import lila.app.templating.Environment.{ *, given }

object importGame:

  private def analyseHelp(using ctx: Context) =
    ctx.isAnon.option(a(cls := "blue", href := routes.Auth.signup)(trans.site.youNeedAnAccountToDoThat()))

  def apply(form: play.api.data.Form[?])(using ctx: PageContext) =
    Page(trans.site.importGame.txt())
      .cssTag("importer")
      .iife(iifeModule("javascripts/importer.js"))
      .graph(
        title = "Paste PGN chess game",
        url = s"$netBaseUrl${routes.Importer.importGame.url}",
        description = trans.site.importGameExplanation.txt()
      ):
        main(cls := "importer page-small box box-pad")(
          h1(cls := "box__top")(trans.site.importGame()),
          p(cls := "explanation")(
            trans.site.importGameExplanation(),
            br,
            a(cls := "text", dataIcon := Icon.InfoCircle, href := routes.Study.allDefault()):
              trans.site.importGameCaveat()
          ),
          standardFlash,
          postForm(cls := "form3 import", action := routes.Importer.sendGame)(
            form3.group(form("pgn"), trans.site.pasteThePgnStringHere())(form3.textarea(_)()),
            form("pgn").value.flatMap { pgn =>
              lila.game.importer
                .parseImport(PgnStr(pgn), ctx.userId)
                .fold(
                  err => frag(pre(cls := "error")(err), br, br).some,
                  _ => none
                )
            },
            form3.group(form("pgnFile"), trans.site.orUploadPgnFile(), klass = "upload"): f =>
              form3.file.pgn(f.name),
            form3.checkbox(
              form("analyse"),
              trans.site.requestAComputerAnalysis(),
              help = Some(analyseHelp),
              disabled = ctx.isAnon
            ),
            a(cls := "text", dataIcon := Icon.InfoCircle, href := routes.Study.allDefault(1)):
              trans.site.importGameDataPrivacyWarning()
            ,
            form3.action(form3.submit(trans.site.importGame(), Icon.UploadCloud.some))
          )
        )
