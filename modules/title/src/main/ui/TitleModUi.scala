package lila.title
package ui

import lila.ui.*
import ScalatagsTemplate.{ *, given }

final class TitleModUi(helpers: Helpers)(ui: TitleUi, picfitUrl: lila.core.misc.PicfitUrl):
  import helpers.{ *, given }

  def queue(reqs: List[TitleRequest])(using ctx: Context): Frag =
    table(cls := "slist slist-pad see title-queue")(
      thead(tr(th("By"), th("As"), th("Date"), th)),
      tbody(
        reqs.map: r =>
          tr(
            td(userIdLink(r.userId.some, params = "?mod")),
            td(userTitleTag(r.data.title), " ", r.data.realName),
            td(showDate(r.history.head.at)),
            td(a(href := routes.TitleVerify.show(r.id), cls := "button button-empty")("View"))
          )
      )
    )

  def show(req: TitleRequest, user: User)(using Context) =
    Page(s"${user.username}'s title verification")
      .css("bits.titleRequest"):
        main(cls := "box box-pad page")(body)
