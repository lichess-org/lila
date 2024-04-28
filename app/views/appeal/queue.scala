package views.appeal

import lila.app.templating.Environment.{ *, given }

import lila.appeal.Appeal
import lila.report.Report.Inquiry

import Appeal.Filter

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
    views.report.list.layout("appeal", scores, streamers, nbAppeals)(
      table(cls := "slist slist-pad see appeal-queue")(
        thead(
          tr(
            th("By"),
            th(filterMarks(filter)),
            th(isGranted(_.Presets).option(a(href := routes.Mod.presets("appeal"))("Presets")))
          )
        ),
        tbody(
          appeals.map { case Appeal.WithUser(appeal, user) =>
            tr(cls := List("new" -> appeal.isUnread))(
              td(
                userIdLink(appeal.id.some, params = "?mod"),
                br,
                markedByMe
                  .contains(appeal.userId)
                  .option(
                    span(
                      dataIcon := Icon.CautionTriangle,
                      cls      := "marked-by-me text"
                    )("My mark")
                  ),
                views.mod.user.userMarks(user, None)
              ),
              td(appeal.msgs.lastOption.map: msg =>
                frag(
                  userIdLink(msg.by.some),
                  " ",
                  momentFromNowOnce(msg.at),
                  p(shorten(msg.text, 200))
                )),
              td(
                a(href := routes.Appeal.show(appeal.id), cls := "button button-empty")("View"),
                inquiries.get(appeal.userId).map { i =>
                  frag(userIdLink(i.mod.some), nbsp, "is handling this")
                }
              )
            )
          }
        )
      )
    )

  private def filterMarks(current: Option[Filter]) =
    span(cls := "appeal-filters btn-rack"):
      Filter.allWithIcon.map: (filter, icon) =>
        a(
          cls := List("btn-rack__btn" -> true, "active" -> current.has(filter)),
          href := routes.Appeal.queue(
            current.fold(filter.some)(_.toggle(filter)).fold("reset")(_.key).some
          ),
          dataIcon := icon.left.toOption
        )(icon.toOption)
