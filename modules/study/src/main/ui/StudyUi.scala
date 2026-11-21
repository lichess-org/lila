package lila.study
package ui

import play.api.libs.json.{ JsObject, Json }

import lila.core.study.IdName
import lila.core.socket.SocketVersion
import lila.ui.*
import lila.ui.ScalatagsTemplate.{ *, given }
import lila.common.Json.given

final class StudyUi(helpers: Helpers)(endpoints: AnalyseEndpoints):
  import helpers.{ *, given }

  private def studyButton(s: IdName, chapterCount: Int) =
    val btn =
      if Study.maxChapters <= chapterCount then submitButton(cls := "disabled", st.disabled)
      else submitButton
    btn(name := "as", value := s.id, cls := "button button-empty study submit")(s.name)

  def show(
      s: lila.study.Study,
      chapter: lila.study.Chapter,
      data: lila.study.JsonView.JsData,
      chatOption: Option[JsObject],
      socketVersion: SocketVersion,
      streamers: Option[Tag]
  )(using ctx: Context) =
    Page(s.name.value)
      .css("analyse.study")
      .css(ctx.pref.hasKeyboardMove.option("keyboardMove"))
      .css(ctx.blind.option("round.nvui"))
      .i18n(_.study)
      .i18n(_.variant)
      .i18nOpt(ctx.speechSynthesis, _.nvui)
      .i18nOpt(ctx.blind, _.keyboardMove)
      .js(analyseNvuiTag)
      .js(
        PageModule(
          "analyse.study",
          Json.obj(
            "study" -> data.study
              .add("admin", Granter.opt(_.StudyAdmin))
              .add("showRatings", ctx.pref.showRatings),
            "data" -> data.analysis,
            "tagTypes" -> lila.study.StudyPgnTags.typesToString,
            "userId" -> ctx.userId,
            "chat" -> chatOption,
            "socketUrl" -> socketUrl(s.id),
            "socketVersion" -> socketVersion
          ) ++ endpoints.json
        )
      )
      .flag(_.noRobots, !s.isPublic)
      .flag(_.zoom)
      .csp(cspExternalEngine.compose(_.withExternalAnalysisApis))
      .graph(
        OpenGraph(
          title = s.name.value,
          url = routeUrl(routes.Study.show(s.id)),
          description = s"A chess study by ${titleNameOrId(s.ownerId)}",
          image = fenThumbnailUrl(
            chapter.root.fen.opening,
            chapter.setup.orientation.some,
            chapter.setup.variant
          ).some
        )
      ):
        frag(
          main(cls := "analyse"),
          streamers.map(_(cls := "none"))
        )

  def socketUrl(id: StudyId) = s"/study/$id/socket/v6"

  private def cspExternalEngine: Update[ContentSecurityPolicy] =
    _.withWebAssembly.withExternalEngine(endpoints.externalEngine)

  def create(
      data: lila.study.StudyForm.importGame.Data,
      owner: List[(IdName, Int)],
      contrib: List[(IdName, Int)],
      backUrl: Option[String],
      siteMessage: SiteMessage
  )(using Context) =
    siteMessage(title = trans.site.toStudy.txt(), icon = Icon.StudyBoard.some, back = backUrl)
      .css("analyse.study.create"):
        div(cls := "study-create")(
          postForm(action := routes.Study.create)(
            form3.hidden("gameId", data.gameId),
            form3.hidden("orientation", data.orientation.map(_.key)),
            form3.hidden("fen", data.fen.map(_.value)),
            form3.hidden("pgn", data.pgnStr),
            form3.hidden("variant", data.variant.map(_.key)),
            form3.hidden("mode", data.mode.map(_.key)),
            h2(trans.study.whereDoYouWantToStudyThat()),
            p(
              submitButton(
                name := "as",
                value := "study",
                cls := "submit button large new text",
                dataIcon := Icon.StudyBoard
              )(trans.study.createStudy())
            ),
            div(cls := "studies")(
              div(
                h2(trans.study.myStudies()),
                if owner.nonEmpty then owner.map(studyButton)
                else p(cls := "placeholder")(trans.site.none())
              ),
              div(
                h2(trans.study.studiesIContributeTo()),
                if contrib.nonEmpty then contrib.map(studyButton)
                else p(cls := "placeholder")(trans.site.none())
              )
            )
          )
        )

  def clone(s: Study, siteMessage: SiteMessage)(using Translate) =
    siteMessage(title = s"Clone ${s.name}", back = none, icon = Icon.StudyBoard.some):
      postForm(action := routes.Study.cloneApply(s.id))(
        p("This will create a new private study with the same chapters."),
        p("You will be the owner of that new study."),
        p("The two studies can be updated separately."),
        p("Deleting one study will ", strong("not"), " delete the other study."),
        p(
          submitButton(
            cls := "submit button large text",
            dataIcon := Icon.StudyBoard,
            style := "margin: 30px auto; display: block; font-size: 2em;"
          )("Clone the study")
        ),
        p(a(href := routes.Study.show(s.id), cls := "text", dataIcon := Icon.LessThan)(trans.site.cancel()))
      )

  def privateStudy(study: Study, siteMessage: SiteMessage)(using Context) =
    siteMessage(
      title = s"${titleNameOrId(study.ownerId)}'s study",
      back = routes.Study.allDefault().url.some,
      icon = none
    ):
      frag(
        "Sorry! This study is private, you cannot access it.",
        Granter
          .opt(_.StudyAdmin)
          .option:
            postForm(action := routes.Study.admin(study.id)):
              submitButton("View as admin")(cls := "button button-red")
      )
