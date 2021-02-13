package controllers

import lila.app._
import lila.oauth.OAuthScope

final class DgtCtrl(env: Env) extends LilaController(env) {

  def index =
    Auth { implicit ctx => _ =>
      Ok(views.html.dgt.index).fuccess
    }

  def config =
    Auth { implicit ctx => me =>
      findToken(me) map { token =>
        Ok(views.html.dgt.config(token))
      }
    }

  def generateToken =
    Auth { _ => me =>
      findToken(me) flatMap { t =>
        t.isEmpty.?? {
          val token = lila.oauth.OAuthForm.token.Data(
            description = "DGT board automatic token",
            scopes = dgtScopes.toList.map(_.key)
          ) make me
          env.oAuth.tokenApi.create(token)
        } inject Redirect(routes.DgtCtrl.config)
      }
    }

  def play =
    Auth { implicit ctx => me =>
      findToken(me) map {
        case None => Redirect(routes.DgtCtrl.config)
        case Some(t) =>
          if (!ctx.pref.hasDgt) env.pref.api.saveTag(me, _.dgt, true)
          Ok(views.html.dgt.play(t))
      }
    }

  private val dgtScopes: Set[OAuthScope] = {
    Set(
      OAuthScope.Challenge.Read,
      OAuthScope.Challenge.Write,
      OAuthScope.Preference.Read,
      OAuthScope.Msg.Write,
      OAuthScope.Board.Play
    )
  }

  private def findToken(me: lila.user.User) =
    env.oAuth.tokenApi.findCompatible(me, dgtScopes)
}
