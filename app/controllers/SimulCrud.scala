package controllers

import play.api.libs.json._

import lidraughts.app._
import lidraughts.simul.crud.CrudForm.{ empty => emptyForm }
import lidraughts.user.UserRepo
import views._

object SimulCrud extends LidraughtsController {

  private def env = Env.simul
  private def crud = env.crudApi

  def index(page: Int) = Secure(_.ManageSimul) { implicit ctx => me =>
    crud.paginator(page) map { paginator =>
      html.simul.crud.index(paginator)
    }
  }

  def edit(id: String) = Secure(_.ManageSimul) { implicit ctx => me =>
    OptionFuResult(crud one id) { simul =>
      crud.editForm(simul) map { form => Ok(html.simul.crud.edit(simul, form)) }
    }
  }

  def update(id: String) = SecureBody(_.ManageSimul) { implicit ctx => me =>
    OptionFuResult(crud one id) { simul =>
      UserRepo.byId(simul.hostId) flatMap { host =>
        UserRepo.byId(simul.arbiterId) flatMap { arbiter =>
          implicit val req = ctx.body
          crud.editForm(simul, host, arbiter).bindFromRequest.fold(
            err => BadRequest(html.simul.crud.edit(
              simul,
              env.forms.applyVariants.bindFromRequest.fold(
                err2 => err,
                data => err.copy(value = emptyForm.copy(variants = data.variants).some)
              )
            )).fuccess,
            data => UserRepo.named(data.hostName) flatMap {
              case Some(newHost) =>
                UserRepo.named(data.arbiterName) flatMap { newArbiter =>
                  crud.update(simul, data, newHost, newArbiter) inject Redirect(routes.SimulCrud.edit(id))
                }
              case _ => BadRequest(s"New host ${data.hostName} not found").fuccess
            }
          )
        }
      }
    }
  }

  def allowed(id: String) = Secure(_.ManageSimul) { implicit ctx => me =>
    OptionFuResult(crud one id) { simul =>
      (Ok(Json.obj("ok" -> ~simul.allowed)) as JSON).fuccess
    }
  }

  def allow(simulId: String, username: String) = allowUser(simulId, username, true)
  def disallow(simulId: String, username: String) = allowUser(simulId, username, false)

  private def allowUser(simulId: String, username: String, allow: Boolean) = Secure(_.ManageSimul) { implicit ctx => me =>
    env.api.allow(simulId, UserRepo normalize username, allow)
    Ok("ok").fuccess
  }

  def form = Secure(_.ManageSimul) { implicit ctx => me =>
    Ok(html.simul.crud.create(crud.createForm)).fuccess
  }

  def create = SecureBody(_.ManageSimul) { implicit ctx => me =>
    implicit val req = ctx.body
    crud.createForm.bindFromRequest.fold(
      err => BadRequest(html.simul.crud.create(
        env.forms.applyVariants.bindFromRequest.fold(
          err2 => err,
          data => err.copy(value = emptyForm.copy(variants = data.variants).some)
        )
      )).fuccess,
      data => UserRepo.named(data.hostName) flatMap {
        case Some(host) =>
          UserRepo.named(data.arbiterName) flatMap { arbiter =>
            crud.create(data, host, arbiter) map { simul =>
              Redirect(routes.SimulCrud.edit(simul.id))
            }
          }
        case _ => BadRequest(s"Host ${data.hostName} not found").fuccess
      }
    )
  }
}
