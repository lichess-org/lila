package controllers

import lila.app.{ *, given }

final class DgtCtrl(env: Env) extends LilaController(env):

  def index = Auth { _ ?=> _ ?=>
    Ok.page:
      views.html.dgt.index
  }

  def config = Auth { _ ?=> me ?=>
    Ok.pageAsync:
      findToken.map(views.html.dgt.config)
  }

  def menuShortcut = Auth { ctx ?=> me ?=>
    ctx.req
      .getQueryString("include")
      .map(_ match
        case "true"  => true
        case "false" => false
      ) match
      case Some(bool) => env.pref.api.saveTag(me, _.dgt, bool) >> Ok
      case None       => BadRequest
  }

  def generateToken = Auth { _ ?=> me ?=>
    findToken.flatMap: t =>
      t.isEmpty
        .so {
          env.oAuth.tokenApi
            .create(
              lila.oauth.OAuthTokenForm.Data(
                description = "DGT board automatic token",
                scopes = dgtScopes.value.map(_.key)
              ),
              isStudent = false
            )
            .void
        }
        .inject(Redirect(routes.DgtCtrl.config))
  }

  def play = Auth { _ ?=> me ?=>
    findToken.flatMap:
      case None => Redirect(routes.DgtCtrl.config)
      case Some(t) =>
        Ok.page(views.html.dgt.play(t))
  }

  private val dgtScopes = lila.oauth.OAuthScope.select(
    _.Challenge.Read,
    _.Challenge.Write,
    _.Preference.Read,
    _.Msg.Write,
    _.Board.Play
  )

  private def findToken(using Me) =
    env.oAuth.tokenApi.findCompatiblePersonal(dgtScopes)
