package views.html.study

import play.api.data.Form

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.common.paginator.Paginator
import lila.study.{ Order, StudyTopic, StudyTopics }
import lila.study.Study.WithChaptersAndLiked

import controllers.routes

object topic:

  def index(popular: StudyTopics, mine: Option[StudyTopics], myForm: Option[Form[?]])(using PageContext) =
    views.html.base.layout(
      title = trans.study.topics.txt(),
      moreCss = frag(cssTag("study.index"), cssTag("form3"), cssTag("tagify")),
      moreJs = jsModule("study.topic.form"),
      wrapClass = "full-screen-force"
    ) {
      main(cls := "page-menu")(
        views.html.study.list.menu("topic", Order.Mine, mine.so(_.value)),
        main(cls := "page-menu__content study-topics box box-pad")(
          h1(cls := "box__top")(trans.study.topics()),
          myForm.map { form =>
            frag(
              h2(trans.study.myTopics()),
              postForm(cls := "form3", action := routes.Study.topics)(
                form3.textarea(form("topics"))(rows := 10, attrData("max") := StudyTopics.userMax),
                form3.submit(trans.save())
              )
            )
          },
          h2(trans.study.popularTopics()),
          topicsList(popular)
        )
      )
    }

  def show(
      topic: StudyTopic,
      pag: Paginator[WithChaptersAndLiked],
      order: Order,
      myTopics: Option[StudyTopics]
  )(using PageContext) =
    views.html.base.layout(
      title = topic.value,
      moreCss = cssTag("study.index"),
      wrapClass = "full-screen-force",
      moreJs = infiniteScrollTag
    ) {
      val active = s"topic:$topic"
      val url    = (o: String) => routes.Study.byTopic(topic.value, o)
      main(cls := "page-menu")(
        views.html.study.list.menu(active, order, myTopics.so(_.value)),
        main(cls := "page-menu__content study-index box")(
          boxTop(
            h1(topic.value),
            bits.orderSelect(order, active, url),
            bits.newForm()
          ),
          myTopics.ifTrue(order == Order.Mine) map { ts =>
            div(cls := "box__pad")(
              topicsList(ts, Order.Mine)
            )
          },
          views.html.study.list.paginate(pag, url(order.key))
        )
      )
    }

  def topicsList(topics: StudyTopics, order: Order = Order.default) =
    div(cls := "topic-list")(
      topics.value.map { t =>
        a(href := routes.Study.byTopic(t.value, order.key))(t.value)
      }
    )
