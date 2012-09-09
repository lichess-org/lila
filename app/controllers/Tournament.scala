package controllers

import lila._
import views._
import http.Context

import scalaz.effects._
import play.api.mvc.Result

object Tournament extends LilaController {

  val repo = env.tournament.repo
  val forms = env.tournament.forms
  val api = env.tournament.api

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

  def create = AuthBody { implicit ctx ⇒
    implicit me ⇒
      IOResult {
        implicit val req = ctx.body
        forms.create.bindFromRequest.fold(
          err ⇒ io(BadRequest(html.message.form(err))),
          data ⇒ api.makeTournament(data, me).map(tournament ⇒
            Redirect(routes.Tournament.show(tournament.id))
          ))
      }
  }
}
