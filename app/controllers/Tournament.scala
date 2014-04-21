package controllers

import play.api.data.Form
import play.api.libs.json.JsValue
import play.api.mvc._

import lila.api.Context
import lila.app._
import lila.game.{ Pov, GameRepo }
import lila.tournament.{ TournamentRepo, Created, Started, Finished, Tournament => Tourney }
import lila.user.UserRepo
import views._

object Tournament extends LilaController {

  private def env = Env.tournament
  private def repo = TournamentRepo

  private def tournamentNotFound(implicit ctx: Context) = NotFound(html.tournament.notFound())

  protected def TourOptionFuRedirect[A](fua: Fu[Option[A]])(op: A => Fu[Call])(implicit ctx: Context) =
    fua flatMap {
      _.fold(tournamentNotFound(ctx).fuccess)(a => op(a) map { b => Redirect(b) })
    }

  val home = Open { implicit ctx =>
    fetchTournaments zip repo.scheduled zip UserRepo.allSortToints(10) map {
      case ((((created, started), finished), scheduled), leaderboard) =>
        Ok(html.tournament.home(created, started, finished, scheduled, leaderboard))
    }
  }

  val faq = Open { implicit ctx => Ok(html.tournament.faqPage()).fuccess }

  val homeReload = Open { implicit ctx =>
    fetchTournaments map {
      case ((created, started), finished) =>
        Ok(html.tournament.homeInner(created, started, finished))
    }
  }

  private def fetchTournaments =
    env allCreatedSorted true zip repo.started zip repo.finished(30)

  def show(id: String) = Open { implicit ctx =>
    repo byId id flatMap {
      _ match {
        case Some(tour: Created)  => showCreated(tour) map { Ok(_) }
        case Some(tour: Started)  => showStarted(tour) map { Ok(_) }
        case Some(tour: Finished) => showFinished(tour) map { Ok(_) }
        case _                    => tournamentNotFound.fuccess
      }
    }
  }

  private def showCreated(tour: Created)(implicit ctx: Context) =
    env.version(tour.id) zip chatOf(tour) map {
      case (version, chat) => html.tournament.show.created(tour, version, chat)
    }

  private def showStarted(tour: Started)(implicit ctx: Context) =
    env.version(tour.id) zip
      chatOf(tour) zip
      GameRepo.games(tour recentGameIds 4) zip
      tour.userCurrentPov(ctx.me).??(GameRepo.pov) map {
        case (((version, chat), games), pov) =>
          html.tournament.show.started(tour, version, chat, games, pov)
      }

  private def showFinished(tour: Finished)(implicit ctx: Context) =
    env.version(tour.id) zip
      chatOf(tour) zip
      GameRepo.games(tour recentGameIds 4) map {
        case ((version, chat), games) =>
          html.tournament.show.finished(tour, version, chat, games)
      }

  def join(id: String) = AuthBody { implicit ctx =>
    implicit me =>
      NoEngine {
        TourOptionFuRedirect(repo enterableById id) { tour =>
          tour.hasPassword.fold(
            fuccess(routes.Tournament.joinPassword(id)),
            env.api.join(tour, me, none).fold(
              _ => routes.Tournament.show(tour.id),
              _ => routes.Tournament.show(tour.id)
            )
          )
        }
      }
  }

  def joinPasswordForm(id: String) = Auth { implicit ctx =>
    implicit me => NoEngine {
      repo createdById id flatMap {
        _.fold(tournamentNotFound(ctx).fuccess) { tour =>
          renderJoinPassword(tour, env.forms.joinPassword) map { Ok(_) }
        }
      }
    }
  }

  def joinPassword(id: String) = AuthBody { implicit ctx =>
    implicit me =>
      NoEngine {
        implicit val req = ctx.body
        repo createdById id flatMap {
          _.fold(tournamentNotFound(ctx).fuccess) { tour =>
            env.forms.joinPassword.bindFromRequest.fold(
              err => renderJoinPassword(tour, err) map { BadRequest(_) },
              password => env.api.join(tour, me, password.some) flatFold (
                _ => renderJoinPassword(tour, env.forms.joinPassword) map { BadRequest(_) },
                _ => fuccess(Redirect(routes.Tournament.show(tour.id)))
              )
            )
          }
        }
      }
  }

  private def renderJoinPassword(tour: Created, form: Form[_])(implicit ctx: Context) =
    env version tour.id map { html.tournament.joinPassword(tour, form, _) }

  def withdraw(id: String) = Auth { implicit ctx =>
    me =>
      TourOptionFuRedirect(repo byId id) { tour =>
        env.api.withdraw(tour, me.id) inject routes.Tournament.show(tour.id)
      }
  }

  def earlyStart(id: String) = Auth { implicit ctx =>
    implicit me =>
      TourOptionFuRedirect(repo.createdByIdAndCreator(id, me.id)) { tour =>
        ~env.api.earlyStart(tour) inject routes.Tournament.show(tour.id)
      }
  }

  def reload(id: String) = Open { implicit ctx =>
    OptionFuOk(repo byId id) {
      case tour: Created  => reloadCreated(tour)
      case tour: Started  => reloadStarted(tour)
      case tour: Finished => reloadFinished(tour)
    }
  }

  private def reloadCreated(tour: Created)(implicit ctx: Context) = fuccess {
    html.tournament.show.inner(none)(html.tournament.show.createdInner(tour))
  }

  private def reloadStarted(tour: Started)(implicit ctx: Context) =
    GameRepo.games(tour recentGameIds 4) zip
      tour.userCurrentPov(ctx.me).??(GameRepo.pov) map {
        case (games, pov) => {
          val pairings = html.tournament.pairings(tour)
          val inner = html.tournament.show.startedInner(tour, games, pov)
          html.tournament.show.inner(pairings.some)(inner)
        }
      }

  private def reloadFinished(tour: Finished)(implicit ctx: Context) =
    GameRepo games (tour recentGameIds 4) map { games =>
      val pairings = html.tournament.pairings(tour)
      val inner = html.tournament.show.finishedInner(tour, games)
      html.tournament.show.inner(pairings.some)(inner)
    }

  def form = Auth { implicit ctx =>
    me =>
      NoEngine {
        Ok(html.tournament.form(env.forms.create, env.forms)).fuccess
      }
  }

  def create = AuthBody { implicit ctx =>
    implicit me =>
      NoEngine {
        implicit val req = ctx.body
        env.forms.create.bindFromRequest.fold(
          err => BadRequest(html.tournament.form(err, env.forms)).fuccess,
          setup => env.api.createTournament(setup, me) map { tour =>
            Redirect(routes.Tournament.show(tour.id))
          })
      }
  }

  def websocket(id: String) = Socket[JsValue] { implicit ctx =>
    ~(getInt("version") |@| get("sri") apply {
      case (version, uid) => env.socketHandler.join(id, version, uid, ctx.me)
    })
  }

  private def chatOf(tour: lila.tournament.Tournament)(implicit ctx: Context) =
    ctx.isAuth ?? {
      Env.chat.api.userChat find tour.id map (_.forUser(ctx.me).some)
    }
}
