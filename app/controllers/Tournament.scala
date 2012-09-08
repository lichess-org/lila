package controllers

import lila._
import views._
import http.Context

import play.api.mvc.Result

object Tournament extends LilaController {

  val repo = env.tournament.repo
  val forms = env.tournament.forms

  val home = Open { implicit ctx ⇒
    IOk(repo.created map { tournaments ⇒
      html.tournament.home(tournaments)
    })
  }

  def show(id: String) = Open { implicit ctx ⇒
    Ok(id)
  }

  def form = Auth { implicit ctx ⇒
    me ⇒
      Ok(html.tournament.form(forms.create))
  }

  def create = TODO
}
