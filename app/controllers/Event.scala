package controllers

import scalalib.Json.given

import lila.app.*

final class Event(env: Env) extends LilaController(env):

  import env.event.api

  def show(id: String) = Open:
    FoundPage(api.oneEnabled(id)): event =>
      for html <- env.event.markdown.of(event)
      yield views.event.show(event, html)

  def manager(page: Int) = Secure(_.ManageEvent) { ctx ?=> _ ?=>
    Ok.async:
      api.pager(page).map(views.event.manager)
  }

  def apiCalendar(page: Int) = SecuredScoped(_.ManageEvent) { ctx ?=> _ ?=>
    Reasonable(page, Max(20)):
      val since = getTimestamp("since").getOrElse(nowInstant)
      val until = getTimestamp("until").getOrElse(nowInstant.plusDays(7))
      for pager <- api.between(since, until, page)
      yield JsonOk(pager.mapResults(env.event.jsonView.calendar))
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
