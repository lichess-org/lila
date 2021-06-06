package views.html.practice

import play.api.data.Form

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

import controllers.routes

object config {

  def apply(structure: lila.practice.PracticeStructure, form: Form[_])(implicit ctx: Context) =
    views.html.base.layout(
      title = "Practice structure",
      moreCss = cssTag("mod.misc")
    )(
      main(cls := "page-menu")(
        views.html.mod.menu("practice"),
        div(cls := "practice_config page-menu__content box box-pad")(
          h1("Practice config"),
          div(cls := "both")(
            postForm(action := routes.Practice.configSave)(
              textarea(cls := "practice_text", name := "text")(form("text").value),
              errMsg(form("text")),
              submitButton(cls := "button button-fat text", dataIcon := "")("Save")
            ),
            div(cls := "preview")(
              ol(
                structure.sections.map { section =>
                  li(
                    h2(s"${section.name} (#${section.id})"),
                    ol(
                      section.studies.map { stud =>
                        li(
                          i(cls := s"practice icon ${stud.id}")(
                            h3(a(href := routes.Study.show(stud.id.value))(s"${stud.name} (#${stud.id})")),
                            em(stud.desc),
                            ol(
                              stud.chapters.map { cha =>
                                li(a(href := routes.Study.chapter(stud.id.value, cha.id.value))(cha.name))
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
}
