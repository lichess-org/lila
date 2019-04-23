package views
package html.site

import controllers.routes
import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.user.User

object message {

  def apply(
    title: String,
    back: Boolean = true,
    icon: Option[String] = None,
    moreCss: Option[Frag] = None
  )(message: Modifier*)(implicit ctx: Context) =
    views.html.base.layout(title = title, moreCss = ~moreCss) {
      main(cls := "box box-pad")(
        h1(cls := List("text" -> icon.isDefined), dataIcon := icon)(title),
        p(message),
        br,
        back option embedJsUnsafe {
          """if (document.referrer) document.write('<a class="button text" data-icon="I" href="' + document.referrer + '">Go Back</a>');"""
        }
      )
    }

  def noBot(implicit ctx: Context) = apply("No bot area") {
    frag("Sorry, bot accounts are not allowed here.")
  }

  def noEngine(implicit ctx: Context) = apply("No engine area") {
    "Sorry, engine assisted players are not allowed here."
  }

  def noBooster(implicit ctx: Context) = apply("No booster area") {
    "Sorry, boosters and sandbaggers are not allowed here."
  }

  def privateStudy(ownerId: User.ID)(implicit ctx: Context) = apply(
    title = s"${usernameOrId(ownerId)}'s study",
    icon = "4".some
  )("Sorry! This study is private, you cannot access it.")

  def streamingMod(implicit ctx: Context) = apply("Disabled while streaming") {
    frag(
      "This moderation feature is disabled while streaming,", br, "to avoid leaking sensible information."
    )
  }

  def challengeDenied(msg: String)(implicit ctx: Context) = apply(
    title = trans.challengeToPlay.txt(),
    icon = "j".some
  )(msg)

  def insightNoGames(u: User)(implicit ctx: Context) = apply(
    title = s"${u.username} has not played a rated game yet!",
    icon = "7".some
  )(frag(
    "Before using chess insights,",
    userLink(u),
    " has to play at least one rated game."
  ))

  def teamCreateLimit(implicit ctx: Context) = apply("Cannot create a team") {
    "You have already created a team this week."
  }

  def authFailed(implicit ctx: Context) = apply("403 - Access denied!") {
    "You tried to visit a page you're not authorized to access."
  }
}
