package lila.puzzle
package ui

import play.api.libs.json.*

import lila.ui.*

import ScalatagsTemplate.{ *, given }

final class PuzzleGuessUi(helpers: Helpers):
  import helpers.{ *, given }

  def home(data: JsObject)(using Context) =
    Page("Puzzle or not?")
      .css("puzzleGuess")
      .js(PageModule("puzzleGuess", data))
      .flag(_.zoom):
        main(
          div(cls := "puzzle-guess puzzle-guess-app")(
            div(cls := "puzzle-guess__board main-board"),
            div(cls := "puzzle-guess__side")
          ),
          div(cls := "puzzle-guess-about")(
            p(
              "Half of these positions are real puzzles: the side to move has a winning tactic. ",
              "The other half are ordinary positions from the very same games. ",
              "First guess which one you are looking at; if it is a puzzle, prove it by solving it."
            ),
            p(
              "Each position is rated like a puzzle: the more players get it wrong, the higher its rating climbs."
            )
          )
        )
