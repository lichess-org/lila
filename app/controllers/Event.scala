package controllers

import lila.app.*

final class Event(env: Env) extends LilaController(env):

  import env.event.api

  def show(id: String) = Open:
    FoundPage(api.oneEnabled(id))(views.event.show)

  def manager = Secure(_.ManageEvent) { ctx ?=> _ ?=>
    Ok.async:
      api.list.map(views.event.manager)
  }

  def edit(id: String) = Secure(_.ManageEvent) { ctx ?=> _ ?=>
    FoundPage(api.one(id)): event =>
      views.event.edit(event, api.editForm(event))
  }

  def update(id: String) = SecureBody(_.ManageEvent) { ctx ?=> me ?=>
    Found(api.one(id)): event =>
      bindForm(api.editForm(event))(
        err => BadRequest.page(views.event.edit(event, err)),
        data => api.update(event, data).inject(Redirect(routes.Event.edit(id)).flashSuccess)
      )
  }

  def form = Secure(_.ManageEvent) { ctx ?=> _ ?=>
    Ok.page:
      views.event.create(api.createForm)
  }

  def create = SecureBody(_.ManageEvent) { ctx ?=> me ?=>
    bindForm(api.createForm)(
      err => BadRequest.page(views.event.create(err)),
      data =>
        api.create(data).map { event =>
          Redirect(routes.Event.edit(event.id)).flashSuccess
        }
    )
  }

  def cloneE(id: String) = Secure(_.ManageEvent) { ctx ?=> _ ?=>
    FoundPage(api.one(id)): old =>
      views.event.create(api.editForm(api.clone(old)))
  }
