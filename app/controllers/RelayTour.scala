package controllers

import play.api.mvc.*
import views.*

import lila.api.context.*
import lila.app.{ given, * }
import lila.common.config.MaxPerSecond
import lila.common.{ config, IpAddress }
import lila.relay.{ RelayTour as TourModel }
import lila.user.{ User as UserModel }

final class RelayTour(env: Env, apiC: => Api, prismicC: => Prismic) extends LilaController(env):

  def index(page: Int, q: String) = Open:
    Reasonable(page, config.Max(20)):
      q.trim.take(100).some.filter(_.nonEmpty) match
        case Some(query) =>
          env.relay.pager
            .search(query, page)
            .map: pager =>
              Ok(html.relay.tour.index(Nil, pager, query))
        case None =>
          for
            active <- (page == 1).so(env.relay.api.officialActive.get({}))
            pager  <- env.relay.pager.inactive(page)
          yield Ok(html.relay.tour.index(active, pager))

  def calendar = page("broadcast-calendar", "calendar")
  def help     = page("broadcasts", "help")

  def by(owner: UserStr, page: Int) = Open:
    env.user
      .lightUser(owner.id)
      .flatMapz: owner =>
        Reasonable(page, config.Max(20)):
          env.relay.pager
            .byOwner(owner.id, page)
            .map: pager =>
              Ok(html.relay.tour.byOwner(pager, owner))

  private def page(bookmark: String, menu: String) = Open:
    pageHit
    OptionOk(prismicC getBookmark bookmark): (doc, resolver) =>
      html.relay.tour.page(doc, resolver, menu)

  def form = Auth { ctx ?=> _ =>
    NoLameOrBot:
      html.relay.tourForm.create(env.relay.tourForm.create)
  }

  def create =
    AuthOrScopedBody(_.Study.Write)(
      auth = ctx ?=>
        me =>
          NoLameOrBot:
            env.relay.tourForm.create
              .bindFromRequest()
              .fold(
                err => BadRequest(html.relay.tourForm.create(err)).toFuccess,
                setup =>
                  rateLimitCreation(me, ctx.req, Redirect(routes.RelayTour.index())):
                    env.relay.api.tourCreate(setup, me) map { tour =>
                      Redirect(routes.RelayRound.form(tour.id.value)).flashSuccess
                    }
              )
      ,
      scoped = ctx ?=>
        me =>
          NoLameOrBot(me):
            env.relay.tourForm.create
              .bindFromRequest()
              .fold(
                err => BadRequest(apiFormError(err)).toFuccess,
                setup =>
                  rateLimitCreation(me, ctx.req, rateLimited):
                    JsonOk:
                      env.relay.api.tourCreate(setup, me) map { tour =>
                        env.relay.jsonView(tour.withRounds(Nil), withUrls = true)
                      }
              )
    )

  def edit(id: TourModel.Id) = Auth { ctx ?=> _ =>
    WithTourCanUpdate(id): tour =>
      html.relay.tourForm.edit(tour, env.relay.tourForm.edit(tour))
  }

  def update(id: TourModel.Id) =
    AuthOrScopedBody(_.Study.Write)(
      auth = ctx ?=>
        me =>
          WithTourCanUpdate(id): tour =>
            env.relay.tourForm
              .edit(tour)
              .bindFromRequest()
              .fold(
                err => BadRequest(html.relay.tourForm.edit(tour, err)),
                setup =>
                  env.relay.api.tourUpdate(tour, setup, me) inject
                    Redirect(routes.RelayTour.redirectOrApiTour(tour.slug, tour.id.value))
              ),
      scoped = _ ?=>
        me =>
          env.relay.api tourById id flatMapz { tour =>
            env.relay.api.canUpdate(me, tour) flatMapz {
              env.relay.tourForm
                .edit(tour)
                .bindFromRequest()
                .fold(
                  err => BadRequest(apiFormError(err)).toFuccess,
                  setup => env.relay.api.tourUpdate(tour, setup, me) inject jsonOkResult
                )
            }
          }
    )

  def delete(id: TourModel.Id) = AuthOrScoped(_.Study.Write) { _ ?=> me =>
    ???
  }

  def redirectOrApiTour(slug: String, id: TourModel.Id) = Open:
    env.relay.api tourById id flatMapz { tour =>
      render.async:
        case Accepts.Json() =>
          JsonOk:
            env.relay.api.withRounds(tour) map { trs =>
              env.relay.jsonView(trs, withUrls = true)
            }
        case _ => redirectToTour(tour)
    }

  def pgn(id: TourModel.Id) = OpenOrScoped(): ctx ?=>
    env.relay.api tourById id mapz { tour =>
      val canViewPrivate = ctx.isWebAuth || ctx.scopes.has(_.Study.Read)
      apiC.GlobalConcurrencyLimitPerIP.download(req.ipAddress)(
        env.relay.pgnStream.exportFullTourAs(tour, ctx.me ifTrue canViewPrivate)
      ): source =>
        asAttachmentStream(s"${env.relay.pgnStream filename tour}.pgn"):
          Ok chunked source as pgnContentType
    }

  def apiIndex = Anon:
    apiC.jsonDownload:
      env.relay.api
        .officialTourStream(MaxPerSecond(20), getInt("nb") | 20)
        .map(env.relay.jsonView.apply(_, withUrls = true))

  private def redirectToTour(tour: TourModel)(using ctx: WebContext): Fu[Result] =
    env.relay.api.defaultRoundToShow.get(tour.id) flatMap {
      case None =>
        ctx.me.so { env.relay.api.canUpdate(_, tour) } mapz {
          Redirect(routes.RelayRound.form(tour.id.value))
        }
      case Some(round) => Redirect(round.withTour(tour).path)
    }

  private def WithTour(id: TourModel.Id)(f: TourModel => Fu[Result])(using AnyContext): Fu[Result] =
    OptionFuResult(env.relay.api tourById id)(f)

  private def WithTourCanUpdate(
      id: TourModel.Id
  )(f: TourModel => Fu[Result])(using ctx: AnyContext): Fu[Result] =
    WithTour(id): tour =>
      ctx.me.so { env.relay.api.canUpdate(_, tour) } flatMapz f(tour)

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
      me: UserModel,
      req: RequestHeader,
      fail: => Fu[Result]
  )(create: => Fu[Result]): Fu[Result] =
    val cost =
      if isGranted(_.Relay, me) then 2
      else if me.hasTitle || me.isVerified then 5
      else 10
    CreateLimitPerUser(me.id, fail, cost = cost):
      CreateLimitPerIP(req.ipAddress, fail, cost = cost):
        create
