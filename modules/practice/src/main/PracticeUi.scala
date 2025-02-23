package lila.practice
package ui

import play.api.data.Form
import play.api.libs.json.*

import lila.ui.*

import ScalatagsTemplate.{ *, given }

final class PracticeUi(helpers: Helpers)(
    csp: Update[ContentSecurityPolicy],
    explorerAndCevalConfig: Context ?=> JsObject,
    modMenu: Context ?=> Frag
):
  import helpers.{ *, given }

  def show(us: UserStudy, data: JsonView.JsData)(using ctx: Context) =
    Page(us.practiceStudy.name.value)
      .css("analyse.practice")
      .i18n(_.puzzle, _.study)
      .i18nOpt(ctx.blind, _.keyboardMove)
      .js(analyseNvuiTag)
      .js(
        PageModule(
          "analyse.study",
          Json.obj(
            "practice" -> data.practice,
            "study"    -> data.study,
            "data"     -> data.analysis
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
                    cls   := "button ok-cancel-confirm",
                    title := "You will lose your practice progress!"
                  )("Reset my progress")
                )
              else a(href := routes.Auth.signup)("Sign up to save your progress")
            )
          ),
          div(cls := "page-menu__content practice-app")(
            data.structure.sections.filter(s => !s.hide || Granter.opt(_.PracticeConfig)).map { section =>
              st.section(
                h2(section.name),
                div(cls := "studies")(
                  section.studies.filter(s => !s.hide || Granter.opt(_.PracticeConfig)).map { stud =>
                    val prog = data.progressOn(stud.id)
                    a(
                      cls  := s"study ${if prog.complete then "done" else "ongoing"}",
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
                  }
                )
              )
            }
          )
        )

  def config(structure: lila.practice.PracticeStructure, form: Form[?])(using Context) =
    Page("Practice structure").css("mod.misc"):
      main(cls := "page-menu")(
        modMenu,
        div(cls := "practice_config page-menu__content box box-pad")(
          h1(cls := "box__top")("Practice config"),
          div(cls := "both")(
            postForm(action := routes.Practice.configSave)(
              textarea(cls := "practice_text", name := "text")(form("text").value),
              errMsg(form("text")),
              submitButton(cls := "button button-fat text", dataIcon := Icon.Checkmark)("Save")
            ),
            div(cls := "preview"):
              ol:
                structure.sections.map: section =>
                  li(
                    h2(section.name, "#", section.id, section.hide.so(" [hidden]")),
                    ol(
                      section.studies.map: stud =>
                        li(
                          i(cls := s"practice icon ${stud.id}")(
                            h3(
                              a(href := routes.Study.show(stud.id))(
                                stud.name,
                                "#",
                                stud.id,
                                stud.hide.so(" [hidden]")
                              )
                            ),
                            em(stud.desc),
                            ol:
                              stud.chapters.map: cha =>
                                li(a(href := routes.Study.chapter(stud.id, cha.id))(cha.name))
                          )
                        )
                    )
                  )
          )
        )
      )
