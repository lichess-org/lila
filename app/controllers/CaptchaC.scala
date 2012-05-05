package lila
package controllers

import play.api.mvc._

object CaptchaC extends LilaController {

  private val captcha = env.captcha

  def create = Action {
    env.captcha.create.unsafePerformIO.fold(
      err ⇒ BadRequest(err.shows),
      data ⇒ JsonOk(Map(
        "id" -> data._1,
        "fen" -> data._2,
        "color" -> data._3.toString
      ))
    )
  }

  def solve(gameId: String) = Action {
    env.captcha.solve(gameId).unsafePerformIO.pp.fold(
      err ⇒ BadRequest(err.shows),
      moves ⇒ JsonOk(moves.list)
    )
  }
}
