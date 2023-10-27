package views.html
package study

import controllers.routes
import play.api.mvc.Call

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.common.LangPath
import lila.common.paginator.Paginator
import lila.study.Study.WithChaptersAndLiked
import lila.study.{ Order, StudyTopic, StudyTopics }
import lila.user.User

object list:

  def all(pag: Paginator[WithChaptersAndLiked], order: Order)(using PageContext) =
    layout(
      title = trans.study.allStudies.txt(),
      active = "all",
      order = order,
      pag = pag,
      searchFilter = "",
      url = o => routes.Study.all(o),
      withHrefLangs = LangPath(routes.Study.allDefault()).some
    )

  def byOwner(pag: Paginator[WithChaptersAndLiked], order: Order, owner: User)(using PageContext) =
    layout(
      title = trans.study.studiesCreatedByX.txt(owner.titleUsername),
      active = "owner",
      order = order,
      pag = pag,
      searchFilter = s"owner:${owner.username}",
      url = o => routes.Study.byOwner(owner.username, o)
    )

  def mine(pag: Paginator[WithChaptersAndLiked], order: Order, topics: StudyTopics)(using
      ctx: PageContext,
      me: Me
  ) =
    layout(
      title = trans.study.myStudies.txt(),
      active = "mine",
      order = order,
      pag = pag,
      searchFilter = s"owner:${me.username}",
      url = o => routes.Study.mine(o),
      topics = topics.some
    )

  def mineLikes(
      pag: Paginator[WithChaptersAndLiked],
      order: Order
  )(using PageContext) =
    layout(
      title = trans.study.myFavoriteStudies.txt(),
      active = "mineLikes",
      order = order,
      pag = pag,
      searchFilter = "",
      url = o => routes.Study.mineLikes(o)
    )

  def mineMember(pag: Paginator[WithChaptersAndLiked], order: Order, topics: StudyTopics)(using
      ctx: PageContext,
      me: Me
  ) =
    layout(
      title = trans.study.studiesIContributeTo.txt(),
      active = "mineMember",
      order = order,
      pag = pag,
      searchFilter = s"member:${me.username}",
      url = o => routes.Study.mineMember(o),
      topics = topics.some
    )

  def minePublic(pag: Paginator[WithChaptersAndLiked], order: Order)(using PageContext)(using me: Me) =
    layout(
      title = trans.study.myPublicStudies.txt(),
      active = "minePublic",
      order = order,
      pag = pag,
      searchFilter = s"owner:${me.username}",
      url = o => routes.Study.minePublic(o)
    )

  def minePrivate(pag: Paginator[WithChaptersAndLiked], order: Order)(using PageContext)(using me: Me) =
    layout(
      title = trans.study.myPrivateStudies.txt(),
      active = "minePrivate",
      order = order,
      pag = pag,
      searchFilter = s"owner:${me.username}",
      url = o => routes.Study.minePrivate(o)
    )

  def search(pag: Paginator[WithChaptersAndLiked], text: String)(using PageContext) =
    views.html.base.layout(
      title = text,
      moreCss = cssTag("study.index"),
      wrapClass = "full-screen-force",
      moreJs = infiniteScrollTag
    ) {
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
    }

  def staffPicks(doc: io.prismic.Document, resolver: io.prismic.DocumentLinkResolver)(using PageContext) =
    views.html.base.layout(
      title = ~doc.getText("doc.title"),
      moreCss = frag(cssTag("study.index"), cssTag("page"))
    ):
      main(cls := "page-menu")(
        menu("staffPicks", Order.Mine, Nil),
        main(cls := "page-menu__content box box-pad page"):
          views.html.site.page.pageContent(doc, resolver)
      )

  private[study] def paginate(pager: Paginator[WithChaptersAndLiked], url: Call)(using PageContext) =
    if pager.currentPageResults.isEmpty then
      div(cls := "nostudies")(
        iconTag(licon.StudyBoard),
        p(trans.study.noneYet())
      )
    else
      div(cls := "studies list infinite-scroll")(
        pager.currentPageResults.map { s =>
          div(cls := "study paginated")(bits.widget(s))
        },
        pagerNext(pager, np => addQueryParam(url.url, "page", np.toString))
      )

  private[study] def menu(active: String, order: Order, topics: List[StudyTopic] = Nil)(using
      ctx: PageContext
  ) =
    val nonMineOrder = if order == Order.Mine then Order.Hot else order
    views.html.site.bits.pageMenuSubnav(
      a(cls := active.active("all"), href := routes.Study.all(nonMineOrder.key))(trans.study.allStudies()),
      ctx.isAuth option bits.authLinks(active, nonMineOrder),
      a(cls := List("active" -> active.startsWith("topic")), href := routes.Study.topics):
        trans.study.topics()
      ,
      topics.map: topic =>
        a(cls := active.active(s"topic:$topic"), href := routes.Study.byTopic(topic.value, order.key))(
          topic.value
        ),
      a(cls := active.active("staffPicks"), href := routes.Study.staffPicks)("Staff picks"),
      a(
        cls      := "text",
        dataIcon := licon.InfoCircle,
        href     := "/blog/V0KrLSkAAMo3hsi4/study-chess-the-lichess-way"
      ):
        trans.study.whatAreStudies()
    )

  private[study] def searchForm(placeholder: String, value: String) =
    form(cls := "search", action := routes.Study.search(), method := "get")(
      input(name       := "q", st.placeholder := placeholder, st.value := value, enterkeyhint := "search"),
      submitButton(cls := "button", dataIcon  := licon.Search)
    )

  private def layout(
      title: String,
      active: String,
      order: Order,
      pag: Paginator[WithChaptersAndLiked],
      url: String => Call,
      searchFilter: String,
      topics: Option[StudyTopics] = None,
      withHrefLangs: Option[LangPath] = None
  )(using PageContext) =
    views.html.base.layout(
      title = title,
      moreCss = cssTag("study.index"),
      wrapClass = "full-screen-force",
      moreJs = infiniteScrollTag,
      withHrefLangs = withHrefLangs
    ) {
      main(cls := "page-menu")(
        menu(active, order, topics.so(_.value)),
        main(cls := "page-menu__content study-index box")(
          div(cls := "box__top")(
            searchForm(title, s"$searchFilter${searchFilter.nonEmpty so " "}"),
            bits.orderSelect(order, active, url),
            bits.newForm()
          ),
          topics map { ts =>
            div(cls := "box__pad")(
              views.html.study.topic.topicsList(ts, Order.Mine)
            )
          },
          paginate(pag, url(order.key))
        )
      )
    }
