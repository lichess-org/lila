package views.html
package study

import play.api.mvc.Call

import lidraughts.api.Context
import lidraughts.app.templating.Environment._
import lidraughts.app.ui.ScalatagsTemplate._
import lidraughts.common.paginator.Paginator
import lidraughts.study.Order
import lidraughts.study.Study.WithChaptersAndLiked
import lidraughts.user.User

import controllers.routes

object list {

  def all(pag: Paginator[WithChaptersAndLiked], order: Order)(implicit ctx: Context) = layout(
    title = trans.study.allStudies.txt(),
    active = "all",
    order = order,
    pag = pag,
    searchFilter = "",
    url = o => routes.Study.all(o)
  )(trans.study.allStudies())

  def byOwner(pag: Paginator[WithChaptersAndLiked], order: Order, owner: User)(implicit ctx: Context) = layout(
    title = trans.study.studiesCreatedByX.txt(owner.titleUsername),
    active = "owner",
    order = order,
    pag = pag,
    searchFilter = s"owner:${owner.username}",
    url = o => routes.Study.byOwner(owner.username, o)
  )(trans.study.studiesCreatedByX(userLink(owner)))

  def mine(pag: Paginator[WithChaptersAndLiked], order: Order, me: User)(implicit ctx: Context) = layout(
    title = trans.study.myStudies.txt(),
    active = "mine",
    order = order,
    pag = pag,
    searchFilter = s"owner:${me.username}",
    url = o => routes.Study.mine(o)
  )(trans.study.myStudies())

  def mineLikes(
    pag: Paginator[WithChaptersAndLiked],
    order: Order,
    me: User
  )(implicit ctx: Context) = layout(
    title = trans.study.myFavoriteStudies.txt(),
    active = "mineLikes",
    order = order,
    pag = pag,
    searchFilter = "",
    url = o => routes.Study.mineLikes(o)
  )(trans.study.myFavoriteStudies())

  def mineMember(pag: Paginator[WithChaptersAndLiked], order: Order, me: User)(implicit ctx: Context) = layout(
    title = trans.study.studiesIContributeTo.txt(),
    active = "mineMember",
    order = order,
    pag = pag,
    searchFilter = s"member:${me.username}",
    url = o => routes.Study.mineMember(o)
  )(trans.study.studiesIContributeTo())

  def minePublic(pag: Paginator[WithChaptersAndLiked], order: Order, me: User)(implicit ctx: Context) = layout(
    title = trans.study.myPublicStudies.txt(),
    active = "minePublic",
    order = order,
    pag = pag,
    searchFilter = s"owner:${me.username}",
    url = o => routes.Study.minePublic(o)
  )(trans.study.myPublicStudies())

  def minePrivate(pag: Paginator[WithChaptersAndLiked], order: Order, me: User)(implicit ctx: Context) = layout(
    title = trans.study.myPrivateStudies.txt(),
    active = "minePrivate",
    order = order,
    pag = pag,
    searchFilter = s"owner:${me.username}",
    url = o => routes.Study.minePrivate(o)
  )(trans.study.myPrivateStudies())

  def search(pag: Paginator[WithChaptersAndLiked], text: String)(implicit ctx: Context) =
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
              searchForm(trans.search.txt(), text),
              bits.newForm()
            ),
            paginate(pag, routes.Study.search(text))
          )
        )
      }

  private[study] def paginate(pager: Paginator[WithChaptersAndLiked], url: Call)(implicit ctx: Context) =
    if (pager.currentPageResults.isEmpty) div(cls := "nostudies")(
      iconTag("4"),
      p(trans.study.noneYet())
    )
    else div(cls := "studies list infinitescroll")(
      pager.currentPageResults.map { s =>
        div(cls := "study paginated")(bits.widget(s))
      },
      pagerNext(pager, np => addQueryParameter(url.url, "page", np))
    )

  private[study] def menu(active: String, order: Order)(implicit ctx: Context) =
    st.aside(cls := "page-menu__menu subnav")(
      a(cls := active.active("all"), href := routes.Study.all(order.key))(trans.study.allStudies()),
      ctx.me.map { bits.authLinks(_, active, order) }
    // a(cls := "text", dataIcon := "", href := "/blog/V0KrLSkAAMo3hsi4/study-chess-the-lichess-way")(trans.study.whatAreStudies())
    )

  private[study] def searchForm(placeholder: String, value: String) =
    form(cls := "search", action := routes.Study.search(), method := "get")(
      input(name := "q", st.placeholder := placeholder, st.value := value),
      submitButton(cls := "button", dataIcon := "y")
    )

  private def layout(
    title: String,
    active: String,
    order: Order,
    pag: Paginator[WithChaptersAndLiked],
    url: controllers.Study.ListUrl,
    searchFilter: String
  )(titleFrag: Frag)(implicit ctx: Context) = views.html.base.layout(
    title = title,
    moreCss = cssTag("study.index"),
    wrapClass = "full-screen-force",
    moreJs = infiniteScrollTag
  ) {
      main(cls := "page-menu")(
        menu(active, order),
        main(cls := "page-menu__content study-index box")(
          div(cls := "box__top")(
            searchForm(title, s"$searchFilter${searchFilter.nonEmpty ?? " "}"),
            views.html.base.bits.mselect(
              "orders",
              span(order.name()),
              (if (active == "all") Order.allButOldest else Order.all) map { o =>
                a(href := url(o.key), cls := (order == o).option("current"))(o.name())
              }
            ),
            bits.newForm()
          ),
          paginate(pag, url(order.key))
        )
      )
    }
}
