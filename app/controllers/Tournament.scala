package controllers

import lila._
import views._
import tournament.{ Created, Started, Finished }
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
        finisheds ← repo finished 20
      } yield html.tournament.home(createds, starteds, finisheds)
    )
  }

  val homeReload = Open { implicit ctx ⇒
    IOk(
      for {
        createds ← repo.created
        starteds ← repo.started
        finisheds ← repo finished 20
      } yield html.tournament.homeInner(createds, starteds, finisheds)
    )
  }

  def show(id: String) = Open { implicit ctx ⇒
    IOptionIOk(repo byId id) {
      case tour: Created  ⇒ showCreated(tour)
      case tour: Started  ⇒ showStarted(tour)
      case tour: Finished ⇒ showFinished(tour)
    }
  }

  private def showCreated(tour: Created)(implicit ctx: Context) = for {
    roomHtml ← messenger render tour
  } yield html.tournament.show.created(
    tour = tour,
    roomHtml = Html(roomHtml),
    version = version(tour.id))

  private def showStarted(tour: Started)(implicit ctx: Context) = for {
    roomHtml ← messenger render tour
    games ← gameRepo.recentTournamentGames(tour.id, 4)
    pov ← tour.userCurrentPov(ctx.me).fold(gameRepo.pov, io(none))
  } yield html.tournament.show.started(
    tour = tour,
    roomHtml = Html(roomHtml),
    version = version(tour.id),
    games = games,
    pov = pov)

  private def showFinished(tour: Finished)(implicit ctx: Context) = for {
    roomHtml ← messenger render tour
    games ← gameRepo.recentTournamentGames(tour.id, 4)
  } yield html.tournament.show.finished(
    tour = tour,
    roomHtml = Html(roomHtml),
    version = version(tour.id),
    games = games)

  def join(id: String) = Auth { implicit ctx ⇒
    implicit me ⇒
      IOptionIORedirect(repo createdById id) { tour ⇒
        api.join(tour, me).fold(
          err ⇒ putStrLn(err.shows) map (_ ⇒ routes.Tournament.home()),
          res ⇒ res map (_ ⇒ routes.Tournament.show(tour.id))
        )
      }
  }

  def withdraw(id: String) = Auth { implicit ctx ⇒
    implicit me ⇒
      IOptionIORedirect(repo byId id) { tour ⇒
        api.withdraw(tour, me.id) map { _ ⇒ routes.Tournament.show(tour.id) }
      }
  }

  def reload(id: String) = Open { implicit ctx ⇒
    IOptionIOk(repo byId id) {
      case tour: Created  ⇒ reloadCreated(tour)
      case tour: Started  ⇒ reloadStarted(tour)
      case tour: Finished ⇒ reloadFinished(tour)
    }
  }

  private def reloadCreated(tour: Created)(implicit ctx: Context) = io {
    val inner = html.tournament.show.createdInner(tour)
    html.tournament.show.inner(none)(inner)
  }

  private def reloadStarted(tour: Started)(implicit ctx: Context) = for {
    games ← gameRepo.recentTournamentGames(tour.id, 4)
    pov ← tour.userCurrentPov(ctx.me).fold(gameRepo.pov, io(none))
  } yield {
    val pairings = html.tournament.pairings(tour)
    val inner = html.tournament.show.startedInner(tour, games, pov)
    html.tournament.show.inner(pairings.some)(inner)
  }

  private def reloadFinished(tour: Finished)(implicit ctx: Context) =
    gameRepo.recentTournamentGames(tour.id, 4) map { games ⇒
      val pairings = html.tournament.pairings(tour)
      val inner = html.tournament.show.finishedInner(tour, games)
      html.tournament.show.inner(pairings.some)(inner)
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
          err ⇒ io(BadRequest(html.tournament.form(err))),
          setup ⇒ api.createTournament(setup, me) map { tour ⇒
            Redirect(routes.Tournament.show(tour.id))
          })
      }
  }

  def websocket(id: String) = WebSocket.async[JsValue] { req ⇒
    implicit val ctx = reqToCtx(req)
    socket.join(id, getInt("version"), get("uid"), ctx.me).unsafePerformIO
  }

  private def version(tournamentId: String): Int = socket blockingVersion tournamentId
}
