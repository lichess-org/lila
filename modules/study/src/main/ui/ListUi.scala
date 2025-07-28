package lila.study
package ui

import play.api.data.Form
import scalalib.paginator.Paginator

import lila.core.study.Order
import lila.study.Study.WithChaptersAndLiked
import lila.ui.*

import ScalatagsTemplate.{ *, given }

final class ListUi(helpers: Helpers, bits: StudyBits):
  import helpers.{ *, given }
  import trans.study as trs

  def all(pag: Paginator[WithChaptersAndLiked], order: Order)(using Context) =
    page(
      title = trs.allStudies.txt(),
      active = "all",
      order = order,
      pag = pag,
      searchFilter = "",
      url = routes.Study.all(_)
    )
      .hrefLangs(lila.ui.LangPath(routes.Study.allDefault()))

  def byOwner(pag: Paginator[WithChaptersAndLiked], order: Order, owner: User)(using Context) =
    page(
      title = trs.studiesCreatedByX.txt(owner.titleUsername),
      active = "owner",
      order = order,
      pag = pag,
      searchFilter = s"owner:${owner.username}",
      url = routes.Study.byOwner(owner.username, _)
    )

  def mine(pag: Paginator[WithChaptersAndLiked], order: Order, topics: StudyTopics)(using
      ctx: Context,
      me: Me
  ) =
    page(
      title = trs.myStudies.txt(),
      active = "mine",
      order = order,
      pag = pag,
      searchFilter = s"owner:${me.username}",
      url = routes.Study.mine(_),
      topics = topics.some
    )

  def mineLikes(
      pag: Paginator[WithChaptersAndLiked],
      order: Order
  )(using Context) =
    page(
      title = trs.myFavoriteStudies.txt(),
      active = "mineLikes",
      order = order,
      pag = pag,
      searchFilter = "",
      url = routes.Study.mineLikes(_)
    )

  def mineMember(pag: Paginator[WithChaptersAndLiked], order: Order, topics: StudyTopics)(using
      ctx: Context,
      me: Me
  ) =
    page(
      title = trs.studiesIContributeTo.txt(),
      active = "mineMember",
      order = order,
      pag = pag,
      searchFilter = s"member:${me.username}",
      url = routes.Study.mineMember(_),
      topics = topics.some
    )

  def minePublic(pag: Paginator[WithChaptersAndLiked], order: Order)(using Context)(using me: Me) =
    page(
      title = trs.myPublicStudies.txt(),
      active = "minePublic",
      order = order,
      pag = pag,
      searchFilter = s"owner:${me.username}",
      url = routes.Study.minePublic(_)
    )

  def minePrivate(pag: Paginator[WithChaptersAndLiked], order: Order)(using Context)(using me: Me) =
    page(
      title = trs.myPrivateStudies.txt(),
      active = "minePrivate",
      order = order,
      pag = pag,
      searchFilter = s"owner:${me.username}",
      url = routes.Study.minePrivate(_)
    )

  def search(pag: Paginator[WithChaptersAndLiked], text: String)(using Context) =
    Page(text)
      .css("analyse.study.index")
      .js(infiniteScrollEsmInit):
        main(cls := "page-menu")(
          menu("search", Orders.default),
          main(cls := "page-menu__content study-index box")(
            div(cls := "box__top")(
              searchForm(trans.search.search.txt(), text),
              bits.newForm()
            ),
            paginate(pag, routes.Study.search(text))
          )
        )

  private def page(
      title: String,
      active: String,
      order: Order,
      pag: Paginator[WithChaptersAndLiked],
      url: Order => Call,
      searchFilter: String,
      topics: Option[StudyTopics] = None
  )(using Context): Page =
    Page(title)
      .css("analyse.study.index")
      .js(infiniteScrollEsmInit):
        main(cls := "page-menu")(
          menu(active, order, topics.so(_.value)),
          main(cls := "page-menu__content study-index box")(
            div(cls := "box__top")(
              searchForm(title, s"$searchFilter${searchFilter.nonEmpty.so(" ")}"),
              bits.orderSelect(order, active, url),
              bits.newForm()
            ),
            topics.map: ts =>
              div(cls := "box__pad")(topic.topicsList(ts, Order.mine)),
            paginate(pag, url(order))
          )
        )

  private def paginate(pager: Paginator[WithChaptersAndLiked], url: Call)(using Context) =
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
    val nonMineOrder = if order == Order.mine then Order.hot else order
    lila.ui.bits.pageMenuSubnav(
      a(cls := active.active("all"), href := routes.Study.all(nonMineOrder))(trs.allStudies()),
      ctx.isAuth.option(bits.authLinks(active, nonMineOrder)),
      a(cls := List("active" -> active.startsWith("topic")), href := routes.Study.topics):
        trs.topics()
      ,
      topics.map: topic =>
        a(cls := active.active(s"topic:$topic"), href := routes.Study.byTopic(topic.value, order))(
          topic.value
        ),
      a(cls := active.active("staffPicks"), href := routes.Study.staffPicks)("Staff picks"),
      a(
        cls := "text",
        dataIcon := Icon.InfoCircle,
        href := "/@/lichess/blog/study-chess-the-lichess-way/V0KrLSkA"
      )(trs.whatAreStudies())
    )

  def searchForm(placeholder: String, value: String) =
    form(cls := "search", action := routes.Study.search(), method := "get")(
      input(name := "q", st.placeholder := placeholder, st.value := value, enterkeyhint := "search"),
      submitButton(cls := "button", dataIcon := Icon.Search)
    )

  object topic:

    def topicsList(topics: StudyTopics, order: Order = Orders.default) =
      div(cls := "topic-list")(
        topics.value.map: t =>
          a(href := routes.Study.byTopic(t.value, order))(t.value)
      )

    def index(popular: StudyTopics, mine: Option[StudyTopics], myForm: Option[Form[?]])(using Context) =
      Page(trans.study.topics.txt())
        .css("analyse.study.index", "bits.form3", "bits.tagify")
        .js(Esm("analyse.study.topic.form")):
          main(cls := "page-menu")(
            menu("topic", Order.mine, mine.so(_.value)),
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
        pag: Paginator[WithChaptersAndLiked],
        order: Order,
        myTopics: Option[StudyTopics]
    )(using Context) =
      val active = s"topic:$topic"
      val url = (o: Order) => routes.Study.byTopic(topic.value, o)
      Page(topic.value)
        .css("analyse.study.index")
        .js(infiniteScrollEsmInit):
          main(cls := "page-menu")(
            menu(active, order, myTopics.so(_.value)),
            main(cls := "page-menu__content study-index box")(
              boxTop(
                h1(topic.value),
                bits.orderSelect(order, active, url),
                bits.newForm()
              ),
              myTopics.ifTrue(order == Order.mine).map { ts =>
                div(cls := "box__pad")(
                  topicsList(ts, Order.mine)
                )
              },
              paginate(pag, url(order))
            )
          )
