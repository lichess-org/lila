package controllers

import play.api.mvc._
import play.api.libs.Comet
import play.api.libs.concurrent._
import play.api.libs.json._
import akka.pattern.ask
import play.api.libs.concurrent.Execution.Implicits._

import lila.app._
import lila.socket.actorApi.GetNbMembers
// import lila.monitor._
import makeTimeout.short

object Monitor extends LilaController {

  // private def reporting = env.monitor.reporting
  // private def usernameMemo = env.user.usernameMemo
  // private def userRepo = env.user.userRepo
  // private def gameRepo = env.game.gameRepo

  def index = TODO
  // Action {
  //   Ok(views.html.monitor.monitor())
  // }

  // def websocket = WebSocket.async[JsValue] { implicit req ⇒
  //   env.monitor.socket.join(uidOption = get("sri", req))
  // }

  // def status = Open { implicit ctx ⇒
  //   Async {
  //     import lila.common.Futuristic.ioToFuture
  //     (~get("key") match {
  //       case "elo" ⇒
  //         userRepo.idsAverageElo(usernameMemo.keys).toFuture zip
  //           gameRepo.recentAverageElo(5).toFuture map {
  //             case (users, (rated, casual)) ⇒ List(users, rated, casual) mkString " "
  //           }
  //       case "moves"   ⇒ (reporting ? GetNbMoves).mapTo[Int]
  //       case "players" ⇒ (reporting ? GetNbMembers).mapTo[Int] map { "%d %d".format(_, usernameMemo.preciseCount) }
  //       case _         ⇒ (reporting ? GetStatus).mapTo[String]
  //     }) map { x ⇒ Ok(x.toString) }
  //   }
  // }
}
