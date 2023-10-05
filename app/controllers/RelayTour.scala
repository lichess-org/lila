package controllers

import play.api.mvc.*
import views.*

import lila.app.{ given, * }
import lila.common.config.MaxPerSecond
import lila.common.{ config, IpAddress }
import lila.relay.{ RelayTour as TourModel }

final class RelayTour(env: Env, apiC: => Api, prismicC: => Prismic) extends LilaController(env):

  def index(page: Int, q: String) = Open:
    Reasonable(page, config.Max(20)):
      Ok.pageAsync:
        q.trim.take(100).some.filter(_.nonEmpty) match
          case Some(query) =>
            env.relay.pager
              .search(query, page)
              .map: pager =>
                html.relay.tour.index(Nil, pager, query)
          case None =>
            for
              active <- (page == 1).so(env.relay.api.officialActive.get({}))
              pager  <- env.relay.pager.inactive(page)
            yield html.relay.tour.index(active, pager)

  def calendar = page("broadcast-calendar", "calendar")
  def help     = page("broadcasts", "help")

  def by(owner: UserStr, page: Int) = Open:
    Reasonable(page, config.Max(20)):
      FoundPage(env.user.lightUser(owner.id)): owner =>
        env.relay.pager
          .byOwner(owner.id, page)
          .map:
            html.relay.tour.byOwner(_, owner)

  private def page(bookmark: String, menu: String) = Open:
    pageHit
    FoundPage(prismicC getBookmark bookmark): (doc, resolver) =>
      html.relay.tour.page(doc, resolver, menu)

  def form = Auth { ctx ?=> _ ?=>
    NoLameOrBot:
      Ok.page(html.relay.tourForm.create(env.relay.tourForm.create))
  }

  def create = AuthOrScopedBody(_.Study.Write) { ctx ?=> me ?=>
    NoLameOrBot:
      def whenRateLimited = negotiate(Redirect(routes.RelayTour.index()), rateLimited)
      env.relay.tourForm.create
        .bindFromRequest()
        .fold(
          err =>
            negotiate(
              BadRequest.page(html.relay.tourForm.create(err)),
              jsonFormError(err)
            ),
          setup =>
            rateLimitCreation(whenRateLimited):
              env.relay.api.tourCreate(setup) flatMap { tour =>
                negotiate(
                  Redirect(routes.RelayRound.form(tour.id)).flashSuccess,
                  JsonOk(env.relay.jsonView(tour.withRounds(Nil), withUrls = true))
                )
              }
        )
  }

  def edit(id: TourModel.Id) = Auth { ctx ?=> _ ?=>
    WithTourCanUpdate(id): tour =>
      Ok.page:
        html.relay.tourForm.edit(tour, env.relay.tourForm.edit(tour))
  }

  def update(id: TourModel.Id) = AuthOrScopedBody(_.Study.Write) { ctx ?=> me ?=>
    WithTourCanUpdate(id): tour =>
      env.relay.tourForm
        .edit(tour)
        .bindFromRequest()
        .fold(
          err =>
            negotiate(
              BadRequest.page(html.relay.tourForm.edit(tour, err)),
              jsonFormError(err)
            ),
          setup =>
            env.relay.api.tourUpdate(tour, setup) >>
              negotiate(
                Redirect(routes.RelayTour.show(tour.slug, tour.id)),
                jsonOkResult
              )
        )
  }

  def delete(id: TourModel.Id) = AuthOrScoped(_.Study.Write) { _ ?=> me ?=>
    WithTour(id): tour =>
      env.relay.api.deleteTourIfOwner(tour) inject Redirect(routes.RelayTour.by(me.username)).flashSuccess
  }

  def cloneTour(id: TourModel.Id) = Secure(_.Relay) { _ ?=> me ?=>
    WithTour(id): from =>
      env.relay.api
        .cloneTour(from)
        .map: tour =>
          Redirect(routes.RelayTour.edit(tour.id)).flashSuccess
  }

  def show(slug: String, id: TourModel.Id) = Open:
    Found(env.relay.api tourById id): tour =>
      negotiate(
        html = env.relay.api.defaultRoundToShow.get(tour.id) flatMap {
          case None =>
            ctx.me
              .soUse { env.relay.api.canUpdate(tour) }
              .flatMap:
                if _ then Redirect(routes.RelayRound.form(tour.id))
                else
                  for
                    owner <- env.user.lightUser(tour.ownerId)
                    markup = tour.markup.map(env.relay.markup(tour))
                    page <- Ok.page(html.relay.tour.showEmpty(tour, owner, markup))
                  yield page
          case Some(round) => Redirect(round.withTour(tour).path)
        },
        json = env.relay.api.withRounds(tour) map { trs =>
          Ok(env.relay.jsonView(trs, withUrls = true))
        }
      )

  def pgn(id: TourModel.Id) = OpenOrScoped(): ctx ?=>
    Found(env.relay.api tourById id): tour =>
      val canViewPrivate = ctx.isWebAuth || ctx.scopes.has(_.Study.Read)
      apiC.GlobalConcurrencyLimitPerIP.download(req.ipAddress)(
        env.relay.pgnStream.exportFullTourAs(tour, ctx.me ifTrue canViewPrivate)
      ): source =>
        asAttachmentStream(s"${env.relay.pgnStream filename tour}.pgn"):
          Ok chunked source as pgnContentType

  def apiIndex = Anon:
    apiC.jsonDownload:
      env.relay.api
        .officialTourStream(MaxPerSecond(20), getInt("nb") | 20)
        .map(env.relay.jsonView.apply(_, withUrls = true))

  private def WithTour(id: TourModel.Id)(f: TourModel => Fu[Result])(using Context): Fu[Result] =
    Found(env.relay.api tourById id)(f)

  private def WithTourCanUpdate(
      id: TourModel.Id
  )(f: TourModel => Fu[Result])(using ctx: Context): Fu[Result] =
    WithTour(id): tour =>
      ctx.me.soUse { env.relay.api.canUpdate(tour) } elseNotFound f(tour)

  private val CreateLimitPerUser = lila.memo.RateLimit[UserId](
    credits = 10 * 10,
    duration = 24.hour,
    key = "broadcast.tournament.user"
  )

  private val CreateLimitPerIP = lila.memo.RateLimit[IpAddress](
    credits = 10 * 10,
    duration = 24.hour,
    key = "broadcast.tournament.ip"
  )

  private[controllers] def rateLimitCreation(
      fail: => Fu[Result]
  )(create: => Fu[Result])(using req: RequestHeader, me: Me): Fu[Result] =
    val cost =
      if isGranted(_.Relay) then 2
      else if me.hasTitle || me.isVerified then 5
      else 10
    CreateLimitPerUser(me, fail, cost = cost):
      CreateLimitPerIP(req.ipAddress, fail, cost = cost):
        create
