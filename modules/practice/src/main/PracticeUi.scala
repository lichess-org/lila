package lila.practice
package ui

import play.api.libs.json.*

import lila.ui.*
import lila.ui.ScalatagsTemplate.{ *, given }

final class PracticeUi(helpers: Helpers)(
    csp: Update[ContentSecurityPolicy],
    explorerAndCevalConfig: Context ?=> JsObject
):
  import helpers.{ *, given }
  import trans.practice as trp

  def show(us: UserStudy, data: JsonView.JsData)(using ctx: Context) =
    Page(us.practiceStudy.name.value)
      .css("analyse.practice")
      .i18n(_.study)
      .i18nOpt(ctx.speechSynthesis, _.nvui)
      .i18nOpt(ctx.blind, _.keyboardMove)
      .js(analyseNvuiTag)
      .js(
        PageModule(
          "analyse.study",
          Json.obj(
            "practice" -> data.practice,
            "study" -> data.study,
            "data" -> data.analysis
          ) ++ explorerAndCevalConfig
        )
      )
      .csp(csp)
      .flag(_.zoom):
        main(cls := "analyse")

  def index(data: lila.practice.UserPractice)(using ctx: Context) =
    Page(s"${trans.site.practice.txt()} - ${trp.makesPerfect.txt()}")
      .css("bits.practice.index")
      .i18n(_.practice)
      .graph(
        title = "Practice your chess",
        description = "Learn how to master the most common chess positions",
        url = routeUrl(routes.Practice.index)
      ):
        main(cls := "page-menu force-ltr")(
          st.aside(cls := "page-menu__menu practice-side")(
            i(cls := "fat"),
            h1(trans.site.practice()),
            h2(trp.makesPerfect()),
            div(cls := "progress")(
              div(cls := "text")(trp.progressX(data.progressPercent.toString + "%")),
              div(cls := "bar", style := s"width: ${data.progressPercent}%")
            ),
            postForm(action := routes.Practice.reset)(
              if ctx.isAuth then
                (data.nbDoneChapters > 0).option(
                  submitButton(
                    cls := "button ok-cancel-confirm"
                  )(trp.resetMyProgress())
                )
              else a(href := routes.Auth.signup)(trp.signUpToSaveYourProgress())
            )
          ),
          div(cls := "page-menu__content practice-app")(
            data.structure.sections.map: section =>
              st.section(
                h2(section.name()),
                div(cls := "studies")(
                  section.studies.map: stud =>
                    val prog = data.progressOn(stud.id)
                    a(
                      cls := s"study ${if prog.complete then "done" else "ongoing"}",
                      href := routes.Practice.show(section.id, stud.slug, stud.id)
                    )(
                      ctx.isAuth.option(
                        span(cls := "ribbon-wrapper")(
                          span(cls := "ribbon")(prog.done, " / ", prog.total)
                        )
                      ),
                      i(cls := s"${stud.id}"),
                      span(cls := "text")(
                        h3(stud.name()),
                        em(stud.desc())
                      )
                    )
                )
              )
          )
        )
      .hrefLangs(lila.ui.LangPath(routes.Practice.index))
