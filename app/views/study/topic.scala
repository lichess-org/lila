package views.html.study

import play.api.data.Form

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.study.{ Order, StudyTopics }

import controllers.routes

object topic {

  def index(popular: StudyTopics, mine: Option[StudyTopics], myForm: Option[Form[_]])(implicit ctx: Context) =
    views.html.base.layout(
      title = "Study topics",
      moreCss = frag(cssTag("study.index"), cssTag("form3"), cssTag("tagify")),
      moreJs = frag(tagifyTag, jsTag("study/topic-form.js")),
      wrapClass = "full-screen-force"
    ) {
      main(cls := "page-menu")(
        views.html.study.list.menu("topic", Order.Hot, mine.??(_.value)),
        main(cls := "page-menu__content study-topics box box-pad")(
          h1("Study topics [BETA]"),
          myForm.map { form =>
            postForm(cls := "form3", action := routes.Study.topics())(
              form3.group(form("topics"), frag("Topics to organize your studies with"))(
                form3.textarea(_)(rows := 10)
              ),
              form3.submit(trans.save())
            )
          },
          mine.filter(_.value.nonEmpty) map { topics =>
            frag(
              h2("My topics"),
              topicsList(topics)
            )
          },
          h2("Popular topics"),
          topicsList(popular)
        )
      )
    }

  private def topicsList(topics: StudyTopics) =
    div(cls := "topic-list")(
      topics.value.map { t =>
        a(href := routes.Study.byTopic(t.value, "hot"))(t.value)
      }
    )
}
