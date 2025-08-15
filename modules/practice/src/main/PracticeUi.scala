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

  def show(us: UserStudy, data: JsonView.JsData)(using ctx: Context) =
    Page(us.practiceStudy.name.value)
      .css("analyse.practice")
      .i18n(_.puzzle, _.study)
      .i18nOpt(ctx.blind, _.keyboardMove, _.nvui)
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
    Page("Practice chess positions")
      .css("bits.practice.index")
      .graph(
        title = "Practice your chess",
        description = "Learn how to master the most common chess positions",
        url = s"$netBaseUrl${routes.Practice.index}"
      ):
        main(cls := "page-menu force-ltr")(
          st.aside(cls := "page-menu__menu practice-side")(
            i(cls := "fat"),
            h1("Practice"),
            h2("makes your chess perfect"),
            div(cls := "progress")(
              div(cls := "text")("Progress: ", data.progressPercent, "%"),
              div(cls := "bar", style := s"width: ${data.progressPercent}%")
            ),
            postForm(action := routes.Practice.reset)(
              if ctx.isAuth then
                (data.nbDoneChapters > 0).option(
                  submitButton(
                    cls := "button ok-cancel-confirm",
                    title := "You will lose your practice progress!"
                  )("Reset my progress")
                )
              else a(href := routes.Auth.signup)("Sign up to save your progress")
            )
          ),
          div(cls := "page-menu__content practice-app")(
            data.structure.sections.map: section =>
              st.section(
                h2(section.name),
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
                        h3(stud.name),
                        em(stud.desc)
                      )
                    )
                )
              )
          )
        )
