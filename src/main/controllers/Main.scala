package controllers

import lila.app._
import views._

import play.api.mvc._, Results._
import play.api.data._, Forms._
import play.api.libs.json._
import play.api.libs.iteratee._
import play.api.libs.concurrent.Akka

object Main extends LilaController {

  def websocket = Socket { implicit ctx ⇒
    uid ⇒
      Env.site.socketHandler.join(
        uid = uid, userId = ctx.userId, flag = get("flag")
      )
  }

  // def captchaCheck(id: String) = Open { implicit ctx ⇒
  //   Ok(env.site.captcha get id valid ~get("solution") fold (1, 0))
  // }

  // def embed = Open { implicit ctx ⇒
  //   JsOk("""document.write("<iframe src='%s?embed=" + document.domain + "' class='lichess-iframe' allowtransparency='true' frameBorder='0' style='width: %dpx; height: %dpx;' title='Lichess free online chess'></iframe>");"""
  //     .format(env.settings.NetBaseUrl, getInt("w") | 820, getInt("h") | 650),
  //     CACHE_CONTROL -> "max-age=86400"
  //   )
  // }

  def developers = Open { implicit ctx ⇒
    fuccess {
      Ok(views.html.site.developers())
    }
  }
}
