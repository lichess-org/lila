package lila.challenge
package ui

import lila.ui.*

import ScalatagsTemplate.{ *, given }

final class ChallengePrefUi(helpers: Helpers)(
    AccountPage: (String, String) => Context ?=> Page,
    mode: play.api.Mode
):
  import helpers.{ *, given }

  def show(challengePref: Option[ChallengePref], username: String)(using ctx: Context) =
    AccountPage("Challenge", "challenge"):
      val challengePrefAttr = ChallengePref.asEncodedUrlAttr(challengePref)
      val challengeLink     = s"${routes.Lobby.home}?user=${username}${challengePrefAttr}#friend"

      div(cls := "box box-pad")(
        div(cls := "box__top")(
          h1(RawFrag("Challenge"))
        ),
        standardFlash.map(div(cls := "box__pad")(_)),
        div(cls := "box__pad")(
          p(
            challengePref match
              case Some(_) => "You have set your preferred challenge."
              case None    => "You have NOT set your preferred challenge."
          )
        ),
        div(cls := "box__pad")(
          p(
            "You can ",
            a(href := challengeLink)("create or edit"),
            " you preferred challenge by pretending you challenge yourself (click on a king to validate). "
          ),
          p("It will be shown to the players who challenge you to a game."),
          p(
            "You can ",
            a(href := routes.Challenge.deletePreference)("remove it"),
            " when you want. "
          )
        )
      )
