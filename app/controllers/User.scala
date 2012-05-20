package controllers

import lila._
import views._

import play.api._
import play.api.mvc._
import play.api.mvc.Results._

object User extends LilaController {

  def userRepo = env.user.userRepo
  val paginator = env.user.paginator

  def show(username: String) = TODO

  def list(page: Int) = Open { implicit ctx ⇒
    Ok(
      html.user.list(paginator elo page)
    )
  }

  val autocomplete = Action { implicit req ⇒
    get("term", req).filter(""!=).fold(
      term ⇒ JsonOk((userRepo usernamesLike term).unsafePerformIO),
      BadRequest("No search term provided")
    )
  }

  val signUp = TODO

  val stats = TODO
}
