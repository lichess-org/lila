package controllers

import lila.app.*

final class Notify(env: Env) extends LilaController(env):

  def recent(page: Int) = Auth { _ ?=> me =>
    XhrOrRedirectHome:
      env.notifyM.api
        .getNotificationsAndCount(me.id, page) map env.notifyM.jsonHandlers.apply map JsonOk
  }

  def clear = Auth { _ ?=> me =>
    env.notifyM.api.remove(me.id)
  }
