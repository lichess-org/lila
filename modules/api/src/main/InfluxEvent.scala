package lila.api

import akka.actor._
import play.api.libs.ws.WS
import play.api.Play.current

import lila.hub.actorApi.{ DeployPre, DeployPost }
import lila.common.Bus

private final class InfluxEvent(endpoint: String, env: String) extends Actor {

  private val seed = ornicar.scalalib.Random.nextString(6)

  override def preStart(): Unit = {
    Bus.subscribe(self, 'deploy)
    event("lila_start", s"Lila starts: $seed".pp)
  }

  def receive = {
    case DeployPre => event("lila_deploy_pre", "Lila will soon restart")
    case DeployPost => event("lila_deploy_post", "Lila restarts for deploy now")
  }

  def event(key: String, text: String) = {
    val data = s"""event,program=lila,env=$env,title=$key text="$text""""
    WS.url(endpoint).post(data).effectFold(
      err => onError(s"$endpoint $data $err"),
      res => if (res.status != 204) onError(s"$endpoint $data ${res.status}")
    )
  }

  def onError(msg: String) = lila.log("influx_event").warn(msg)
}
