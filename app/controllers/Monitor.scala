package controllers

import akka.pattern.ask
import play.api.libs.json._
import play.api.mvc._

import lila.app._
import lila.monitor.actorApi._
import lila.socket.actorApi.PopulationGet
import makeTimeout.short

object Monitor extends LilaController {

  private def env = Env.monitor

  def index = Secure(_.Admin) { ctx =>
    me =>
      Ok(views.html.monitor.monitor()).fuccess
  }

  def websocket = SocketOption[JsValue] { implicit ctx =>
    get("sri") ?? { sri =>
      env.socketHandler(sri) map some
    }
  }

  def statusParam = Action.async { implicit req =>
    handleStatus(~get("key", req))
  }

  def status(key: String) = Action.async { implicit req =>
    handleStatus(key)
  }

  private def handleStatus(key: String) = key match {
    case "threads"     => Ok(java.lang.management.ManagementFactory.getThreadMXBean.getThreadCount).fuccess
    case "moves"       => (env.reporting ? GetNbMoves).mapTo[Int] map { Ok(_) }
    case "moveLatency" => (env.reporting ? GetMoveLatency).mapTo[Int] map { Ok(_) }
    case "players" => {
      (env.reporting ? PopulationGet).mapTo[Int] map { "%d %d".format(_, Env.user.onlineUserIdMemo.count) }
    } map { Ok(_) }
    case "uptime" => fuccess {
      val up = lila.common.PlayApp.uptime
      Ok {
        s"""${prop("java.vm.name")} ${prop("java.vendor")} ${prop("java.version")}
${prop("user.name")} @ ${prop("os.arch")} ${prop("os.name")} ${prop("os.version")}
uptime: ${org.joda.time.format.PeriodFormat.wordBased(new java.util.Locale("en")).print(up)}
uptime seconds: ${up.toStandardSeconds.getSeconds}
last deploy: ${lila.common.PlayApp.startedAt}"""
      }
    }
    case "locale" => Ok(java.util.Locale.getDefault.toString).fuccess
    case key      => BadRequest(s"Unknown monitor status key: $key").fuccess
  }

  private def prop(name: String) = System.getProperty(name)
}
