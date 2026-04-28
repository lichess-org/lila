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
  import trans.learn as trl

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
    Page("Practice chess positions")
      .css("bits.practice.index")
      .graph(
        title = "Practice your chess",
        description = "Learn how to master the most common chess positions",
        url = routeUrl(routes.Practice.index)
      ):
        main(cls := "page-menu force-ltr")(
          st.aside(cls := "page-menu__menu practice-side")(
            div(cls := "practice-side__header")(
              img(
                cls := "practice-side__decoration",
                alt := "Decorative image of a robotic golem",
                src := assetUrl("images/practice/robot-golem.svg")
              ),
              div(cls := "practice-side__title")(
                h1("Practice"),
                h2("makes your chess perfect")
              )
            ),
            div(cls := "progress")(
              div(cls := "text")(trl.progressX(s"${data.progressPercent}%")),
              div(cls := "bar", style := s"width: ${data.progressPercent}%")
            ),
            postForm(action := routes.Practice.reset)(
              if ctx.isAuth then
                (data.nbDoneChapters > 0).option(
                  submitButton(
                    cls := "ok-cancel-confirm",
                    title := trl.youWillLoseAllYourProgress.txt()
                  )(trl.resetMyProgress.txt())
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
                    val stateClas =
                      if prog.complete then "done" else if prog.done > 0 then "ongoing" else "future";
                    a(
                      cls := s"study ${stateClas}",
                      href := routes.Practice.show(section.id, stud.slug, stud.id)
                    )(
                      ctx.isAuth.option(
                        span(cls := "ribbon-wrapper")(
                          span(cls := s"ribbon ${stateClas}")(
                            prog.done,
                            " / ",
                            prog.total
                          )
                        )
                      ),
                      i(cls := s"${stud.id}"),
                      span(cls := "text")(
                        h3(stud.name),
                        p(stud.desc)
                      ),
                      (!prog.complete).option(div(cls := "attention-effect"))
                    )
                )
              )
          )
        )
