package views.html
package appeal

import controllers.routes
import controllers.appeal.routes.{ Appeal as appealRoutes }

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.appeal.Appeal
import lila.report.Report.Inquiry

object queue:

  def apply(
      appeals: List[Appeal.WithUser],
      inquiries: Map[UserId, Inquiry],
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
            th("Last message"),
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
