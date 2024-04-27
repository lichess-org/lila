package lila.study
package ui

import play.api.data.Form
import scalalib.paginator.Paginator

import lila.ui.*
import ScalatagsTemplate.{ *, given }
import lila.common.String.removeMultibyteSymbols

final class ListUi(helpers: Helpers, bits: StudyBits):
  import helpers.{ *, given }
  import trans.{ study as trs }

  def paginate(pager: Paginator[Study.WithChaptersAndLiked], url: Call)(using Context) =
    if pager.currentPageResults.isEmpty then
      div(cls := "nostudies")(
        iconTag(Icon.StudyBoard),
        p(trs.noneYet())
      )
    else
      div(cls := "studies list infinite-scroll")(
        pager.currentPageResults.map { s =>
          div(cls := "study paginated")(bits.widget(s))
        },
        pagerNext(pager, np => addQueryParam(url.url, "page", np.toString))
      )

  def menu(active: String, order: Order, topics: List[StudyTopic] = Nil)(using ctx: Context) =
    val nonMineOrder = if order == Order.Mine then Order.Hot else order
    lila.ui.bits.pageMenuSubnav(
      a(cls := active.active("all"), href := routes.Study.all(nonMineOrder.key))(trs.allStudies()),
      ctx.isAuth.option(bits.authLinks(active, nonMineOrder)),
      a(cls := List("active" -> active.startsWith("topic")), href := routes.Study.topics):
        trs.topics()
      ,
      topics.map: topic =>
        a(cls := active.active(s"topic:$topic"), href := routes.Study.byTopic(topic.value, order.key))(
          topic.value
        ),
      a(cls := active.active("staffPicks"), href := routes.Study.staffPicks)("Staff picks"),
      a(
        cls      := "text",
        dataIcon := Icon.InfoCircle,
        href     := "/blog/V0KrLSkAAMo3hsi4/study-chess-the-lichess-way"
      ):
        trs.whatAreStudies()
    )

  def search(pag: Paginator[Study.WithChaptersAndLiked], text: String)(using Context) =
    main(cls := "page-menu")(
      menu("search", Order.default),
      main(cls := "page-menu__content study-index box")(
        div(cls := "box__top")(
          searchForm(trans.search.search.txt(), text),
          bits.newForm()
        ),
        paginate(pag, routes.Study.search(text))
      )
    )

  def searchForm(placeholder: String, value: String) =
    form(cls := "search", action := routes.Study.search(), method := "get")(
      input(name       := "q", st.placeholder := placeholder, st.value := value, enterkeyhint := "search"),
      submitButton(cls := "button", dataIcon  := Icon.Search)
    )

  object topic:

    def topicsList(topics: StudyTopics, order: Order = Order.default) =
      div(cls := "topic-list")(
        topics.value.map: t =>
          a(href := routes.Study.byTopic(t.value, order.key))(t.value)
      )

    def index(popular: StudyTopics, mine: Option[StudyTopics], myForm: Option[Form[?]])(using Context) =
      main(cls := "page-menu")(
        menu("topic", Order.Mine, mine.so(_.value)),
        main(cls := "page-menu__content study-topics box box-pad")(
          h1(cls := "box__top")(trans.study.topics()),
          myForm.map { form =>
            frag(
              h2(trans.study.myTopics()),
              postForm(cls := "form3", action := routes.Study.topics)(
                form3.textarea(form("topics"))(rows := 10, attrData("max") := StudyTopics.userMax),
                form3.submit(trans.site.save())
              )
            )
          },
          h2(trans.study.popularTopics()),
          topicsList(popular)
        )
      )

    def show(
        topic: StudyTopic,
        pag: Paginator[Study.WithChaptersAndLiked],
        order: Order,
        myTopics: Option[StudyTopics]
    )(using Context) =
      val active = s"topic:$topic"
      val url    = (o: String) => routes.Study.byTopic(topic.value, o)
      main(cls := "page-menu")(
        menu(active, order, myTopics.so(_.value)),
        main(cls := "page-menu__content study-index box")(
          boxTop(
            h1(topic.value),
            bits.orderSelect(order, active, url),
            bits.newForm()
          ),
          myTopics.ifTrue(order == Order.Mine).map { ts =>
            div(cls := "box__pad")(
              topicsList(ts, Order.Mine)
            )
          },
          paginate(pag, url(order.key))
        )
      )
