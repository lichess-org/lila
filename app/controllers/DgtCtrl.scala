package controllers

import lila.app.{ given, * }

final class DgtCtrl(env: Env) extends LilaController(env):

  def index = Auth { _ ?=> _ ?=>
    Ok.page:
      views.html.dgt.index
  }

  def config = Auth { _ ?=> me ?=>
    Ok.pageAsync:
      findToken.map(views.html.dgt.config)
  }

  def generateToken = Auth { _ ?=> me ?=>
    findToken.flatMap: t =>
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

  def play = Auth { _ ?=> me ?=>
    findToken.flatMap:
      case None => Redirect(routes.DgtCtrl.config)
      case Some(t) =>
        if !ctx.pref.hasDgt then env.pref.api.saveTag(me, _.dgt, true)
        Ok.page(views.html.dgt.play(t))
  }

  private val dgtScopes = lila.oauth.OAuthScope.select(
    _.Challenge.Read,
    _.Challenge.Write,
    _.Preference.Read,
    _.Msg.Write,
    _.Board.Play
  )

  private def findToken(using me: Me) =
    env.oAuth.tokenApi.findCompatiblePersonal(me, dgtScopes)
