package controllers

import lila._
import views._
import tournament.{ Created }
import http.Context

import scalaz.effects._
import play.api.mvc._
import play.api.libs.json._
import play.api.libs.iteratee._
import play.api.templates.Html

object Tournament extends LilaController {

  private def repo = env.tournament.repo
  private def forms = env.tournament.forms
  private def api = env.tournament.api
  private def socket = env.tournament.socket
  private def messenger = env.tournament.messenger
  private def userRepo = env.user.userRepo

  val home = Open { implicit ctx ⇒
    IOk(repo.created map { tournaments ⇒
      html.tournament.home(tournaments)
    })
  }

  def show(id: String) = Open { implicit ctx ⇒
    IOptionIOk(repo byId id) {
      case tour: Created ⇒ for {
        roomHtml ← messenger render tour
        users ← userRepo byIds tour.data.users
      } yield html.tournament.show.created(
        tour = tour,
        roomHtml = Html(roomHtml),
        version = version(tour.id),
        users = users
      )
      case _ ⇒ throw new Exception("oups")
    }
  }

  def join(id: String) = Auth { implicit ctx ⇒
    implicit me ⇒
      IOptionIORedirect(repo createdById id) { tour ⇒
        api.join(tour, me) map { _ ⇒ routes.Tournament.show(tour.id) }
      }
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
          setup ⇒ api.createTournament(setup, me).map(tournament ⇒
            Redirect(routes.Tournament.show(tournament.id))
          ))
      }
  }

  def websocket(id: String) = WebSocket.async[JsValue] { req ⇒
    implicit val ctx = reqToCtx(req)
    socket.join(id, getInt("version"), get("uid"), ctx.me).unsafePerformIO
  }

  private def version(tournamentId: String): Int = socket blockingVersion tournamentId
}
