package controllers

import scala.annotation.nowarn
import play.api.mvc.*
import scalalib.Json.given

import lila.app.{ *, given }
import lila.core.id.{ RelayTourId, RelayGroupId }
import lila.relay.{ RelayJsonView, RelayCalendar, RelayTour as TourModel, RelayGroup, RelayPlayer }
import lila.relay.ui.FormNavigation

final class RelayTour(env: Env, apiC: => Api, roundC: => RelayRound) extends LilaController(env):

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
              Ok.page(views.relay.tour.search(pager, query))
        case None =>
          for
            data <- env.relay.home.get(page)
            cms <- env.cms.renderKey("broadcast-announcement", liveCheck = true)
            res <- Ok.async(views.relay.tour.index(data, cms.map(_.html)))
          yield res

  def calendarMonth(year: Int, month: Int) = Open:
    env.relay.calendar
      .readMonth(year, month)
      .so: at =>
        for
          tours <- env.relay.calendar.atMonth(at)
          cms <- env.cms.renderKey("broadcast-calendar-announcement", liveCheck = true)
          page <- Ok.async(views.relay.tour.calendar(at, tours, cms.map(_.html)))
        yield page

  def calendar = calendarMonth(RelayCalendar.now().getYear, RelayCalendar.now().getMonth.getValue)
  def help = page("broadcasts", "help")
  def app = page("broadcaster-app", "app")

  def by(owner: UserStr, page: Int) = Open:
    Reasonable(page, Max(20)):
      FoundPage(env.user.lightUser(owner.id)): owner =>
        env.relay.pager
          .byOwner(owner.id, page)
          .map:
            views.relay.tour.byOwner(_, owner)

  def apiBy(owner: UserStr, page: Int) = AnonOrScoped(_.Study.Read, _.Web.Mobile):
    Reasonable(page, Max(20)):
      Found(env.user.lightUser(owner.id)): owner =>
        env.relay.pager
          .byOwner(owner.id, page)
          .map(_.mapResults(env.relay.jsonView.tourWithAnyRound(_)))
          .map(JsonOk(_))

  def subscribed(page: Int) = Auth { ctx ?=> me ?=>
    Reasonable(page, Max(20)):
      for
        pager <- env.relay.pager.subscribedBy(me.userId, page)
        page <- Ok.async(views.relay.tour.subscribed(pager))
      yield page
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
      views.relay.tour.page(p.title, menu):
        views.cms.render(p)

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
            for
              tour <- env.relay.api.tourCreate(setup)
              result <- negotiate(
                Redirect(routes.RelayRound.form(tour.id)).flashSuccess,
                JsonOk(env.relay.jsonView.fullTourWithRounds(tour.withRounds(Nil), group = none))
              )
            yield result
      )
  }

  def edit(id: RelayTourId) = Auth { ctx ?=> _ ?=>
    WithTourCanUpdate(id): nav =>
      Ok.page:
        views.relay.form.tour.edit(env.relay.tourForm.edit(nav.tourWithGroup), nav)
  }

  def update(id: RelayTourId) = AuthOrScopedBody(_.Study.Write) { ctx ?=> me ?=>
    WithTourCanUpdate(id): nav =>
      bindForm(env.relay.tourForm.edit(nav.tourWithGroup))(
        err =>
          negotiate(
            BadRequest.page(views.relay.form.tour.edit(err, nav)),
            jsonFormError(err)
          ),
        setup =>
          env.relay.api.tourUpdate(nav.tour, setup) >>
            negotiate(Redirect(routes.RelayTour.edit(nav.tour.id)).flashSuccess, jsonOkResult)
      )
  }

  def delete(id: RelayTourId) = AuthOrScoped(_.Study.Write) { _ ?=> me ?=>
    WithTour(id): tour =>
      env.relay.api.deleteTourIfOwner(tour) >>
        Redirect(routes.RelayTour.by(me.username)).flashSuccess
  }

  def image(id: RelayTourId, tag: Option[String]) = AuthBody(lila.web.HashedMultiPart(parse)) { ctx ?=> _ ?=>
    WithTourCanUpdate(id): nav =>
      ctx.body.body.file("image") match
        case Some(image) =>
          limit.imageUpload(rateLimited):
            env.relay.api.image.upload(nav.tour, image, tag).inject(Ok).recover { case e: Exception =>
              BadRequest(e.getMessage)
            }
        case None => env.relay.api.image.delete(nav.tour, tag) >> Ok
  }

  def playersView(id: RelayTourId) = Open:
    WithTour(id): tour =>
      env.relay.playerApi.jsonList(tour.id).map(JsonStrOk)

  def teamLeaderboard(id: RelayTourId) = Open:
    WithTour(id): tour =>
      env.relay.teamLeaderboard.leaderboardJson(tour.id).map(JsonStrOk)

  def subscribe(id: RelayTourId, isSubscribed: Boolean) = AuthOrScoped(_.Web.Mobile) { _ ?=> me ?=>
    for _ <- env.relay.api.subscribe(id, me.userId, isSubscribed)
    yield jsonOkResult
  }

  def cloneTour(id: RelayTourId) = Secure(_.Relay) { _ ?=> me ?=>
    WithTour(id): from =>
      env.relay.api
        .cloneTour(from)
        .map: tour =>
          Redirect(routes.RelayTour.edit(tour.id)).flashSuccess:
            tour.tier.isDefined.so:
              "Tournament cloned and set to private for now. See the tier selector."
  }

  def show(slug: String, id: RelayTourId) = Open:
    env.relay.api
      .tourById(id)
      .flatMap:
        case None => showGroup(slug, id.into(RelayGroupId))
        case Some(tour) =>
          if tour.isPrivate && ctx.isAnon
          then Unauthorized.page(views.site.message.relayPrivate)
          else
            env.relay.defaults.roundToLink
              .get(tour.id)
              .flatMap:
                case None =>
                  ctx
                    .useMe(env.relay.api.canUpdate(tour))
                    .flatMap:
                      if _ then Redirect(routes.RelayRound.form(tour.id))
                      else emptyBroadcastPage(tour)
                case Some(round) => Redirect(round.withTour(tour).path)

  private def showGroup(slug: String, id: RelayGroupId)(using Context): Fu[Result] =
    Found(env.relay.api.groupById(id)): group =>
      if slug != group.name.toSlug
      then Redirect(routes.RelayTour.show(group.name.toSlug, id.into(RelayTourId)))
      else
        for
          allTours <- env.relay.api.toursByIds(group.tours)
          tours = if isGrantedOpt(_.StudyAdmin) then allTours else allTours.filter(_.canView)
          page <- Ok.page(views.relay.group.show(group, tours))
        yield page

  def embedShow(@nowarn slug: String, id: RelayTourId) = Anon:
    InEmbedContext:
      val tourFu =
        env.relay.api
          .tourById(id)
          .orElse:
            env.relay.defaults.tourOfGroup.get(id.into(RelayGroupId))
      FoundEmbed(tourFu.map(_.filterNot(_.isPrivate))): tour =>
        env.relay.defaults.roundToLink
          .get(tour.id)
          .flatMap:
            _.map(_.withTour(tour)).fold(emptyBroadcastPage(tour))(roundC.embedShow(_, none))

  private def emptyBroadcastPage(tour: TourModel)(using Context) = for
    owner <- env.user.lightUser(tour.ownerIds.head)
    html = env.relay.markdown.of(tour)
    page <- Ok.page(views.relay.tour.showEmpty(tour, owner, html))
  yield page

  def apiShow(id: RelayTourId) = OpenOrScoped(_.Study.Read, _.Web.Mobile):
    Found(env.relay.api.tourById(id)): tour =>
      if !tour.canView && !isGrantedOpt(_.StudyAdmin)
      then Unauthorized(jsonError("This tournament is private"))
      else
        for
          trs <- env.relay.api.withRounds(tour)
          group <- env.relay.api.withTours.get(tour.id)
          photos <- env.relay.playerApi.photosJson(tour.id)
          json = env.relay.jsonView.fullTourWithRounds(trs, group).add("photos" -> photos.some)
        yield Ok(json)

  def pgn(id: RelayTourId) = OpenOrScoped(): ctx ?=>
    Found(env.relay.api.tourById(id)): tour =>
      val canViewPrivate = ctx.isWebAuth || ctx.scopes.has(_.Study.Read)
      apiC.GlobalConcurrencyLimitPerIP.download(req.ipAddress)(
        env.relay.pgnStream.exportFullTourAs(tour, ctx.me.ifTrue(canViewPrivate))
      ): source =>
        Ok.chunked(source)
          .asAttachmentStream(s"${env.relay.pgnStream.filename(tour)}.pgn")
          .as(pgnContentType)

  def apiIndex = Anon:
    apiC.jsonDownload:
      env.relay.tourStream
        .officialTourStream(MaxPerSecond(20), Max(getInt("nb") | 20).atMost(100))

  def apiTop(page: Int) = Anon:
    Reasonable(page, Max(20)):
      JsonOk(env.relay.home.getJson(page))

  def apiSearch(page: Int, q: String) = Anon:
    Reasonable(page, Max(20)):
      q.trim.take(100).some.filter(_.nonEmpty) match
        case Some(query) =>
          for
            tour <- env.relay.pager.search(query, page)
            res <- JsonOk(env.relay.jsonView.search(tour))
          yield res
        case None => JsonBadRequest("Search query cannot be empty")

  def player(tourId: RelayTourId, id: String) = OpenOrScoped(_.Study.Read, _.Web.Mobile): ctx ?=>
    Found(env.relay.api.tourById(tourId)): tour =>
      val decoded = lila.common.String.decodeUriPathSegment(id) | id
      val json =
        for
          player <- env.relay.playerApi.player(tour, decoded)
          fideId = player.flatMap(_.fideId)
          fp <- fideId.so(env.fide.playerApi.withFollow)
          user <- fideId.so(env.title.api.publicUserOf)
        yield player.map(RelayPlayer.json.full(tour)(_, fp.map(_.player), user, fp.map(_.follow)))
      Found(json)(JsonOk)

  private given (using RequestHeader): RelayJsonView.Config = RelayJsonView.Config(html = getBool("html"))

  private def WithTour(id: RelayTourId)(f: TourModel => Fu[Result])(using Context): Fu[Result] =
    Found(env.relay.api.tourById(id))(f)

  private def WithTourCanUpdate(
      id: RelayTourId
  )(f: (FormNavigation) => Fu[Result])(using Context, Me): Fu[Result] =
    WithTour(id): tour =>
      for
        canUpdate <- env.relay.api.canUpdate(tour)
        nav <- env.relay.api.formNavigation(tour)
        res <- if canUpdate then f(nav) else Forbidden.page(views.relay.form.noAccess(nav))
      yield res

  private[controllers] def rateLimitCreation(
      fail: => Fu[Result]
  )(create: => Fu[Result])(using req: RequestHeader, me: Me): Fu[Result] =
    val cost =
      if isGranted(_.StudyAdmin) then 1
      else if isGranted(_.Relay) then 2
      else if me.hasTitle || me.isVerified then 5
      else 10
    limit.relayTour(me.userId -> req.ipAddress, fail, cost = cost)(create)
