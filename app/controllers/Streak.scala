package controllers

import views._

import lila.api.Context
import lila.app._

final class Streak(
    env: Env,
    puzzleC: => Puzzle
) extends LilaController(env) {

  def home =
    Open { implicit ctx =>
      NoBot {
        ???
      }
    }
}
