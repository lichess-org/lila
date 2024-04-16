package views.html.challenge

import controllers.routes

import lila.app.templating.Environment.{ *, given }
import lila.ui.ScalatagsTemplate.{ *, given }
import lila.challenge.Challenge.Status
import lila.core.LightUser

object mine:

  def apply(
      c: lila.challenge.Challenge,
      json: play.api.libs.json.JsObject,
      friends: Seq[LightUser],
      error: Option[String],
      color: Option[chess.Color]
  )(using ctx: PageContext) =

    val cancelForm =
      postForm(action := routes.Challenge.cancel(c.id), cls := "cancel xhr"):
        submitButton(cls := "button button-red text", dataIcon := Icon.X)(trans.site.cancel())

    views.html.base.layout(
      title = challengeTitle(c),
      openGraph = challengeOpenGraph(c).some,
      pageModule = bits.jsModule(c, json, owner = true).some,
      moreCss = cssTag("challenge.page")
    ):
      val challengeLink = s"$netBaseUrl${routes.Round.watcher(c.id, "white")}"
      main(cls := s"page-small challenge-page box box-pad challenge--${c.status.name}")(
        c.status match
          case Status.Created | Status.Offline =>
            div(id := "ping-challenge")(
              h1(cls := "box__top")(
                if c.isOpen then c.name | "Open challenge" else trans.challenge.challengeToPlay.txt()
              ),
              bits.details(c, color),
              c.destUserId
                .map { destId =>
                  div(cls := "waiting")(
                    userIdLink(destId.some, cssClass = "target".some),
                    if c.clock.isEmpty then
                      div(cls := "correspondence-waiting text", dataIcon := Icon.Checkmark):
                        "Challenge sent"
                    else spinner,
                    p(trans.site.waitingForOpponent())
                  )
                }
                .getOrElse {
                  if c.isOpen then
                    div(cls := "waiting")(
                      spinner,
                      p(trans.site.waitingForOpponent())
                    )
                  else
                    div(cls := "invite")(
                      div(
                        h2(cls := "ninja-title", trans.site.toInviteSomeoneToPlayGiveThisUrl()),
                        br,
                        p(cls := "challenge-id-form")(
                          input(
                            id         := "challenge-id",
                            cls        := "copyable autoselect",
                            spellcheck := "false",
                            readonly,
                            value := challengeLink,
                            size  := challengeLink.length
                          ),
                          button(
                            title    := "Copy URL",
                            cls      := "copy button",
                            dataRel  := "challenge-id",
                            dataIcon := Icon.Link
                          )
                        ),
                        p(trans.site.theFirstPersonToComeOnThisUrlWillPlayWithYou())
                      ),
                      ctx.isAuth.option(
                        div(cls := "invite__user")(
                          h2(cls := "ninja-title", trans.challenge.inviteLichessUser()),
                          friends.nonEmpty.option(
                            div(cls := "invite__user__recent")(
                              friends.map: user =>
                                button(cls := "button", dataUser := user.name):
                                  lightUserSpan(user, withOnline = true)
                            )
                          ),
                          br,
                          postForm(
                            cls    := "user-invite complete-parent",
                            action := routes.Challenge.toFriend(c.id)
                          )(
                            input(
                              name        := "username",
                              cls         := "friend-autocomplete",
                              placeholder := trans.search.search.txt()
                            ),
                            error.map { p(cls := "error")(_) }
                          )
                        )
                      ),
                      div(cls := "invite__qrcode")(
                        h2(cls := "ninja-title", trans.site.orLetYourOpponentScanQrCode()),
                        img(
                          src := s"https://api.qrserver.com/v1/create-qr-code/?size=150x150&data=$challengeLink",
                          alt := "QR Code"
                        )
                      )
                    )
                },
              c.notableInitialFen.map { fen =>
                frag(
                  br,
                  div(cls := "board-preview", views.html.board.bits.mini(fen.board, c.finalColor)(div))
                )
              },
              (!c.isOpen).option(cancelForm)
            )
          case Status.Declined =>
            div(cls := "follow-up")(
              h1(cls := "box__top")(trans.challenge.challengeDeclined()),
              blockquote(cls := "challenge-reason pull-quote")(
                p(c.anyDeclineReason.trans()),
                footer(userIdLink(c.destUserId))
              ),
              bits.details(c, color),
              a(cls := "button button-fat", href := routes.Lobby.home)(trans.site.newOpponent())
            )
          case Status.Accepted =>
            div(cls := "follow-up")(
              h1(cls := "box__top")(trans.challenge.challengeAccepted()),
              bits.details(c, color),
              a(id := "challenge-redirect", href := routes.Round.watcher(c.id, "white"), cls := "button-fat"):
                trans.site.joinTheGame()
            )
          case Status.Canceled =>
            div(cls := "follow-up")(
              h1(cls := "box__top")(trans.challenge.challengeCanceled()),
              bits.details(c, color),
              a(cls := "button button-fat", href := routes.Lobby.home)(trans.site.newOpponent())
            )
      )
