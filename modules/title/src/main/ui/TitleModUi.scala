package lila.title
package ui

import lila.core.config.NetDomain
import lila.core.id.ImageId
import lila.ui.*

import ScalatagsTemplate.{ *, given }

final class TitleModUi(helpers: Helpers)(ui: TitleUi)(using NetDomain):
  import helpers.{ *, given }

  def queue(reqs: List[TitleRequest])(using ctx: Context): Frag =
    table(cls := "slist slist-pad see title-queue")(
      thead(tr(th("By"), th("As"), th("Date"), th)),
      tbody(
        reqs.map: r =>
          tr(
            td(userIdLink(r.userId.some, params = "?mod")),
            td(userTitleTag(r.data.title), " ", r.data.realName),
            td(momentFromNow(r.history.head.at)),
            td(a(href := routes.TitleVerify.show(r.id), cls := "button button-empty")("View"))
          )
      )
    )

  def show(req: TitleRequest, user: User, fide: Option[Frag], similar: List[TitleRequest], modZone: Frag)(
      using Context
  ) =
    def pictureIfGranted(idOpt: Option[ImageId]) =
      idOpt.flatMap: id =>
        Granter.opt(_.TitleRequest).option(a(href := ui.thumbnail.raw(id))(ui.thumbnail(id.some, 500)))
    Page(s"${user.username}'s title verification")
      .css("bits.titleRequest")
      .css(Granter.opt(_.UserModView).option("mod.user"))
      .js(esmInitBit("titleRequest") ++ Granter.opt(_.UserModView).so(Esm("mod.user"))):
        main(cls := "box box-pad page title-mod")(
          div(cls := "box__top")(
            h1("Title verification by ", userLink(user), " ", showStatus(req.status)),
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
          modZone,
          div(cls := "title-mod__history")(
            h2("History"),
            table(cls := "slist")(
              req.history.toList.reverse.map: h =>
                tr(
                  td(momentFromNow(h.at)),
                  td(showStatus(h.status)),
                  td(h.status.textOpt)
                )
            )
          ),
          div(cls := "title-mod__similar")(
            h2("Similar requests"),
            table(cls := "slist")(
              similar.map: r =>
                tr(
                  td(userIdLink(r.userId.some, params = "?mod")),
                  td(userTitleTag(r.data.title), " ", r.data.realName),
                  td(showStatus(r.status), nbsp, momentFromNow(r.history.head.at)),
                  td(a(href := routes.TitleVerify.show(r.id), cls := "button button-empty")("View request"))
                )
            )
          ),
          div(cls := "title-mod__data")(
            h2("Application"),
            table(cls := "slist")(
              tr(th("Status"), td(showStatus(req.status))),
              tr(th("Requested title"), td(userTitleTag(req.data.title))),
              tr(th("Public"), td(if req.data.public then goodTag("Yes") else badTag("No"))),
              tr(th("Coach"), td(if req.data.coach then goodTag("Yes") else badTag("No"))),
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
                  case Some(url) => a(href := url.toString, targetBlank)(url.toString)
                  case None      => "None")
              ),
              pictureIfGranted(req.idDocument).map: idPic =>
                tr(
                  th("ID document"),
                  td(idPic)
                ),
              pictureIfGranted(req.selfie).map: selfiePic =>
                tr(
                  th("Selfie"),
                  td(selfiePic)
                ),
              tr(th("Comment"), td(req.data.comment.map(richText(_))))
            )
          ),
          Granter
            .opt(_.TitleRequest)
            .option(
              div(cls := "title-mod__actions")(
                postForm(action := routes.TitleVerify.process(req.id))(
                  form3
                    .group(
                      lila.title.TitleForm.process("text"),
                      "Ask for modifications"
                    )(form3.textarea(_)(rows := 2, required)),
                  form3.actions(
                    submitButton(
                      cls   := "button button-red button-empty button-fat",
                      name  := "action",
                      value := "reject",
                      if req.status.is(_.rejected)
                      then disabled := true
                      else attr("formnovalidate").empty
                    )("Reject request"),
                    submitButton(
                      cls   := "button button-blue button-fat",
                      name  := "action",
                      value := "feedback"
                    )(
                      "Ask for modifications"
                    ),
                    submitButton(
                      cls   := "button button-green button-fat",
                      name  := "action",
                      value := "approve",
                      attr("formnovalidate").empty
                    )(
                      "Approve request"
                    )
                  )
                )
              )
            )
        )

  private def showStatus(status: TitleRequest.Status)(using Context) =
    span(cls := s"title__status title__status--${status.name}")(status.name)
