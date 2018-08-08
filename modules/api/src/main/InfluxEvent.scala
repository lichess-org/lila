package lidraughts.api

import akka.actor._
import play.api.libs.ws.WS
import play.api.Play.current

import lidraughts.hub.actorApi.{ DeployPre, DeployPost }

private final class InfluxEvent(endpoint: String, env: String) extends Actor {

  override def preStart(): Unit = {
    context.system.lidraughtsBus.subscribe(self, 'deploy)
    event("lidraughts_start", "Lidraughts starts")
  }

  def receive = {
    case DeployPre => event("lidraughts_deploy_pre", "Lidraughts will soon restart")
    case DeployPost => event("lidraughts_deploy_post", "Lidraughts restarts for deploy now")
  }

  def event(key: String, text: String) = {
    val data = s"""event,program=lidraughts,env=$env,title=$key text="$text""""
    WS.url(endpoint).post(data).effectFold(
      err => onError(s"$endpoint $data $err"),
      res => if (res.status != 204) onError(s"$endpoint $data ${res.status}")
    )
  }

  def onError(msg: String) = lidraughts.log("influx_event").warn(msg)
}
