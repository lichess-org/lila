package views
package html.site

import controllers.routes
import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

object contact {

  private sealed trait Node {
    val id: String
    val name: String
  }
  private case class Branch(id: String, name: String, children: List[Node]) extends Node
  private case class Leaf(id: String, name: String, content: Frag) extends Node

  private case class FlatNode(id: String, name: String, parentId: Option[String])

  private def reopenLeaf(prefix: String) = Leaf(s"$prefix-reopen", "I want to re-open my account", frag(
    p(
      "We may agree to re-open your account, ",
      strong("but only once"),
      "."
    ),
    p(
      s"Send an email to $contactEmail ",
      strong("from the same email address that you used to create the account"),
      ".", br,
      "This is required so we know that you indeed own the account."
    ),
    p("Don't forget to mention your username.")
  ))

  private def howToReportBugs: Frag = frag(
    ul(
      li(
        "In the ",
        a(href := routes.ForumCateg.show("lichess-feedback"))("Lichess Feedback Forum")
      ),
      li(
        "As a ",
        a(href := "https://github.com/ornicar/lila/issues")("Lichess website issue"),
        " on GitHub"
      ),
      li(
        "As a ",
        a(href := "https://github.com/veloce/lichobile/issues")("Lichess mobile app issue"),
        " on GitHub"
      ),
      li(
        "In the ",
        a(href := "https://discord.gg/hy5jqSs")("Lichess discord server")
      )
    ),
    p("Please describe what the bug looks like, what you expected to happen instead, and the steps to reproduce the bug.")
  )

  private lazy val menu: Branch =
    Branch("root", "What can we help you with?", List(
      Branch("login", "I can't log in", List(
        Leaf("email-confirm", "I don't receive my confirmation email", frag(
          p(
            "You signed up, but didn't receive your confirmation email?", br,
            a(href := routes.Account.emailConfirmHelp)("Visit this page to solve the issue"), "."
          )
        )),
        Leaf("forgot-password", "I forgot my password", frag(
          p(
            "To request a new password, ",
            a(href := routes.Auth.passwordReset)(
              "visit the password reset page"
            ),
            "."
          )
        )),
        Leaf("forgot-username", "I forgot my username", frag(
          p(
            "You can ",
            a(href := routes.Auth.login)("login"),
            " with the email address you signed up with."
          )
        )),
        Leaf("lost-2fa", "I lost access to my two-factor authentication codes", frag(
          p(
            "Do a ",
            a(href := routes.Auth.passwordReset)("password reset"),
            " to remove your second factor."
          )
        )),
        reopenLeaf("login"),
        Leaf("dns", "\"This site can’t be reached\"", frag(
          p("If you can't reach Lichess, and your browser says something like:"),
          ul(
            li("This site can't be reached."),
            li(strong("lichess.org"), "’s server IP address could not be found."),
            li("We can’t connect to the server at lichess.org.")
          ),
          p("Then you have a ", strong("DNS issue"), "."),
          p(
            "There's nothing we can do about it, but ",
            a("here's how you can fix it")(href := "https://www.wikihow.com/Fix-DNS-Server-Not-Responding-Problem"),
            "."
          )
        ))
      )),
      Branch("account", "I need account support", List(
        Leaf("title", "I want my title displayed on Lichess", frag(
          p(
            "To show your title on your Lichess profile, and participate to Titled Arenas, ",
            a(href := routes.Page.master)(
              "visit the title confirmation page"
            ),
            "."
          )
        )),
        Leaf("close", "I want to close my account", frag(
          p(
            "You can close your account ",
            a(href := routes.Account.close)("on this page"),
            "."
          ),
          p("Do not ask us by email to close an account, we won't do it.")
        )),
        reopenLeaf("account"),
        Leaf("change-username", "I want to change my username", frag(
          p("We're very sorry, but the username cannot be changed. For technical reasons, it's downright impossible."),
          p("However, you can always close your current account, and create a new one.")
        )),
        Leaf("clear-history", "I want to clear my history or rating", frag(
          p("It's not possible to clear your game history, puzzle history, or ratings."),
          p("However, you can always close your current account, and create a new one.")
        ))
      )),
      Branch("report", "I want to report a player",
        List("cheating", "sandbagging", "trolling", "insults", "some other reason").map { reason =>
          Leaf(reason, s"Report a player for $reason", frag(
            p(
              s"To report a player for $reason, ",
              a(href := routes.Report.form)(strong("use the report form")), "."
            ),
            p(
              "You can also reach that page by clicking the ",
              button(cls := "thin button", dataIcon := "!"),
              " report button on a profile page."
            ),
            p(
              strong("Do not"), " report players in the forum.", br,
              strong("Do not"), " send us report emails.", br,
              "Only reporting players through ",
              a(href := routes.Report.form)("the report form"),
              " is effective."
            )
          ))
        }),
      Branch("bug", "I want to report a bug", List(
        Leaf("enpassant", "Illegal pawn capture", frag(
          p("It is called \"en passant\" and is one of the rules of chess."),
          p(
            "Try ",
            a(href := "/learn#/15")("this interactive game"),
            " to learn more about this chess rule."
          )
        )),
        Leaf("castling", "Illegal or impossible castling", frag(
          p("The castle is only prevented if the king goes through a controlled square."),
          p(
            "Make sure you understand the ",
            a(href := "https://en.wikipedia.org/wiki/Castling#Requirements")("castling requirements"),
            "."
          ),
          p(
            "Try ",
            a(href := "/learn#/15")("this interactive game"),
            " to practice castling in chess."
          ),
          p("If you imported the game, or started it from a position, make sure you correctly set the castling rights.")
        )),
        Leaf("insufficient", "Insufficient mating material", frag(
          p(
            "According to the ",
            a(href := "https://www.fide.com/fide/handbook.html?id=171&view=article")(
              "FIDE Laws of Chess"
            ),
            ", if a checkmate is possible with any legal sequence of moves, then the game is not a draw."
          ),
          p(
            "It is possible to checkmate with only a knight or a bishop, if the opponent has more than a king on the board."
          )
        )),
        Leaf("casual", "No rating points were awarded", frag(
          p("Make sure you played a rated game."),
          p("Casual games do not affect the players ratings.")
        )),
        Leaf("error-page", "Error page", frag(
          p("If you faced an error page, you may report it:"),
          howToReportBugs
        )),
        Leaf("other-bug", "Other bug", frag(
          p("If you found a new bug, you may report it:"),
          howToReportBugs
        ))
      )),
      Branch("appeal", "Appeal for a ban or IP restriction", List(
        Leaf("appeal-cheat", "Engine or cheat mark", frag(
          p(s"If you have been marked as an engine, you may send an appeal to $contactEmail."),
          p(
            "False positives do happen sometimes, and we're sorry about that.", br,
            "If your appeal is legit, we will lift the ban ASAP."
          ),
          p(
            "However if you indeed used engine assistance, ",
            strong("even just once"),
            ", then your account is unfortunately lost.", br,
            "Do not deny having cheated. If you want to be allowed to create a new account, ",
            "just admit to what you did, and show that you understood that it was a mistake."
          )
        )),
        Leaf("appeal-other", "None of the above", frag(
          p(s"You may send an appeal to $contactEmail."),
          p(
            "False positives do happen sometimes, and we're sorry about that.", br,
            "If your appeal is legit, we will lift the ban or restriction ASAP."
          )
        ))
      )),
      Branch("collab", "Collaboration, legal, commercial", List(
        Leaf("monetize", "Monetizing Lichess", frag(
          p("We are not interested in any way of monetizing Lichess."),
          p("We will never display any kind of ads, we won't track our players, and we won't sell or buy traffic or users."),
          p("Please do not email us about marketing, tracking, or advertising.")
        )),
        Leaf("buy", "Buying Lichess", frag(
          p("We are not selling, to anyone, for any price. Ever.")
        )),
        Leaf("authorize", "Authorization to use Lichess", frag(
          p("You are welcome to use Lichess for your activity, even commercial."),
          p("You can show it in your videos, and you can print screenshots of Lichess in your books."),
          p("Credit is appreciated but not required.")
        )),
        Leaf("gdpr", "GDPR", frag(
          p("If you are a European citizen, you may request the deletion of your Lichess account."),
          p(
            "First, ",
            a(href := routes.Account.close)("close your account"),
            "."
          ),
          p(s"Then send us an email at $contactEmail to request the definitive erasure of all data linked to the account."),
          p("Note that games are facts, not personal information. And as such they are never deleted.")
        )),
        Leaf("contact-other", "None of the above", frag(
          p(s"Please send us an email at $contactEmail."),
          p(
            "Please explain your request clearly and thoroughly. ",
            "State your Lichess username, and any information that could help us help you."
          )
        ))
      ))
    ))

  private def renderNode(node: Node, parent: Option[Node]): Frag = node match {
    case Leaf(id, name, content) => List(
      div(makeId(node.id), cls := "node leaf")(
        h2(parent map goBack, node.name),
        div(cls := "content")(content)
      )
    )
    case b @ Branch(id, name, children) => frag(
      div(makeId(node.id), cls := s"node branch $id")(
        h2(parent map goBack, node.name),
        div(cls := "links")(
          children map { child =>
            a(makeLink(child.id))(child.name)
          }
        )
      ),
      children map { renderNode(_, b.some) }
    )
  }

  private lazy val renderedMenu = renderNode(menu, none)

  private def makeId(id: String) = st.id := s"help-$id"
  private def makeLink(id: String) = href := s"#help-$id"

  private def goBack(parent: Node): Frag =
    a(makeLink(parent.id), cls := "back", dataIcon := "I", title := "Go back")

  def apply()(implicit ctx: Context) = help.layout(
    title = "Contact",
    active = "contact",
    moreCss = cssTag("contact"),
    moreJs = embedJsUnsafe("""location=location.hash||"#help-root""""),
    contentCls = "page box box-pad"
  )(frag(
      h1("Contact Lichess"),
      div(cls := "contact")(
        renderedMenu
      )
    ))
}
