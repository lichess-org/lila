package controllers

import play.api.mvc.*

import scalalib.Json.given
import lila.app.{ *, given }
import lila.core.net.IpAddress
import lila.relay.RelayTour as TourModel
import lila.core.id.RelayTourId

final class RelayTour(env: Env, apiC: => Api) extends LilaController(env):

  def index(page: Int, q: String) = Open:
    indexResults(page, q)

  def indexLang = LangPage(routes.RelayTour.index())(indexResults(1, ""))

  private def indexResults(page: Int, q: String)(using ctx: Context) =
    Reasonable(page, Max(20)):
      q.trim.take(100).some.filter(_.nonEmpty) match
        case Some(query) =>
          env.relay.pager
            .search(query, page)
            .flatMap: pager =>
              Ok.async:
                views.relay.tour.search(pager, query)
        case None =>
          for
            active   <- (page == 1).so(env.relay.listing.active.get({}))
            upcoming <- (page == 1).so(env.relay.listing.upcoming.get({}))
            past     <- env.relay.pager.inactive(page)
            res      <- Ok.async(views.relay.tour.index(active, upcoming, past))
          yield res

  def calendar = page("broadcast-calendar", "calendar")
  def help     = page("broadcasts", "help")

  def by(owner: UserStr, page: Int) = Open:
    Reasonable(page, Max(20)):
      FoundPage(env.user.lightUser(owner.id)): owner =>
        env.relay.pager
          .byOwner(owner.id, page)
          .map:
            views.relay.tour.byOwner(_, owner)

  def apiBy(owner: UserStr, page: Int) = Open:
    Reasonable(page, Max(20)):
      Found(env.user.lightUser(owner.id)): owner =>
        env.relay.pager
          .byOwner(owner.id, page)
          .map(_.mapResults(env.relay.jsonView(_)))
          .map(JsonOk(_))

  def subscribed(page: Int) = Auth { ctx ?=> me ?=>
    Reasonable(page, Max(20)):
      env.relay.pager
        .subscribedBy(me.userId, page)
        .flatMap: pager =>
          Ok.async:
            views.relay.tour.subscribed(pager)
  }

  def allPrivate(page: Int) = Secure(_.StudyAdmin) { _ ?=> _ ?=>
    Reasonable(page, Max(20)):
      env.relay.pager
        .allPrivate(page)
        .flatMap: pager =>
          Ok.async:
            views.relay.tour.allPrivate(pager)
  }

  private def page(key: String, menu: String) = Open:
    pageHit
    FoundPage(env.cms.renderKey(key)): p =>
      views.relay.tour.page(p.title, views.cms.render(p), menu)

  def form = Auth { ctx ?=> _ ?=>
    NoLameOrBot:
      Ok.page(views.relay.form.tour.create(env.relay.tourForm.create))
  }

  def create = AuthOrScopedBody(_.Study.Write) { ctx ?=> me ?=>
    NoLameOrBot:
      def whenRateLimited = negotiate(Redirect(routes.RelayTour.index()), rateLimited)
      bindForm(env.relay.tourForm.create)(
        err =>
          negotiate(
            BadRequest.page(views.relay.form.tour.create(err)),
            jsonFormError(err)
          ),
        setup =>
          rateLimitCreation(whenRateLimited):
            env.relay.api.tourCreate(setup).flatMap { tour =>
              negotiate(
                Redirect(routes.RelayRound.form(tour.id)).flashSuccess,
                JsonOk(env.relay.jsonView.fullTourWithRounds(tour.withRounds(Nil)))
              )
            }
      )
  }

  def edit(id: RelayTourId) = Auth { ctx ?=> _ ?=>
    WithTourCanUpdate(id): tg =>
      Ok.page:
        views.relay.form.tour.edit(tg, env.relay.tourForm.edit(tg))
  }

  def update(id: RelayTourId) = AuthOrScopedBody(_.Study.Write) { ctx ?=> me ?=>
    WithTourCanUpdate(id): tg =>
      bindForm(env.relay.tourForm.edit(tg))(
        err =>
          negotiate(
            BadRequest.page(views.relay.form.tour.edit(tg, err)),
            jsonFormError(err)
          ),
        setup =>
          env.relay.api.tourUpdate(tg.tour, setup) >>
            negotiate(
              Redirect(routes.RelayTour.show(tg.tour.slug, tg.tour.id)),
              jsonOkResult
            )
      )
  }

  def delete(id: RelayTourId) = AuthOrScoped(_.Study.Write) { _ ?=> me ?=>
    WithTour(id): tour =>
      env.relay.api.deleteTourIfOwner(tour).inject(Redirect(routes.RelayTour.by(me.username)).flashSuccess)
  }

  def image(id: RelayTourId, tag: Option[String]) = AuthBody(parse.multipartFormData) { ctx ?=> me ?=>
    WithTourCanUpdate(id): tg =>
      ctx.body.body.file("image") match
        case Some(image) =>
          limit.imageUpload(ctx.ip, rateLimited):
            (env.relay.api.image.upload(me, tg.tour, image, tag) >> {
              Ok
            }).recover { case e: Exception =>
              BadRequest(e.getMessage)
            }
        case None => env.relay.api.image.delete(tg.tour, tag) >> Ok
  }

  def leaderboardView(id: RelayTourId) = Open:
    WithTour(id): tour =>
      tour.autoLeaderboard.so(env.relay.leaderboard(tour)).map(_.fold(notFoundJson())(JsonStrOk))

  def subscribe(id: RelayTourId, isSubscribed: Boolean) = Auth { _ ?=> me ?=>
    env.relay.api.subscribe(id, me.userId, isSubscribed).inject(jsonOkResult)
  }

  def cloneTour(id: RelayTourId) = Secure(_.Relay) { _ ?=> me ?=>
    WithTour(id): from =>
      env.relay.api
        .cloneTour(from)
        .map: tour =>
          Redirect(routes.RelayTour.edit(tour.id)).flashSuccess
  }

  def show(slug: String, id: RelayTourId) = Open:
    Found(env.relay.api.tourById(id)): tour =>
      env.relay.listing.defaultRoundToShow
        .get(tour.id)
        .flatMap:
          case None =>
            ctx.me
              .soUse(env.relay.api.canUpdate(tour))
              .flatMap:
                if _ then Redirect(routes.RelayRound.form(tour.id))
                else
                  for
                    owner <- env.user.lightUser(tour.ownerId)
                    markup = tour.markup.map(env.relay.markup(tour))
                    page <- Ok.page(views.relay.tour.showEmpty(tour, owner, markup))
                  yield page
          case Some(round) => Redirect(round.withTour(tour).path)

  def apiShow(id: RelayTourId) = Open:
    Found(env.relay.api.tourById(id)): tour =>
      env.relay.api
        .withRounds(tour)
        .map: trs =>
          Ok(env.relay.jsonView.fullTourWithRounds(trs))

  def pgn(id: RelayTourId) = OpenOrScoped(): ctx ?=>
    Found(env.relay.api.tourById(id)): tour =>
      val canViewPrivate = ctx.isWebAuth || ctx.scopes.has(_.Study.Read)
      apiC.GlobalConcurrencyLimitPerIP.download(req.ipAddress)(
        env.relay.pgnStream.exportFullTourAs(tour, ctx.me.ifTrue(canViewPrivate))
      ): source =>
        asAttachmentStream(s"${env.relay.pgnStream.filename(tour)}.pgn"):
          Ok.chunked(source).as(pgnContentType)

  def apiIndex = Anon:
    apiC.jsonDownload:
      env.relay.tourStream
        .officialTourStream(MaxPerSecond(20), Max(getInt("nb") | 20).atMost(100))

  private def WithTour(id: RelayTourId)(f: TourModel => Fu[Result])(using Context): Fu[Result] =
    Found(env.relay.api.tourById(id))(f)

  private def WithTourCanUpdate(
      id: RelayTourId
  )(f: TourModel.WithGroupTours => Fu[Result])(using ctx: Context): Fu[Result] =
    WithTour(id): tour =>
      ctx.me
        .soUse { env.relay.api.canUpdate(tour) }
        .elseNotFound:
          env.relay.api.withTours.addTo(tour).flatMap(f)

  private[controllers] def rateLimitCreation(
      fail: => Fu[Result]
  )(create: => Fu[Result])(using req: RequestHeader, me: Me): Fu[Result] =
    val cost =
      if isGranted(_.StudyAdmin) then 1
      else if isGranted(_.Relay) then 2
      else if me.hasTitle || me.isVerified then 5
      else 10
    limit.relayTour(me.userId -> req.ipAddress, fail, cost = cost)(create)
