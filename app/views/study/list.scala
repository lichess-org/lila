package views.html
package study

import play.api.mvc.Call

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.paginator.Paginator
import lila.common.String.html.safeJsonValue
import lila.user.User

import controllers.routes

object list {

  def all(pag: Paginator[lila.study.Study.WithChaptersAndLiked], order: lila.study.Order)(implicit ctx: Context) = layout(
    title = s"All studies",
    active = "all",
    order = order,
    pag = pag,
    searchFilter = "",
    url = o => routes.Study.all(o)
  )("All studies")

  def byOwner(pag: Paginator[lila.study.Study.WithChaptersAndLiked], order: lila.study.Order, owner: User)(implicit ctx: Context) = layout(
    title = s"Studies created by ${owner.titleUsername}",
    active = "owner",
    order = order,
    pag = pag,
    searchFilter = s"owner:${owner.username}",
    url = o => routes.Study.byOwner(owner.username, o)
  )(frag(
    userLink(owner),
    "'s studies"
  ))

  def mine(pag: Paginator[lila.study.Study.WithChaptersAndLiked], order: lila.study.Order, me: User)(implicit ctx: Context) = layout(
    title = s"My studies",
    active = "mine",
    order = order,
    pag = pag,
    searchFilter = s"owner:${me.username}",
    url = o => routes.Study.mine(o)
  )("My studies")

  def mineLikes(
    pag: Paginator[lila.study.Study.WithChaptersAndLiked],
    order: lila.study.Order,
    me: User
  )(implicit ctx: Context) = layout(
    title = "My favourite studies",
    active = "mineLikes",
    order = order,
    pag = pag,
    searchFilter = "",
    url = o => routes.Study.mineLikes(o)
  )("My favourite studies")

  def mineMember(pag: Paginator[lila.study.Study.WithChaptersAndLiked], order: lila.study.Order, me: User)(implicit ctx: Context) = layout(
    title = s"Studies I contribute to",
    active = "mineMember",
    order = order,
    pag = pag,
    searchFilter = s"member:${me.username}",
    url = o => routes.Study.mineMember(o)
  )("Studies I contribute to")

  def minePublic(pag: Paginator[lila.study.Study.WithChaptersAndLiked], order: lila.study.Order, me: User)(implicit ctx: Context) = layout(
    title = "My public studies",
    active = "minePublic",
    order = order,
    pag = pag,
    searchFilter = s"owner:${me.username}",
    url = o => routes.Study.minePublic(o)
  )("My public studies")

  def minePrivate(pag: Paginator[lila.study.Study.WithChaptersAndLiked], order: lila.study.Order, me: User)(implicit ctx: Context) = layout(
    title = "My private studies",
    active = "minePrivate",
    order = order,
    pag = pag,
    searchFilter = s"owner:${me.username}",
    url = o => routes.Study.minePrivate(o)
  )("My private studies")

  private[study] def paginate(pager: Paginator[lila.study.Study.WithChaptersAndLiked], url: Call)(implicit ctx: Context) =
    if (pager.currentPageResults.isEmpty) div(cls := "nostudies")(
      iconTag("4"),
      p("None yet.")
    )
    else div(cls := "list infinitescroll")(
      pager.currentPageResults.map { s =>
        div(cls := "study paginated")(bits.widget(s))
      },
      pager.nextPage.map { np =>
        div(cls := "pager none")(
          a(rel := "next", href := addQueryParameter(url.toString, "page", np))("Next")
        )
      }
    )

  private def orderChoice(
    url: lila.study.Order => Call,
    order: lila.study.Order,
    orders: List[lila.study.Order] = lila.study.Order.all
  )(implicit ctx: Context) = div(cls := "orders mselect")(
    div(cls := "button")(
      order.name,
      iconTag("u")
    ),
    div(cls := "list")(
      orders.map { o =>
        a(href := url(o))(o.name)
      }
    )
  )

  private def layout(
    title: String,
    active: String,
    order: lila.study.Order,
    pag: Paginator[lila.study.Study.WithChaptersAndLiked],
    url: controllers.Study.ListUrl,
    searchFilter: String
  )(titleFrag: Frag)(implicit ctx: Context) = views.html.base.layout(
    title = title,
    menu = Some(frag(
      a(
        cls := active.active("all"),
        href := routes.Study.all(order.key)
      )("All studies"),
      ctx.me.map { bits.authLinks(_, active, order) },
      a(cls := "text", dataIcon := "", href := "//lichess.org/blog/V0KrLSkAAMo3hsi4/study-chess-the-lichess-way")("What are studies?")
    )),
    moreCss = cssTag("studyList.css"),
    moreJs = infiniteScrollTag
  ) {
      div(cls := "content_box no_padding studies")(
        div(cls := "top")(
          form(cls := "search", action := routes.Study.search(), method := "get")(
            input(name := "q", placeholder := title, value := s"$searchFilter${searchFilter.nonEmpty ?? " "}"),
            button(`type` := "submit", cls := "submit button", dataIcon := "y")
          ),
          orderChoice(o => url(o.key), order, if (active == "all") lila.study.Order.allButOldest else lila.study.Order.all),
          bits.newForm()
        ),
        paginate(pag, url(order.key))
      )
    }
}
