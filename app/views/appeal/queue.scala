package views.appeal

import lila.app.UiEnv.{ *, given }
import lila.appeal.Appeal
import lila.report.ui.PendingCounts
import lila.report.Report.Inquiry
import lila.core.misc.AppealTopic

object queue:

  def apply(
      appeals: List[Appeal],
      inquiries: Map[UserId, Inquiry],
      topic: Option[AppealTopic],
      markedByMe: Set[UserId],
      scores: lila.report.Room.Scores,
      pending: PendingCounts
  )(using Context, Me) =
    views.report.ui.list.layout("appeal", scores, pending, moreJs = esmInitBit("appealTopicSelect"))(
      views.mod.ui.reportMenu
    ):
      table(cls := "slist slist-pad see appeal-queue")(
        thead(
          tr(
            th(topicFilter(topic)),
            th,
            th(isGranted(_.Presets).option(a(href := routes.Mod.presets("appeal"))("Presets")))
          )
        ),
        tbody(
          appeals.map { appeal =>
            tr(cls := List("new" -> appeal.isUnread))(
              td(
                userIdLink(appeal.user.some, params = "?mod"),
                br,
                span(cls := "appeal-topic")(appeal.topic.key),
                markedByMe
                  .contains(appeal.user)
                  .option(
                    span(
                      dataIcon := Icon.CautionTriangle,
                      cls := "marked-by-me text"
                    )("My mark")
                  )
              ),
              td(appeal.msgs.lastOption.map: msg =>
                frag(
                  userIdLink(msg.by.some),
                  " ",
                  momentFromNowOnce(msg.at),
                  p(shorten(msg.text, 200))
                )),
              td(
                a(href := routes.Appeal.modShow(appeal.user, appeal.topic), cls := "button button-empty")(
                  "View"
                ),
                for i <- inquiries.get(appeal.user)
                yield frag(userIdLink(i.mod.some), nbsp, "is handling this")
              )
            )
          }
        )
      )

  private def topicFilter(current: Option[AppealTopic]) =
    select(cls := "appeal-filters"):
      val choices = "all" :: AppealTopic.values.map(_.key).toList
      choices.map: key =>
        st.option(value := key, (key == current.fold("all")(_.key)).option(selected))(key)
