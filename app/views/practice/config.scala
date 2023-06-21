package views.html.practice

import play.api.data.Form

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }

import controllers.routes

object config:

  def apply(structure: lila.practice.PracticeStructure, form: Form[?])(using PageContext) =
    views.html.base.layout(
      title = "Practice structure",
      moreCss = cssTag("mod.misc")
    )(
      main(cls := "page-menu")(
        views.html.mod.menu("practice"),
        div(cls := "practice_config page-menu__content box box-pad")(
          h1(cls := "box__top")("Practice config"),
          div(cls := "both")(
            postForm(action := routes.Practice.configSave)(
              textarea(cls := "practice_text", name := "text")(form("text").value),
              errMsg(form("text")),
              submitButton(cls := "button button-fat text", dataIcon := licon.Checkmark)("Save")
            ),
            div(cls := "preview")(
              ol(
                structure.sections.map { section =>
                  li(
                    h2(section.name, "#", section.id, section.hide so " [hidden]"),
                    ol(
                      section.studies.map { stud =>
                        li(
                          i(cls := s"practice icon ${stud.id}")(
                            h3(
                              a(href := routes.Study.show(stud.id))(
                                stud.name,
                                "#",
                                stud.id,
                                stud.hide so " [hidden]"
                              )
                            ),
                            em(stud.desc),
                            ol(
                              stud.chapters.map { cha =>
                                li(a(href := routes.Study.chapter(stud.id, cha.id))(cha.name))
                              }
                            )
                          )
                        )
                      }
                    )
                  )
                }
              )
            )
          )
        )
      )
    )
