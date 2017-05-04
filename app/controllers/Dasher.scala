package controllers

import play.api.mvc._
import scala.collection.breakOut

import lila.api.Context
import lila.app._
import lila.pref.JsonView._
import play.api.libs.json._

object Dasher extends LilaController {

  private def translations(implicit ctx: Context) = Env.i18n.jsDump.keysToObject(List(
    Env.i18n.keys.profile,
    Env.i18n.keys.inbox,
    Env.i18n.keys.preferences,
    Env.i18n.keys.logOut,
    Env.i18n.keys.networkLagBetweenYouAndLichess,
    Env.i18n.keys.timeToProcessAMoveOnLichessServer,
    Env.i18n.keys.sound
  ), Env.i18n.pool lang ctx.req)

  def get = Open { implicit ctx =>
    negotiate(
      html = notFound,
      api = _ => ctx.me match {
      case None => fuccess(unauthorizedApiResult)
      case Some(me) => Env.pref.api.getPref(me) map { prefs =>
        Ok {
          Json.obj(
            "user" -> lila.common.LightUser.lightUserWrites.writes(me.light),
            "lang" -> Json.obj(
              "current" -> Env.i18n.pool.lang(ctx.req).language.toString,
              "accepted" -> (ctx.req.acceptLanguages.map(_.language.toString)(breakOut): List[String]).distinct
            ),
            "sound" -> Json.obj(
              "list" -> lila.pref.SoundSet.list.map { set =>
                s"${set.key} ${set.name}"
              }
            ),
            "background" -> Json.obj(
              "current" -> ctx.currentBg,
              "image" -> ctx.bgImg
            ),
            "board" -> Json.obj(
              "is3d" -> ctx.is3d,
              "zoom" -> ctx.zoom
            ),
            "kid" -> me.kid,
            "coach" -> isGranted(_.Coach),
            "prefs" -> prefs,
            "i18n" -> translations
          )
        }
      }
    }
    )
  }
}
