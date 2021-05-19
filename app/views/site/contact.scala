package views.html
package site

import controllers.routes
import scala.util.chaining._

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

object contact {

  import trans.contact._
  import views.html.base.navTree._

  private lazy val contactEmailBase64 = lila.common.String.base64.encode(contactEmailInClear)

  def contactEmailLink(implicit ctx: Context) =
    a(cls := "contact-email-obfuscated", attr("data-email") := contactEmailBase64)(
      trans.clickToRevealEmailAddress()
    )

  private def reopenLeaf(prefix: String)(implicit ctx: Context) =
    Leaf(
      s"$prefix-reopen",
      wantReopen(),
      frag(
        p(a(href := routes.Account.reopen)(reopenOnThisPage())),
        p(doNotAskByEmailToReopen())
      )
    )

  private def howToReportBugs(implicit ctx: Context): Frag =
    frag(
      ul(
        li(
          a(href := routes.ForumCateg.show("lichess-feedback"))(reportBugInForum())
        ),
        li(
          a(href := "https://github.com/ornicar/lila/issues")(reportWebsiteIssue())
        ),
        li(
          a(href := "https://github.com/veloce/lichobile/issues")(reportMobileIssue())
        ),
        li(
          a(href := "https://discord.gg/hy5jqSs")(reportBugInDiscord())
        )
      ),
      p(howToReportBug())
    )

  private def menu(implicit ctx: Context): Branch =
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
            reopenLeaf("login"),
            Leaf(
              "dns",
              "\"This site can’t be reached\"",
              frag(
                p("If you can't reach Lichess, and your browser says something like:"),
                ul(
                  li("This site can't be reached."),
                  li(strong("lichess.org"), "’s server IP address could not be found."),
                  li("We can’t connect to the server at lichess.org.")
                ),
                p("Then you have a ", strong("DNS issue"), "."),
                p(
                  "There's nothing we can do about it, but ",
                  a("here's how you can fix it")(
                    href := "https://www.wikihow.com/Fix-DNS-Server-Not-Responding-Problem"
                  ),
                  "."
                )
              )
            )
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
                a(href := routes.Page.master)(visitTitleConfirmation()),
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
              a(href := routes.Report.form)(toReportAPlayerUseForm()),
              "."
            ),
            p(
              youCanAlsoReachReportPage(button(cls := "thin button button-empty", dataIcon := "!"))
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
                p(a(href := "/learn#/15")(tryEnPassant()), ".")
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
              p(ratedGame())
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
              frag(
                p("Please report security issues to ", contactEmailLink),
                p(
                  "Like all contributions to Lichess, security reviews and pentesting are appreciated. ",
                  "Note that Lichess is built by volunteers and we currently do not have a bug bounty program."
                ),
                p(
                  "Vulnerabilities are relevant even when they are not directly exploitable, ",
                  "for example XSS mitigated by CSP."
                ),
                p(
                  "When doing your research, please minimize negative impact for other users. ",
                  "As long as you keep this in mind, testing should not require prior coordination. ",
                  "Avoid spamming, DDoS and volumetric attacks."
                ),
                p(
                  "We believe transport encryption should be sufficient for all reports. ",
                  "If you insist on using PGP, please clarify the nature of the message ",
                  "in the plain-text subject and encrypt for ",
                  a(href := "/.well-known/gpg.asc")("multiple recipients"),
                  "."
                )
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
        frag(
          p(doNotMessageModerators()),
          p(sendAppealTo(a(href := routes.Appeal.home)(netConfig.domain, routes.Appeal.home.url))),
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
                p("If you are a European citizen, you may request the deletion of your Lichess account."),
                p(
                  "First, ",
                  a(href := routes.Account.close)("close your account"),
                  "."
                ),
                p(
                  "Then send us an email at ",
                  contactEmailLink,
                  " to request the definitive erasure of all data linked to the account."
                )
              )
            ),
            Leaf(
              "contact-other",
              noneOfTheAbove(),
              frag(
                p(sendEmailAt(contactEmailLink)),
                p(explainYourRequest())
              )
            )
          )
        )
      )
    )

  def apply()(implicit ctx: Context) =
    page.layout(
      title = trans.contact.contact.txt(),
      active = "contact",
      moreCss = cssTag("contact"),
      moreJs = jsModule("contact"),
      contentCls = "page box box-pad"
    )(
      frag(
        h1(contactLichess()),
        div(cls := "nav-tree")(renderNode(menu, none))
      )
    )
}
