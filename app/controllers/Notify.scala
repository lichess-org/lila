package controllers

import lila.app.*

final class Notify(env: Env) extends LilaController(env):

  def recent(page: Int) = Auth { _ ?=> me ?=>
    XhrOrRedirectHome:
      for notifs <- env.notifyM.api.getNotificationsAndCount(me, page)
      yield Ok(env.notifyM.jsonHandlers(notifs))
  }

  def clear = Auth { _ ?=> me ?=>
    env.notifyM.api.remove(me).inject(NoContent)
  }
