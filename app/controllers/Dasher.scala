package controllers

import play.api.libs.json._
import play.api.mvc._
import scala.collection.breakOut

import lila.api.Context
import lila.app._
import lila.common.LightUser.lightUserWrites
import lila.pref.JsonView._

object Dasher extends LilaController {

  private def translations(implicit ctx: Context) = Env.i18n.jsDump.keysToObject(
    ctx.isAnon.fold(
      List(
        Env.i18n.keys.signIn,
        Env.i18n.keys.signUp
      ),
      List(
        Env.i18n.keys.profile,
        Env.i18n.keys.inbox,
        Env.i18n.keys.preferences,
        Env.i18n.keys.logOut
      )
    ) ::: List(
        Env.i18n.keys.networkLagBetweenYouAndLichess,
        Env.i18n.keys.timeToProcessAMoveOnLichessServer,
        Env.i18n.keys.sound
      ), Env.i18n.pool lang ctx.req
  )

  def get = Open { implicit ctx =>
    negotiate(
      html = notFound,
      api = _ => Ok {
      Json.obj(
        "user" -> ctx.me.map(_.light),
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
          "image" -> ctx.pref.bgImg
        ),
        "board" -> Json.obj(
          "is3d" -> ctx.pref.is3d,
          "zoom" -> ctx.zoom
        ),
        "theme" -> Json.obj(
          "d2" -> Json.obj(
            "current" -> ctx.currentTheme.name,
            "list" -> lila.pref.Theme.list.map(_.name)
          ),
          "d3" -> Json.obj(
            "current" -> ctx.currentTheme3d.name,
            "list" -> lila.pref.Theme3d.list.map(_.name)
          )
        ),
        "piece" -> Json.obj(
          "d2" -> Json.obj(
            "current" -> ctx.currentPieceSet.name,
            "list" -> lila.pref.PieceSet.list.map(_.name)
          ),
          "d3" -> Json.obj(
            "current" -> ctx.currentPieceSet3d.name,
            "list" -> lila.pref.PieceSet3d.list.map(_.name)
          )
        ),
        "kid" -> ctx.me ?? (_.kid),
        "coach" -> isGranted(_.Coach),
        "i18n" -> translations
      )
    } fuccess
    )
  }
}
