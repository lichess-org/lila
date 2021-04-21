package views.html
package appeal

import controllers.routes

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.user.User

object tree {

  import trans.contact.doNotMessageModerators
  import views.html.base.navTree._

  private def cleanMenu(implicit ctx: Context): Branch =
    Branch(
      "root",
      "Your account is not marked or restricted. You're all good!",
      List(
        Leaf(
          "clean-other-account",
          "I want to appeal for another account",
          frag(
            p(
              "Sorry we don't take appeals from other accounts. The appeal should come from nowhere else, but the concerned account."
            )
          )
        ),
        Leaf(
          "clean-warning",
          "I want to discuss a warning I received",
          frag(
            p(
              "Please note that warnings are only warnings, and that your account has not been restricted currently.",
              br,
              "If you still want to file an appeal, use the following form:"
            ),
            newAppeal()
          )
        ),
        Leaf(
          "clean-other-issue",
          "I have another issue to discuss",
          p(
            "This channel of communication is for appealing moderation related issues.",
            br,
            "Please use ",
            a(href := routes.Main.contact)("the contact page"),
            " or ",
            a(href := "https://discordapp.com/invite/pvHanhg")("our discord server"),
            " to contact us about other issues.",
            br,
            "You can also ",
            a(href := routes.Page.loneBookmark("appeal"))("find here more information about appeals.")
          )
        )
      )
    )

  private def engineMenu(implicit ctx: Context): Branch = {
    val accept =
      "I accept that I used outside assistance in my games."
    val deny =
      "I deny having used outside assistance in my games."
    Branch(
      "root",
      "Your account is marked for illegal assistance in games.",
      List(
        Leaf(
          "engine-accept",
          accept,
          frag(
            sendUsAnAppeal,
            newAppeal(s"$accept I am sorry and I would like another chance.")
          )
        ),
        Leaf(
          "engine-deny",
          deny,
          frag(
            p("You may send us an appeal, and a moderator will review it as soon as possible."),
            p(strong("What should I write in my appeal?")),
            p("Be honest and tell us the truth."),
            p(
              "Include everything that you think matters for your case. Only send your appeal once, and don't send any additional messages if they don't add anything important to your appeal. Sending additional messages will not get your appeal dealt with any sooner."
            ),
            p(
              "It is important to be honest from the start. If at first you deny doing anything wrong, we'll treat your appeal accordingly, and we will simply disregard any changes in your position. In other words, don't try to deny things at first only to confess to something later on."
            ),
            p(
              "Note that if your appeal is denied, you are not permitted to open additional accounts on Lichess."
            ),
            newAppeal(deny)
          )
        )
      ),
      content = frag(
        "We define this as using any external assistance to strengthen your knowledge and, or, calculation ability to gain an unfair advantage over your opponent. Some examples would include computer engine assistance, opening books (except for correspondence games), endgame tablebases, and asking another player for help, although these aren’t the only things we would consider cheating."
      ).some
    )
  }

  private def boostMenu(implicit ctx: Context): Branch = {
    val accept = "I accept that I manipulated my rating."
    val acceptFull =
      "I accept that I deliberately manipulated my rating by losing games on purpose, or by playing another account that was deliberately losing games. I am sorry and I would like another chance."
    val deny =
      "I deny having manipulated my rating."
    val denyFull =
      "I deny having manipulated my rating. I have never lost rated games on purpose, or played several games with someone who does."
    Branch(
      "root",
      "Your account is marked for rating manipulation.",
      List(
        Leaf(
          "boost-accept",
          accept,
          frag(
            sendUsAnAppeal,
            newAppeal(acceptFull)
          )
        ),
        Leaf(
          "boost-deny",
          deny,
          frag(
            sendUsAnAppeal,
            newAppeal(denyFull)
          )
        )
      ),
      content = p(
        "We define this as deliberately manipulating rating by losing games on purpose, or by playing against another account that is deliberately losing games."
      ).some
    )
  }

  private def muteMenu(implicit ctx: Context): Branch = {
    val accept = "I accept that I have not followed the communication guidelines"
    val acceptFull =
      "I accept that I have not followed the communication guidelines. I will behave better in future, please give me another chance."
    val deny =
      "I have followed the communication guidelines"
    Branch(
      "root",
      "Your account is muted.",
      List(
        Leaf(
          "mute-accept",
          accept,
          frag(
            p(
              "I accept that I have not followed the ",
              a(href := routes.Page.loneBookmark("communication-guidelines"))("communication guidelines"),
              ". I will behave better in future, please give me another chance."
            ),
            sendUsAnAppeal,
            newAppeal(acceptFull)
          )
        ),
        Leaf(
          "mute-deny",
          deny,
          frag(
            sendUsAnAppeal,
            newAppeal(deny)
          )
        )
      ),
      content = p(
        "Read our ",
        a(href := routes.Page.loneBookmark("communication-guidelines"))("communication guidelines"),
        ". Failure to follow the communication guidelines can result in accounts being muted."
      ).some
    )
  }

  private def playbanMenu(implicit ctx: Context): Branch = {
    Branch(
      "root",
      "You have a play timeout.",
      List(
        Leaf(
          "playban-abort",
          "For having aborted too many games.",
          frag(
            p(
              "We understand your frustration, but temporary play bans for aborting too many games are necessary. It's very annoying for your opponent when the game gets aborted and we have to discourage it."
            ),
            p("A few things we can suggest are:"),
            ul(
              li("Don't send a challenge if you don't want to play and then abort the game."),
              li(
                "If you don't want to face lower or higher rated opponents, set a rating range on your seek."
              ),
              li(
                "Don't abort games if you want to have a particular color, you have to play with both colors."
              )
            )
          )
        ),
        Leaf(
          "playban-timeout",
          "For letting my game clock time run out.",
          p(
            "We understand your frustration, but temporary play bans for stalling in games are necessary, it can be very frustrating for opponents to waste time in lost positions before resigning."
          )
        ),
        Leaf(
          "playban-disconnect",
          "For frequently disconnecting from games.",
          frag(
            p(
              "We understand your frustration, but temporary play bans for losing connection are necessary, even if you don't disconnect on purpose. It's very annoying to suddenly lose your opponent during a game and we have to discourage it."
            ),
            p(
              "The only thing we can suggest to you is that you try to get a better connection or play longer time-control games that are more forgiving of disconnections."
            )
          )
        )
      )
    )
  }

  private def altScreen(implicit ctx: Context) = div(cls := "leaf")(
    h2("Your account was closed by moderators."),
    div(cls := "content")(
      p("Did you create multiple accounts? If so, remember that you promised not to, on the sign up page."),
      p(
        "If you violated the terms of service on a previous account, then you are not allowed to make a new one, ",
        "unless it was explicitely allowed by the moderation team during an appeal."
      ),
      p(
        "If you never violated the terms of service, and didn't make several accounts, then you can appeal this account closure:"
      )
    ),
    newAppeal()
  )

  def apply(me: User, playban: Boolean)(implicit ctx: Context) =
    bits.layout("Appeal a moderation decision") {
      val query = isGranted(_.Appeals) ?? ctx.req.queryString.toMap
      main(cls := "page page-small box box-pad appeal")(
        h1("Appeal"),
        div(cls := "nav-tree")(
          if (me.disabled || query.contains("alt")) altScreen
          else
            renderNode(
              {
                if (playban || query.contains("playban")) playbanMenu
                else if (me.marks.engine || query.contains("engine")) engineMenu
                else if (me.marks.boost || query.contains("boost")) boostMenu
                else if (me.marks.troll || query.contains("shadowban")) muteMenu
                else cleanMenu
              },
              none
            )
        ),
        div(cls := "appeal__rules")(
          p(cls := "text", dataIcon := "")(doNotMessageModerators()),
          p(
            a(cls := "text", dataIcon := "", href := routes.Page.loneBookmark("appeal"))(
              "Read more about the appeal process"
            )
          ),
          p(a(cls := "text", dataIcon := "x", href := routes.Account.data)("Export personal data"))
        )
      )
    }

  private val sendUsAnAppeal = frag(
    p("Send us an appeal, and a moderator will review it as soon as possible."),
    p("Add any relevant information that could help us process your appeal."),
    p("Please be honest, concise, and on point.")
  )

  private def newAppeal(preset: String = "")(implicit ctx: Context) =
    discussion.renderForm(
      lila.appeal.Appeal.form.fill(preset),
      action = routes.Appeal.post.url,
      isNew = true,
      presets = none
    )

  private def renderHelp =
    div(cls := "appeal__help")(
      p(
        "If your account has been restricted for violation of ",
        a(href := routes.Page.tos)("the Lichess rules"),
        " you may file an appeal here."
      ),
      p(
        "You can read more about the appeal process ",
        a(href := routes.Page.loneBookmark("appeal"))("here.")
      )
    )
}
