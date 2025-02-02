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
      val challengePrefEdit = s"${routes.Lobby.home}?edit=true${challengePrefAttr}#friend"
      val challengeLink     = s"${routes.Lobby.home}?user=${username.toString}${challengePrefAttr}#friend"

      div(cls := "box box-pad")(
        div(cls := "box__top")(
          h1(RawFrag("Challenge"))
        ),
        standardFlash.map(div(cls := "box__pad")(_)),
        div(cls := "box__pad")(
          p(
            "Setting your preferred challenge will set the 'Challenge to a game' dialog when friends challenge you."
          ),
          p(
            challengePref match
              case Some(_) => "You have set your preferred challenge."
              case None    => "You have NOT set your preferred challenge."
          ),
          p(
            challengePref match
              case Some(_) =>
                p(
                  "You can ",
                  a(href := challengePrefEdit)("edit it"),
                  " or ",
                  a(href := routes.Challenge.deletePreference)("remove it"),
                  ". You can also share this ",
                  a(href := challengeLink)("link"),
                  " to challenge you."
                )
              case None =>
                p(
                  "You can ",
                  a(href := challengePrefEdit)("create it")
                )
          )
        )
      )
