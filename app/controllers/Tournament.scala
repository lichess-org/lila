package controllers

import lila._
import views._
import tournament.{ Created, Started }
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
  private def gameRepo = env.game.gameRepo

  val home = Open { implicit ctx ⇒
    IOk(
      for {
        createds ← repo.created
        starteds ← repo.started
      } yield html.tournament.home(createds, starteds)
    )
  }

  def show(id: String) = Open { implicit ctx ⇒
    IOptionIOk(repo byId id) {
      case tour: Created ⇒ showCreated(tour)
      case tour: Started ⇒ showStarted(tour)
      case _             ⇒ throw new Exception("uuuh")
    }
  }

  private def showCreated(tour: Created)(implicit ctx: Context) = for {
    roomHtml ← messenger render tour
    users ← userRepo byIds tour.data.users
  } yield html.tournament.show.created(
    tour = tour,
    roomHtml = Html(roomHtml),
    version = version(tour.id),
    users = users)

  private def showStarted(tour: Started)(implicit ctx: Context) = for {
    roomHtml ← messenger render tour
    users ← userRepo byIds tour.data.users
    games ← gameRepo.recentTournamentGames(tour.id, 4)
  } yield html.tournament.show.started(
    tour = tour,
    roomHtml = Html(roomHtml),
    version = version(tour.id),
    users = users,
    games = games)

  def join(id: String) = Auth { implicit ctx ⇒
    implicit me ⇒
      IOptionIORedirect(repo createdById id) { tour ⇒
        api.join(tour, me) map { result ⇒
          result.fold(
            err ⇒ { println(err.shows); routes.Tournament.home() },
            _ ⇒ routes.Tournament.show(tour.id)
          )
        }
      }
  }

  def withdraw(id: String) = Auth { implicit ctx ⇒
    implicit me ⇒
      IOptionIORedirect(repo createdById id) { tour ⇒
        api.withdraw(tour, me) map { _ ⇒ routes.Tournament.show(tour.id) }
      }
  }

  def reload(id: String) = Open { implicit ctx ⇒
    IOptionIOk(repo startedById id) { tour ⇒
      gameRepo.recentTournamentGames(tour.id, 4) map { games ⇒
        val pairings = html.tournament.pairings(tour)
        val inner = html.tournament.show.startedInner(tour, games)
        html.tournament.show.inner(pairings.some)(inner)
      }
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
            tournament.fold(
              err ⇒ { println(err.shows); Redirect(routes.Tournament.home()) },
              tour ⇒ Redirect(routes.Tournament.show(tour.id))
            )
          ))
      }
  }

  def websocket(id: String) = WebSocket.async[JsValue] { req ⇒
    implicit val ctx = reqToCtx(req)
    socket.join(id, getInt("version"), get("uid"), ctx.me).unsafePerformIO
  }

  private def version(tournamentId: String): Int = socket blockingVersion tournamentId
}
