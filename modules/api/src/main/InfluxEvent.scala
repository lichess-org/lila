package lila.api

import akka.actor._
import play.api.libs.ws.WSClient

import lila.common.Bus
import lila.hub.actorApi.{ DeployPost, DeployPre }

final private class InfluxEvent(
    ws: WSClient,
    endpoint: String,
    env: String
) extends Actor {

  private val seed = ornicar.scalalib.Random.nextString(6)

  override def preStart(): Unit = {
    Bus.subscribe(self, "deploy")
    event("lila_start", s"Lila starts: $seed")
  }

  implicit def ec = context.dispatcher

  def receive = {
    case DeployPre  => event("lila_deploy_pre", "Lila will soon restart")
    case DeployPost => event("lila_deploy_post", "Lila restarts for deploy now")
  }

  def event(key: String, text: String) = {
    val data = s"""event,program=lila,env=$env,title=$key text="$text""""
    ws.url(endpoint)
      .post(data)
      .effectFold(
        err => onError(s"$endpoint $data $err"),
        res => if (res.status != 204) onError(s"$endpoint $data ${res.status}")
      )
  }

  def onError(msg: String) = lila.log("influx_event").warn(msg)
}
