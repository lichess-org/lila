package controllers

import lila.app._
import views._
import lila.game.{ Pov, GameRepo }
import lila.tournament.{ TournamentRepo, Created, Started, Finished, Tournament ⇒ Tourney }
import lila.user.{ UserRepo, Context }

import play.api.mvc._
import play.api.libs.json.JsValue

object Tournament extends LilaController {

  private def env = Env.tournament
  private def repo = TournamentRepo

  val home = Open { implicit ctx ⇒
    tournaments zip UserRepo.allSortToints(10) map {
      case (((created, started), finished), leaderboard) ⇒
        Ok(html.tournament.home(created, started, finished, leaderboard))
    }
  }

  val faq = Open { implicit ctx ⇒ Ok(html.tournament.faqPage()).fuccess }

  val homeReload = Open { implicit ctx ⇒
    tournaments map {
      case ((created, started), finished) ⇒
        Ok(html.tournament.homeInner(created, started, finished))
    }
  }

  private def tournaments =
    repo.created zip repo.started zip repo.finished(20)

  def show(id: String) = Open { implicit ctx ⇒
    repo byId id flatMap {
      _ match {
        case Some(tour: Created)  ⇒ showCreated(tour) map { Ok(_) }
        case Some(tour: Started)  ⇒ showStarted(tour) map { Ok(_) }
        case Some(tour: Finished) ⇒ showFinished(tour) map { Ok(_) }
        case _                    ⇒ NotFound(html.tournament.notFound()).fuccess
      }
    }
  }

  private def showCreated(tour: Created)(implicit ctx: Context) =
    env.version(tour.id) zip (env.messenger getMessages tour.id) map {
      case (version, messages) ⇒
        html.tournament.show.created(tour, messages, version)
    }

  private def showStarted(tour: Started)(implicit ctx: Context) =
    env.version(tour.id) zip
      (env.messenger getMessages tour.id) zip
      GameRepo.games(tour recentGameIds 4) zip
      tour.userCurrentPov(ctx.me).zmap(GameRepo.pov) map {
        case (((version, messages), games), pov) ⇒
          html.tournament.show.started(tour, messages, version, games, pov)
      }

  private def showFinished(tour: Finished)(implicit ctx: Context) =
    env.version(tour.id) zip
      (env.messenger getMessages tour.id) zip
      GameRepo.games(tour recentGameIds 4) map {
        case ((version, messages), games) ⇒
          html.tournament.show.finished(tour, messages, version, games)
      }

  def join(id: String) = AuthBody { implicit ctx ⇒
    implicit me ⇒
      NoEngine {
        OptionFuRedirect(repo createdById id) { tour ⇒
          env.api.join(tour, me).fold(
            err ⇒ {
              logwarn(err.toString)
              routes.Tournament.home()
            },
            _ ⇒ routes.Tournament.show(tour.id)
          )
        }
      }
  }

  def withdraw(id: String) = Auth { implicit ctx ⇒
    implicit me ⇒
      OptionFuRedirect(repo byId id) { tour ⇒
        env.api.withdraw(tour, me.id) inject routes.Tournament.show(tour.id)
      }
  }

  def earlyStart(id: String) = Auth { implicit ctx ⇒
    implicit me ⇒
      OptionFuRedirect(repo.createdByIdAndCreator(id, me.id)) { tour ⇒
        ~env.api.earlyStart(tour) inject routes.Tournament.show(tour.id)
      }
  }

  def reload(id: String) = Open { implicit ctx ⇒
    OptionFuOk(repo byId id) {
      case tour: Created  ⇒ reloadCreated(tour)
      case tour: Started  ⇒ reloadStarted(tour)
      case tour: Finished ⇒ reloadFinished(tour)
    }
  }

  private def reloadCreated(tour: Created)(implicit ctx: Context) = fuccess {
    html.tournament.show.inner(none)(html.tournament.show.createdInner(tour))
  }

  private def reloadStarted(tour: Started)(implicit ctx: Context) =
    GameRepo.games(tour recentGameIds 4) zip
      tour.userCurrentPov(ctx.me).zmap(GameRepo.pov) map {
        case (games, pov) ⇒ {
          val pairings = html.tournament.pairings(tour)
          val inner = html.tournament.show.startedInner(tour, games, pov)
          html.tournament.show.inner(pairings.some)(inner)
        }
      }

  private def reloadFinished(tour: Finished)(implicit ctx: Context) =
    GameRepo games (tour recentGameIds 4) map { games ⇒
      val pairings = html.tournament.pairings(tour)
      val inner = html.tournament.show.finishedInner(tour, games)
      html.tournament.show.inner(pairings.some)(inner)
    }

  def form = Auth { implicit ctx ⇒
    me ⇒
      NoEngine {
        Ok(html.tournament.form(env.forms.create, env.forms)).fuccess
      }
  }

  def create = AuthBody { implicit ctx ⇒
    implicit me ⇒
      NoEngine {
        implicit val req = ctx.body
        env.forms.create.bindFromRequest.fold(
          err ⇒ BadRequest(html.tournament.form(err, env.forms)).fuccess,
          setup ⇒ env.api.createTournament(setup, me) map { tour ⇒
            Redirect(routes.Tournament.show(tour.id))
          })
      }
  }

  def websocket(id: String) = Socket[JsValue] { implicit ctx ⇒
    ~(getInt("version") |@| get("sri") apply {
      case (version, uid) ⇒ env.socketHandler.join(id, version, uid, ctx.me)
    })
  }
}
