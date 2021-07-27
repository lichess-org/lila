package views.html
package appeal

import controllers.routes

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.appeal.Appeal
import lila.report.Report.Inquiry
import lila.user.User

object queue {

  def apply(
      appeals: List[Appeal.WithUser],
      inquiries: Map[User.ID, Inquiry],
      scores: lila.report.Room.Scores,
      streamers: Int,
      nbAppeals: Int
  )(implicit ctx: Context) =
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
                userIdLink(appeal.id.some),
                br,
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
                a(href := routes.Appeal.show(appeal.id), cls := "button button-empty")("View"),
                inquiries.get(appeal.id) map { i =>
                  frag(userIdLink(i.mod.some), nbsp, "is handling this")
                }
              )
            )
          }
        )
      )
    )
}
