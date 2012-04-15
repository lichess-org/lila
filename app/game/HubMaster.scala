package lila
package game

import play.api.libs.concurrent._
import play.api.Play.current
import akka.actor._
import akka.pattern.{ ask, pipe }
import akka.util.duration._
import akka.util.{ Duration, Timeout }
import akka.dispatch.{ Future }
import scalaz.effects._
import socket._

final class HubMaster(hubMemo: HubMemo) extends Actor {

  private implicit val timeout = Timeout(200 millis)
  private implicit val executor = Akka.system.dispatcher

  def receive = {
    case WithHubs(op) => op(hubMemo.all).unsafePerformIO
  }

  def hubActors = hubMemo.all.values

  private def hubs = hubMemo.all
}
