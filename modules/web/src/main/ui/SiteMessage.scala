package lila.web
package ui

import lila.ui.*

import ScalatagsTemplate.{ *, given }

final class SiteMessage(helpers: Helpers):
  import helpers.{ *, given }

  def apply(title: String, back: Option[String] = None, icon: Option[Icon] = None) =
    Page(title).wrap: body =>
      main(cls := "box box-pad")(
        boxTop(
          h1(dataIcon := icon.ifTrue(back.isEmpty), cls := List("text" -> (icon.isDefined && back.isEmpty)))(
            back.map: url =>
              a(href := url, dataIcon := Icon.LessThan, cls := "text"),
            title
          )
        ),
        p(body)
      )

  def noBot = apply("No bot area"):
    frag("Sorry, bot accounts are not allowed here.")

  def noEngine = apply("No engine area"):
    "Sorry, engine assisted players are not allowed here."

  def noBooster = apply("No booster area"):
    "Sorry, boosters and sandbaggers are not allowed here."

  def noLame(using me: Me) =
    if me.marks.boost then noBooster
    if me.marks.engine then noEngine
    else // ?
      apply("Restricted area"):
        "Sorry, the access to this resource is restricted."

  def blacklistedMessage(using ctx: Context) =
    s"Sorry, your IP address ${ctx.ip} has been used to violate the ToS, and is now blacklisted."

  def blacklistedSnippet(using Context) = lila.ui.Snippet(frag(blacklistedMessage))

  def streamingMod = apply("Disabled while streaming"):
    frag(
      "This moderation feature is disabled while streaming, ",
      "to avoid leaking sensible information."
    )

  def challengeDenied(msg: String)(using Context) =
    apply(
      title = trans.challenge.challengeToPlay.txt(),
      back = routes.Lobby.home.url.some
    )(msg)

  def insightNoGames(u: User)(using Context) =
    apply(
      title = s"${u.username} has not played a rated game yet!",
      back = routes.User.show(u.id).url.some
    ):
      frag(
        "Before using chess insights,",
        userLink(u),
        " has to play at least one rated game."
      )

  def teamCreateLimit = apply("Cannot create a team"):
    "You have already created a team this week."

  def teamJoinLimit = apply("Cannot join the team"):
    "You have already joined too many teams."

  def relayPrivate = apply("This tournament is private", routes.RelayTour.index().url.some):
    "Sorry, this tournament is private, or maybe it doesn't exist."

  def authFailed = apply("403 - Access denied!"):
    "You tried to visit a page you're not authorized to access."

  def serverError(msg: String) = apply("Something went wrong")(msg)

  def temporarilyDisabled = apply("Temporarily disabled"):
    "Sorry, this feature is temporarily disabled while we figure out a way to bring it back."

  def rateLimited(msg: String = "Too many requests") = apply(msg):
    "Your device or network has sent too many requests in a short amount of time. Please try again later."

  def notYet(text: String) = apply("Not yet available")(text)
