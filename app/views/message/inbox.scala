package views.html.message

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.paginator.Paginator

import controllers.routes

object inbox {

  def apply(me: lila.user.User, threads: Paginator[lila.message.Thread])(implicit ctx: Context) =
    views.html.base.layout(
      title = trans.inbox.txt(),
      moreCss = cssTag("message"),
      moreJs = frag(infiniteScrollTag, jsTag("message.js"))
    ) {
        main(cls := "message-list box")(
          div(cls := "box__top")(
            h1(trans.inbox()),
            div(cls := "box__top__actions")(
              threads.nbResults > 0 option frag(
                select(cls := "select")(
                  option(value := "")("Select"),
                  option(value := "all")("All"),
                  option(value := "none")("None"),
                  option(value := "unread")("Unread"),
                  option(value := "read")("Read")
                ),
                select(cls := "action")(
                  option(value := "")("Do"),
                  option(value := "unread")("Mark as unread"),
                  option(value := "read")("Mark as read"),
                  option(value := "delete")("Delete")
                )
              ),
              a(href := routes.Message.form, cls := "button button-green text", dataIcon := "m")(trans.composeMessage())
            )
          ),
          table(cls := "slist slist-pad")(
            if (threads.nbResults > 0) tbody(cls := "infinitescroll")(
              pagerNextTable(threads, p => routes.Message.inbox(p).url),
              threads.currentPageResults.map { thread =>
                tr(cls := List(
                  "paginated" -> true,
                  "new" -> thread.isUnReadBy(me),
                  "mod" -> thread.asMod
                ))(
                  td(cls := "author")(userIdLink(thread.visibleOtherUserId(me), none)),
                  td(cls := "subject")(a(href := s"${routes.Message.thread(thread.id)}#bottom")(thread.name)),
                  td(cls := "date")(momentFromNow(thread.updatedAt)),
                  td(cls := "check")(input(tpe := "checkbox", name := "threads", value := thread.id))
                )
              }
            )
            else tbody(tr(td(trans.noNewMessages(), br, br)))
          )
        )
      }
}
