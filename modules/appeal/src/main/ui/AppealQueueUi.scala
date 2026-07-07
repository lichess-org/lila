package lila.appeal
package ui

import lila.core.userId.ModId
import lila.ui.*
import lila.ui.ScalatagsTemplate.{ *, given }

final class AppealQueueUi(helpers: Helpers):
  import helpers.{ *, given }

  def apply(
      appeals: List[Appeal],
      inquiries: Map[UserId, ModId],
      topic: Option[AppealTopic],
      markedByMe: Set[UserId]
  )(using Context, Me) =
    table(cls := "slist slist-pad see appeal-queue")(
      thead(
        tr(
          th(topicFilter(topic)),
          th,
          th(Granter(_.Presets).option(a(href := routes.Mod.presets("appeal"))("Presets")))
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
              for modId <- inquiries.get(appeal.user)
              yield frag(userIdLink(modId.some), nbsp, "is handling this")
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
