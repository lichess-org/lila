package lila.title
package ui

import lila.ui.*
import ScalatagsTemplate.{ *, given }
import lila.core.config.NetDomain
import lila.core.id.ImageId

final class TitleModUi(helpers: Helpers)(ui: TitleUi, picfitUrl: lila.core.misc.PicfitUrl)(using NetDomain):
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

  def show(req: TitleRequest, user: User, fide: Option[Frag], modZone: Frag)(using Context) =
    def picture(id: ImageId) = a(href := ui.thumbnail.raw(id))(ui.thumbnail(id.some, 500))
    Page(s"${user.username}'s title verification")
      .css("bits.titleRequest"):
        main(cls := "box box-pad page")(
          div(cls := "box__top")(
            h1("Title verification by ", userLink(user)),
            div(cls := "box__top__actions")(
              a(
                cls  := "button button-empty mod-zone-toggle",
                href := routes.User.mod(user.id),
                titleOrText("Mod zone (Hotkey: m)"),
                dataIcon := Icon.Agent
              )
            )
          ),
          standardFlash,
          div(cls := "title-mod__data")(
            table(cls := "slist")(
              tr(th("Requested title"), td(userTitleTag(req.data.title))),
              tr(th("Real name"), td(req.data.realName)),
              tr(
                th("FIDE profile"),
                td(
                  req.data.fideId match
                    case Some(id) =>
                      fide match
                        case Some(found) => found
                        case None        => badTag("Invalid FIDE ID: ", strong(id))
                    case None => "None"
                )
              ),
              tr(
                th("National federation"),
                td(req.data.federationUrl match
                  case Some(url) => a(href := url.toString)(url.toString)
                  case None      => "None"
                )
              ),
              tr(
                th("ID document"),
                td(req.idDocument.map(picture))
              ),
              tr(
                th("Selfie"),
                td(req.selfie.map(picture))
              ),
              tr(th("Comment"), td(req.data.comment.map(richText(_))))
            )
          )
        )
