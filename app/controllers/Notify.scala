package controllers

import lila.app.*

final class Notify(env: Env) extends LilaController(env):

  def recent(page: Int) = Auth { _ ?=> me ?=>
    XhrOrRedirectHome:
      env.notifyM.api
        .getNotificationsAndCount(me, page)
        .map(env.notifyM.jsonHandlers.apply)
        .map(JsonOk)
  }

  def clear = Auth { _ ?=> me ?=>
    env.notifyM.api.remove(me).inject(NoContent)
  }
