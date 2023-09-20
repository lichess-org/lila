package views.html
package appeal

import controllers.routes
import controllers.appeal.routes.{ Appeal as appealRoutes }

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.appeal.Appeal
import Appeal.Filter
import lila.report.Report.Inquiry

object queue:

  def apply(
      appeals: List[Appeal.WithUser],
      inquiries: Map[UserId, Inquiry],
      filter: Option[Filter],
      markedByMe: Set[UserId],
      scores: lila.report.Room.Scores,
      streamers: Int,
      nbAppeals: Int
  )(using PageContext) =
    views.html.report.list.layout("appeal", scores, streamers, nbAppeals)(
      table(cls := "slist slist-pad see appeal-queue")(
        thead(
          tr(
            th("By"),
            th("Last message", filterMarks(filter)),
            th(isGranted(_.Presets) option a(href := routes.Mod.presets("appeal"))("Presets"))
          )
        ),
        tbody(
          appeals.map { case Appeal.WithUser(appeal, user) =>
            tr(cls := List("new" -> appeal.isUnread))(
              td(
                userIdLink(appeal.id.some, params = "?mod"),
                br,
                markedByMe.contains(appeal.userId) option span(
                  dataIcon := licon.CautionTriangle,
                  cls      := "marked-by-me text"
                )(
                  "My mark"
                ),
                views.html.user.mod.userMarks(user, None)
              ),
              td(appeal.msgs.lastOption map { msg =>
                frag(
                  userIdLink(msg.by.some),
                  " ",
                  momentFromNowOnce(msg.at),
                  p(shorten(msg.text, 200))
                )
              }),
              td(
                a(href := appealRoutes.show(appeal.id), cls := "button button-empty")("View"),
                inquiries.get(appeal.userId) map { i =>
                  frag(userIdLink(i.mod.some), nbsp, "is handling this")
                }
              )
            )
          }
        )
      )
    )

  private def filterMark(
      filter: Option[Filter],
      enabled: Filter => Boolean,
      newFilter: Filter,
      icon: licon.Icon
  ) =
    val goTo = filter.fold(newFilter.some)(_.toggle(newFilter))

    filterLink(goTo, i(cls := List("appeal-filters--enabled" -> ~filter.map(enabled)), dataIcon := icon))

  private def filterLink(goTo: Option[Filter], frag: Frag) =
    a(href := appealRoutes.queue(goTo.map(_.key)))(frag)

  private def filterMarks(filter: Option[Filter]) =
    span(cls := "appeal-filters")(
      filterMark(filter, _.troll, Filter.Troll, licon.BubbleSpeech),
      filterMark(filter, _.boost, Filter.Boost, licon.LineGraph),
      filterMark(filter, _.engine, Filter.Engine, licon.Cogs),
      filterLink(
        filter.flatMap(_.toggle(Filter.Alt)),
        i(cls := List("appeal-filters--enabled" -> ~filter.map(_.alt)))("A")
      ),
      filterMark(filter, _.clean, Filter.Clean, licon.User)
    )
