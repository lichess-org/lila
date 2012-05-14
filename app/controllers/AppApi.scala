package controllers

import lila._
import DataForm._

import play.api.mvc._
import play.api.libs.concurrent._

object AppApi extends LilaController {

  private val api = env.appApi

  def show(fullId: String) = Action {
    Async {
      (api show fullId).asPromise map { op ⇒
        op.unsafePerformIO.fold(e ⇒ BadRequest(e.shows), JsonOk)
      }
    }
  }

  def reloadTable(gameId: String) = Action {
    ValidIOk(api reloadTable gameId)
  }

  def start(gameId: String) = Action { implicit req ⇒
    FormValidIOk[EntryData](entryForm)(entryData ⇒ api.start(gameId, entryData))
  }

  def join(fullId: String) = Action { implicit req ⇒
    FormValidIOk[JoinData](joinForm) { join ⇒
      api.join(fullId, join._1, join._2, join._3)
    }
  }

  def activity(gameId: String, color: String) = Action {
    Async {
      api.isConnected(gameId, color).asPromise map { bool ⇒
        Ok(bool.fold(1, 0))
      }
    }
  }

  def gameVersion(gameId: String) = Action {
    Async {
      (api gameVersion gameId).asPromise map { Ok(_) }
    }
  }

  def gameInfo(gameId: String) = Action {
    (api gameInfo gameId).unsafePerformIO.fold(
      info ⇒ JsonOk(info.toMap),
      BadRequest("No such game")
    )
  }

  def rematchAccept(gameId: String, color: String, newGameId: String) = Action { implicit req ⇒
    FormValidIOk[RematchData](rematchForm)(r ⇒
      api.rematchAccept(gameId, newGameId, color, r._1, r._2, r._3, r._4))
  }

  def adjust(username: String) = Action {
    IOk(api adjust username)
  }

  def captcha = Action {
    env.site.captcha.create.unsafePerformIO.fold(
      err ⇒ BadRequest(err.shows),
      data ⇒ JsonOk(Map(
        "id" -> data._1,
        "fen" -> data._2,
        "color" -> data._3.toString
      ))
    )
  }
}
