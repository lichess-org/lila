package views.study

import play.api.data.Form

import lila.app.templating.Environment.{ *, given }

import scalalib.paginator.Paginator
import lila.study.Study.WithChaptersAndLiked
import lila.study.{ Order, StudyTopic, StudyTopics }
import trans.{ study as trs }

object list:

  lazy val ui = lila.study.ui.ListUi(helpers, bits)
  import ui.*

  def all(pag: Paginator[WithChaptersAndLiked], order: Order)(using PageContext) =
    layout(
      title = trs.allStudies.txt(),
      active = "all",
      order = order,
      pag = pag,
      searchFilter = "",
      url = o => routes.Study.all(o),
      withHrefLangs = lila.ui.LangPath(routes.Study.allDefault()).some
    )

  def byOwner(pag: Paginator[WithChaptersAndLiked], order: Order, owner: User)(using PageContext) =
    layout(
      title = trs.studiesCreatedByX.txt(owner.titleUsername),
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
      title = trs.myStudies.txt(),
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
      title = trs.myFavoriteStudies.txt(),
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
      title = trs.studiesIContributeTo.txt(),
      active = "mineMember",
      order = order,
      pag = pag,
      searchFilter = s"member:${me.username}",
      url = o => routes.Study.mineMember(o),
      topics = topics.some
    )

  def minePublic(pag: Paginator[WithChaptersAndLiked], order: Order)(using PageContext)(using me: Me) =
    layout(
      title = trs.myPublicStudies.txt(),
      active = "minePublic",
      order = order,
      pag = pag,
      searchFilter = s"owner:${me.username}",
      url = o => routes.Study.minePublic(o)
    )

  def minePrivate(pag: Paginator[WithChaptersAndLiked], order: Order)(using PageContext)(using me: Me) =
    layout(
      title = trs.myPrivateStudies.txt(),
      active = "minePrivate",
      order = order,
      pag = pag,
      searchFilter = s"owner:${me.username}",
      url = o => routes.Study.minePrivate(o)
    )

  def search(pag: Paginator[WithChaptersAndLiked], text: String)(using PageContext) =
    views.base.layout(
      title = text,
      moreCss = cssTag("study.index"),
      wrapClass = "full-screen-force",
      modules = infiniteScrollEsmInit
    )(ui.search(pag, text))

  def staffPicks(p: lila.cms.CmsPage.Render)(using PageContext) =
    views.base.layout(title = p.title, moreCss = frag(cssTag("study.index"), cssTag("page"))):
      main(cls := "page-menu")(
        menu("staffPicks", Order.Mine, Nil),
        main(cls := "page-menu__content box box-pad page"):
          views.site.page.pageContent(p)
      )

  private def layout(
      title: String,
      active: String,
      order: Order,
      pag: Paginator[WithChaptersAndLiked],
      url: String => Call,
      searchFilter: String,
      topics: Option[StudyTopics] = None,
      withHrefLangs: Option[lila.ui.LangPath] = None
  )(using PageContext): Frag =
    views.base.layout(
      title = title,
      moreCss = cssTag("study.index"),
      wrapClass = "full-screen-force",
      modules = infiniteScrollEsmInit,
      withHrefLangs = withHrefLangs
    ):
      main(cls := "page-menu")(
        menu(active, order, topics.so(_.value)),
        main(cls := "page-menu__content study-index box")(
          div(cls := "box__top")(
            searchForm(title, s"$searchFilter${searchFilter.nonEmpty.so(" ")}"),
            bits.orderSelect(order, active, url),
            bits.newForm()
          ),
          topics.map: ts =>
            div(cls := "box__pad")(ui.topic.topicsList(ts, Order.Mine)),
          paginate(pag, url(order.key))
        )
      )

object topic:

  def index(popular: StudyTopics, mine: Option[StudyTopics], myForm: Option[Form[?]])(using PageContext) =
    views.base.layout(
      title = trans.study.topics.txt(),
      moreCss = frag(cssTag("study.index"), cssTag("form3"), cssTag("tagify")),
      modules = EsmInit("analyse.study.topic.form"),
      wrapClass = "full-screen-force"
    )(list.ui.topic.index(popular, mine, myForm))

  def show(
      topic: StudyTopic,
      pag: Paginator[WithChaptersAndLiked],
      order: Order,
      myTopics: Option[StudyTopics]
  )(using PageContext) =
    views.base.layout(
      title = topic.value,
      moreCss = cssTag("study.index"),
      wrapClass = "full-screen-force",
      modules = infiniteScrollEsmInit
    )(list.ui.topic.show(topic, pag, order, myTopics))
