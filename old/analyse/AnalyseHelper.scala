package lila.app
package analyse

import core.CoreEnv
import http.Context
import user.{ User, UserHelper }

import play.api.templates.Html
import play.api.mvc.Call

trait AnalyseHelper { 

  protected def env: CoreEnv

  def canRequestAnalysis(implicit ctx: Context) = 
    ctx.isAuth
}
