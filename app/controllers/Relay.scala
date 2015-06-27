package controllers

import play.api.data.Form
import play.api.libs.json._
import play.api.mvc._, Results._

import lila.api.Context
import lila.app._
import lila.relay.{ Relay => RelayModel }
import views._

object Relay extends LilaController {

  private def env = Env.relay

  private def relayNotFound(implicit ctx: Context) = NotFound(html.relay.notFound())

  val index = Open { implicit ctx =>
    env.repo recentNonEmpty 50 flatMap { relays =>
      env.contentApi byRelays relays map { contents =>
        Ok(html.relay.home(relays, contents))
      }
    }
  }

  def show(id: String, slug: String) = Open { implicit ctx =>
    env.repo byId id flatMap {
      _.fold(relayNotFound.fuccess) { relay =>
        if (relay.slug != slug) Redirect(routes.Relay.show(id, relay.slug)).fuccess
        else env.contentApi.byRelay(relay) flatMap { content =>
          env.version(relay.id) zip
            env.jsonView(relay, content) zip
            chatOf(relay) map {
              case ((version, data), chat) => html.relay.show(relay, content, version, data, chat)
            }
        }
      } map NoCache
    }
  }

  def atom = Action.async { implicit req =>
    env.repo recent 100 flatMap { relays =>
      env.contentApi byRelays relays map { contents =>
        Ok(xml.relay.atom(relays, contents)) as XML
      }
    }
  }

  def contentForm(id: String, slug: String) = Secure(_.RelayContent) { implicit ctx =>
    me =>
      OptionFuOk(env.repo byId id) { relay =>
        env.contentApi byRelay relay map { content =>
          html.relay.contentForm(relay, content, lila.relay.ContentApi form content)
        }
      }
  }

  def contentPost(id: String, slug: String) = SecureBody(_.RelayContent) { implicit ctx =>
    me =>
      OptionFuResult(env.repo byId id) { relay =>
        env.contentApi byRelay relay flatMap { content =>
          implicit val req = ctx.body
          lila.relay.ContentApi.form.bindFromRequest.fold(
            err => BadRequest(html.relay.contentForm(relay, content, err)).fuccess,
            data => env.contentApi.upsert(relay, data, me) inject
              Redirect(routes.Relay.show(relay.id, relay.slug))
          )
        }
      }
  }

  def websocket(id: String, apiVersion: Int) = SocketOption[JsValue] { implicit ctx =>
    (getInt("version") |@| get("sri")).tupled ?? {
      case (version, uid) => env.socketHandler.join(id, version, uid, ctx.me)
    }
  }

  private def chatOf(relay: RelayModel)(implicit ctx: Context) =
    ctx.isAuth ?? {
      Env.chat.api.userChat find relay.id map (_.forUser(ctx.me).some)
    }
}
