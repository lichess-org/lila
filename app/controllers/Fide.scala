package controllers

import play.api.mvc.*

import lila.app.{ *, given }
import lila.core.fide.FidePlayerOrder
import lila.fide.Federation

final class Fide(env: Env) extends LilaController(env):

  import env.fide.json.given
  private def playerUrl(player: lila.fide.FidePlayer) = routes.Fide.show(player.id, player.slug)

  def index(page: Int, q: Option[String] = None) = Open:
    Reasonable(page):
      val order = get("order").flatMap(FidePlayerOrder.byKey.get) | FidePlayerOrder.default
      env.fide
        .search(q, page, order)
        .flatMap:
          case Left(p) => Redirect(playerUrl(p.player))
          case Right(pager) => renderPage(views.fide.player.index(pager, q.so(_.trim), order)).map(Ok(_))

  def show(id: chess.FideId, slug: String, page: Int) = Open:
    env.fide.repo.player
      .fetch(id)
      .flatMap:
        case None => NotFound.page(views.fide.player.notFound(id))
        case Some(player) =>
          if player.slug != slug then Redirect(playerUrl(player))
          else
            for
              user <- env.title.api.publicUserOf(player.id)
              tours <- env.relay.playerTour.playerTours(player, page)
              isFollowing <- ctx.userId.so(env.fide.repo.follower.isFollowing(_, id))
              ratings <- env.fide.repo.rating.get(player.id)
              rendered <- renderPage(views.fide.player.show(player, user, tours, ratings, isFollowing))
            yield Ok(rendered)

  def follow(fideId: chess.FideId, follow: Boolean) = AuthOrScopedBody(_.Web.Mobile): _ ?=>
    me ?=>
      val f = if follow then env.fide.repo.follower.follow else env.fide.repo.follower.unfollow
      for _ <- f(me.userId, fideId) yield NoContent

  def apiShow(id: chess.FideId) = Anon:
    Found(env.fide.playerApi.withFollow(id))(JsonOk)

  def apiSearch(q: String) = Anon:
    env.fide.search(q.some, 1, FidePlayerOrder.default).map(_.fold(Seq(_), _.currentPageResults)).map(JsonOk)

  def federations(page: Int) = Open:
    for
      feds <- env.fide.paginator.federations(page)
      renderedPage <- renderPage(views.fide.federation.index(feds))
    yield Ok(renderedPage)

  def federation(slug: String, page: Int) = Open:
    Found(env.fide.federationApi.find(slug)): fed =>
      val fedSlug = Federation.nameToSlug(fed.name)
      if slug != fedSlug then Redirect(routes.Fide.federation(fedSlug))
      else
        for
          players <- env.fide.paginator.federationPlayers(fed, page)
          playersList = views.fide.playerUi.playerList(
            players,
            FidePlayerOrder.default,
            routes.Fide.federation(fed.slug, _),
            sortable = false
          )
          rendered <- renderPage(views.fide.federation.show(fed, playersList))
        yield Ok(rendered)

  def playerPhoto(id: chess.FideId) = SecureBody(lila.web.HashedMultiPart(parse))(_.FidePlayer) {
    ctx ?=> _ ?=>
      Found(env.fide.repo.player.fetch(id)): p =>
        ctx.body.body.file("photo") match
          case Some(photo) =>
            for _ <- env.fide.playerApi.uploadPhoto(p, photo)
            yield Redirect(routes.Coach.edit)
          case None => Redirect(playerUrl(p))
  }

  def playerUpdate(id: chess.FideId) = SecureBody(_.FidePlayer) { ctx ?=> _ ?=>
    Found(env.fide.repo.player.fetch(id)): p =>
      bindForm(lila.fide.FidePlayer.form.credit(p))(
        _ => funit,
        credit => env.fide.playerApi.setPhotoCredit(p, credit)
      ).inject(Redirect(playerUrl(p)))
  }
