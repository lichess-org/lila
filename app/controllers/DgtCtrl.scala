package controllers

import lila.app.{ given, * }

final class DgtCtrl(env: Env) extends LilaController(env):

  def index = Auth { _ ?=> _ =>
    Ok(views.html.dgt.index).toFuccess
  }

  def config = Auth { _ ?=> me =>
    findToken(me) map { token =>
      Ok(views.html.dgt.config(token))
    }
  }

  def generateToken = Auth { _ ?=> me =>
    findToken(me).flatMap: t =>
      t.isEmpty.so {
        env.oAuth.tokenApi.create(
          lila.oauth.OAuthTokenForm.Data(
            description = "DGT board automatic token",
            scopes = dgtScopes.value.map(_.key)
          ),
          me,
          isStudent = false
        ) >>
          env.pref.api.saveTag(me, _.dgt, true)
      } inject Redirect(routes.DgtCtrl.config)
  }

  def play = Auth { _ ?=> me =>
    findToken(me).map:
      case None => Redirect(routes.DgtCtrl.config)
      case Some(t) =>
        if (!ctx.pref.hasDgt) env.pref.api.saveTag(me, _.dgt, true)
        Ok(views.html.dgt.play(t))
  }

  private val dgtScopes = lila.oauth.OAuthScope.select(
    _.Challenge.Read,
    _.Challenge.Write,
    _.Preference.Read,
    _.Msg.Write,
    _.Board.Play
  )

  private def findToken(me: lila.user.User) =
    env.oAuth.tokenApi.findCompatiblePersonal(me, dgtScopes)
