package views.html
package site

import controllers.appeal.routes.{ Appeal as appealRoutes }
import controllers.report.routes.{ Report as reportRoutes }
import controllers.routes
import scala.util.chaining.*

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }

object contact:

  import trans.contact.*
  import views.html.base.navTree.*
  import views.html.base.navTree.Node.*

  def contactEmailLinkEmpty(email: String = contactEmailInClear) =
    a(cls := "contact-email-obfuscated", attr("data-email") := lila.common.String.base64.encode(email))
  def contactEmailLink(email: String = contactEmailInClear)(using PageContext) =
    contactEmailLinkEmpty(email)(trans.clickToRevealEmailAddress())

  private def reopenLeaf(prefix: String)(using PageContext) =
    Leaf(
      s"$prefix-reopen",
      wantReopen(),
      frag(
        p(a(href := routes.Account.reopen)(reopenOnThisPage())),
        p(doNotAskByEmailToReopen())
      )
    )

  private def howToReportBugs(using PageContext): Frag =
    frag(
      ul(
        li(
          a(href := routes.ForumCateg.show("lichess-feedback"))(reportBugInForum())
        ),
        li(
          a(href := "https://github.com/lichess-org/lila/issues")(reportWebsiteIssue())
        ),
        li(
          a(href := "https://github.com/lichess-org/lichobile/issues")(reportMobileIssue())
        ),
        li(
          a(href := "https://discord.gg/lichess")(reportBugInDiscord())
        )
      ),
      p(howToReportBug())
    )

  private def menu(using PageContext): Branch =
    Branch(
      "root",
      whatCanWeHelpYouWith(),
      List(
        Branch(
          "login",
          iCantLogIn(),
          List(
            Leaf(
              "email-confirm",
              noConfirmationEmail(),
              p(
                a(href := routes.Account.emailConfirmHelp)(visitThisPage()),
                "."
              )
            ),
            Leaf(
              "forgot-password",
              forgotPassword(),
              p(
                a(href := routes.Auth.passwordReset)(visitThisPage()),
                "."
              )
            ),
            Leaf(
              "forgot-username",
              forgotUsername(),
              p(
                a(href := routes.Auth.login)(youCanLoginWithEmail()),
                "."
              )
            ),
            Leaf(
              "lost-2fa",
              lost2FA(),
              p(a(href := routes.Auth.passwordReset)(doPasswordReset()), ".")
            ),
            reopenLeaf("login")
          )
        ),
        Branch(
          "account",
          accountSupport(),
          List(
            Leaf(
              "title",
              wantTitle(),
              p(
                a(href := routes.ContentPage.master)(visitTitleConfirmation()),
                "."
              )
            ),
            Leaf(
              "close",
              wantCloseAccount(),
              frag(
                p(a(href := routes.Account.close)(closeYourAccount()), "."),
                p(doNotAskByEmail())
              )
            ),
            reopenLeaf("account"),
            Leaf(
              "change-username",
              wantChangeUsername(),
              frag(
                p(a(href := routes.Account.username)(changeUsernameCase()), "."),
                p(cantChangeMore()),
                p(orCloseAccount())
              )
            ),
            Leaf(
              "clear-history",
              wantClearHistory(),
              frag(
                p(cantClearHistory()),
                p(orCloseAccount())
              )
            )
          )
        ),
        Leaf(
          "report",
          wantReport(),
          frag(
            p(
              a(href := reportRoutes.form)(toReportAPlayerUseForm()),
              "."
            ),
            p(
              youCanAlsoReachReportPage(
                button(cls := "thin button button-empty", dataIcon := licon.CautionTriangle)
              )
            ),
            p(
              doNotMessageModerators(),
              br,
              doNotReportInForum(),
              br,
              doNotSendReportEmails(),
              br,
              onlyReports()
            )
          )
        ),
        Branch(
          "bug",
          wantReportBug(),
          List(
            Leaf(
              "enpassant",
              illegalPawnCapture(),
              frag(
                p(calledEnPassant()),
                p(a(href := "/learn#/15")(tryEnPassant()))
              )
            ),
            Leaf(
              "castling",
              illegalCastling(),
              frag(
                p(castlingPrevented()),
                p(a(href := "https://en.wikipedia.org/wiki/Castling#Requirements")(castlingRules()), "."),
                p(a(href := "/learn#/14")(tryCastling()), "."),
                p(castlingImported())
              )
            ),
            Leaf(
              "insufficient",
              insufficientMaterial(),
              frag(
                p(a(href := faq.fideHandbookUrl)(fideMate()), "."),
                p(knightMate())
              )
            ),
            Leaf(
              "casual",
              noRatingPoints(),
              frag(
                p(ratedGame()),
                botRatingAbuse()
              )
            ),
            Leaf(
              "error-page",
              errorPage(),
              frag(
                p(reportErrorPage()),
                howToReportBugs
              )
            ),
            Leaf(
              "security",
              "Security vulnerability",
              p(
                "Please refer to our ",
                a(href := "https://github.com/lichess-org/lila/security/policy")("Security policy"),
                "."
              )
            ),
            Leaf(
              "other-bug",
              "Other bug",
              frag(
                p("If you found a new bug, you may report it:"),
                howToReportBugs
              )
            )
          )
        ),
        Leaf(
          "broadcast",
          wantBroadcastTournament(),
          frag(
            p(a(href := routes.RelayTour.help)(learnHowToMakeBroadcasts()), "."),
            p(
              contactAboutOfficialBroadcasts(),
              " ",
              sendEmailAt(contactEmailLink("broadcast@lichess.org"))
            )
          )
        ),
        frag(
          p(doNotMessageModerators()),
          p(sendAppealTo(a(href := appealRoutes.home)(netConfig.domain, appealRoutes.home.url))),
          p(
            falsePositives(),
            br,
            ifLegit()
          )
        ) pipe { appealBase =>
          Branch(
            "appeal",
            banAppeal(),
            List(
              Leaf(
                "appeal-cheat",
                engineAppeal(),
                frag(
                  appealBase,
                  p(
                    accountLost(),
                    br,
                    doNotDeny()
                  )
                )
              ),
              Leaf(
                "appeal-other",
                otherRestriction(),
                appealBase
              )
            )
          )
        },
        Branch(
          "collab",
          collaboration(),
          List(
            Leaf(
              "monetize",
              monetizing(),
              frag(
                p("We are not interested in any way of monetizing Lichess."),
                p(
                  "We will never display any kind of ads, we won't track our players, and we won't sell or buy traffic or users."
                ),
                p("Please do not email us about marketing, tracking, or advertising."),
                br,
                p(
                  "We encourage everyone to ",
                  a(href := "/ads")("block all ads and trackers.")
                )
              )
            ),
            Leaf(
              "buy",
              buyingLichess(),
              p("We are not selling, to anyone, for any price. Ever.")
            ),
            Leaf(
              "authorize",
              authorizationToUse(),
              frag(
                p(welcomeToUse()),
                p(videosAndBooks()),
                p(creditAppreciated())
              )
            ),
            Leaf(
              "gdpr",
              "GDPR",
              frag(
                p("You may request the deletion of your Lichess account."),
                p(
                  "First, ",
                  a(href := routes.Account.close)("close your account"),
                  "."
                ),
                p(
                  "Then send us an email at ",
                  contactEmailLink(),
                  " to request the definitive erasure of all data linked to the account."
                )
              )
            ),
            Leaf(
              "dmca",
              "DMCA / Intellectual Property Take Down Notice",
              p(
                a(href := dmcaUrl)("Complete this form"),
                " ",
                "if you are the original copyright holder, or an agent acting on behalf of the copyright holder, and believe Lichess is hosting work(s) you hold the copyright to."
              )
            ),
            Leaf(
              "contact-other",
              noneOfTheAbove(),
              frag(
                p(sendEmailAt(contactEmailLink())),
                p(explainYourRequest())
              )
            )
          )
        )
      )
    )

  val dmcaUrl = "/dmca"

  def apply()(using PageContext) =
    page.layout(
      title = trans.contact.contact.txt(),
      active = "contact",
      moreCss = cssTag("contact"),
      moreJs = jsModule("contact"),
      contentCls = "page box box-pad"
    )(
      frag(
        h1(cls := "box__top")(contactLichess()),
        div(cls := "nav-tree")(renderNode(menu, none))
      )
    )
