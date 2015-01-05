package controllers

import play.api.data.Form
import play.api.libs.json._
import play.api.mvc._

import lila.api.Context
import lila.app._
import lila.game.{ Pov, GameRepo }
import lila.tournament.{ System, TournamentRepo, Created, Started, Finished, Tournament => Tourney }
import lila.user.UserRepo
import views._

object Tournament extends LilaController {

  private def env = Env.tournament
  private def repo = TournamentRepo

  private def tournamentNotFound(implicit ctx: Context) = NotFound(html.tournament.notFound())

  val home = Open { implicit ctx =>
    fetchTournaments zip repo.scheduled zip UserRepo.allSortToints(10) map {
      case ((((created, started), finished), scheduled), leaderboard) =>
        Ok(html.tournament.home(created, started, finished, scheduled, leaderboard))
    }
  }

  def help(sysStr: Option[String]) = Open { implicit ctx =>
    val system = sysStr flatMap {
      case "arena" => System.Arena.some
      case "swiss" => System.Swiss.some
      case _       => none
    }
    Ok(html.tournament.faqPage(system)).fuccess
  }

  val homeReload = Open { implicit ctx =>
    fetchTournaments map {
      case ((created, started), finished) =>
        Ok(html.tournament.homeInner(created, started, finished))
    }
  }

  private def fetchTournaments =
    env allCreatedSorted true zip repo.publicStarted zip repo.finished(20)

  def show(id: String) = Open { implicit ctx =>
    repo byId id flatMap {
      _.fold(tournamentNotFound.fuccess) { tour =>
        env.version(tour.id) zip
          env.jsonView(tour) zip
          chatOf(tour) map {
            case ((version, data), chat) => html.tournament.show(tour, version, data, chat)
          }
      }
    }
  }

  def join(id: String) = AuthBody { implicit ctx =>
    implicit me =>
      NoEngine {
        negotiate(
          html = repo enterableById id map {
            case None => tournamentNotFound
            case Some(tour) =>
              env.api.join(tour, me)
              Redirect(routes.Tournament.show(tour.id))
          },
          api = _ => OptionFuOk(repo enterableById id) { tour =>
            env.api.join(tour, me)
            fuccess(Json.obj("ok" -> true))
          }
        )
      }
  }

  def withdraw(id: String) = Auth { implicit ctx =>
    me =>
      OptionResult(repo byId id) { tour =>
        env.api.withdraw(tour, me.id)
        Ok(Json.obj("ok" -> true)) as JSON
      }
  }

  def earlyStart(id: String) = Auth { implicit ctx =>
    implicit me =>
      OptionResult(repo.createdByIdAndCreator(id, me.id)) { tour =>
        env.api startIfReady tour
        Ok(Json.obj("ok" -> true)) as JSON
      }
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

  def websocket(id: String, apiVersion: Int) = Socket[JsValue] { implicit ctx =>
    ~(getInt("version") |@| get("sri") apply {
      case (version, uid) => env.socketHandler.join(id, version, uid, ctx.me)
    })
  }

  private def chatOf(tour: lila.tournament.Tournament)(implicit ctx: Context) =
    ctx.isAuth ?? {
      Env.chat.api.userChat find tour.id map (_.forUser(ctx.me).some)
    }
}
