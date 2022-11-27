package views.html
package site

import controllers.appeal.routes.{ Appeal as appealRoutes }
import controllers.report.routes.{ Report as reportRoutes }
import controllers.routes
import scala.util.chaining.*

import lila.api.{ Context, given }
import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }

object contact:

  import trans.contact.*
  import views.html.base.navTree.*

  def contactEmailLinkEmpty(email: String = contactEmailInClear) =
    a(cls := "contact-email-obfuscated", attr("data-email") := lila.common.String.base64.encode(email))
  def contactEmailLink(email: String = contactEmailInClear)(implicit ctx: Context) =
    contactEmailLinkEmpty(email)(trans.clickToRevealEmailAddress())

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
                p(reachLichess()),
                ul(
                  li(reachSite()),
                  li(strong("lichess.org"), serverIp()),
                  li(lichessServer())
                ),
                p(haveA(), strong(dns()), "."),
                p(
                  nothing(),
                  a(fixIt())(
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
              a(href := reportRoutes.form)(toReportAPlayerUseForm()),
              "."
            ),
            p(
              youCanAlsoReachReportPage(button(cls := "thin button button-empty", dataIcon := ""))
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
              securityVul(),
              frag(
                p(reportSecurity(), contactEmailLink()),
                p(
                  contribution(),
                  cashBounties()
                ),
                p(
                  relevantVul(),
                  vulExample()
                ),
                p(
                  research(),
                  testing(),
                  spamming()
                ),
                p(
                  encryption(),
                  pgp(),
                  plainText(),
                  a(href := "/.well-known/gpg.asc")("multiple recipients"),
                  "."
                )
              )
            ),
            Leaf(
              "other-bug",
              otherBug(),
              frag(
                p(bugReport()),
                howToReportBugs
              )
            )
          )
        ),
        Leaf(
          "broadcast",
          broadcast(),
          frag(
            p(a(href := routes.RelayTour.help)(broadcastLearn()), "."),
            p(
              broadcastContact(),
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
                p(monetizingInterest()),
                p(
                  ads()
                ),
                p(marketing()),
                br,
                p(
                  encourage(),
                  a(href := "/ads")(adsTrackers())
                )
              )
            ),
            Leaf(
              "buy",
              buyingLichess(),
              p(selling())
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
                p(deleting()),
                p(
                  "First, ",
                  a(href := routes.Account.close)(closeAccount()),
                  "."
                ),
                p(
                  "Then send us an email at ",
                  contactEmailLink(),
                  erasure()
                )
              )
            ),
            Leaf(
              "dmca",
              "DMCA / Intellectual Property Take Down Notice",
              p(
                a(href := dmcaUrl)("Complete this form"),
                " ",
                copyright()
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

  def apply()(implicit ctx: Context) =
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
