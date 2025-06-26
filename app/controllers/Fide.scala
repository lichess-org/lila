package controllers

import play.api.mvc.*

import lila.app.{ *, given }
import lila.fide.Federation
import lila.fide.FideJson.given

final class Fide(env: Env) extends LilaController(env):

  def index(page: Int, q: Option[String] = None) = Open:
    Reasonable(page):
      env.fide
        .search(q, page)
        .flatMap:
          case Left(player) => Redirect(routes.Fide.show(player.id, player.slug))
          case Right(pager) => renderPage(views.fide.player.index(pager, q.so(_.trim))).map(Ok(_))

  def show(id: chess.FideId, slug: String, page: Int) = Open { ctx ?=>
    env.fide.repo.player
      .fetch(id)
      .flatMap:
        case None         => NotFound.page(views.fide.player.notFound(id))
        case Some(player) =>
          if player.slug != slug then Redirect(routes.Fide.show(id, player.slug))
          else
            for
              user        <- env.title.api.publicUserOf(player.id)
              tours       <- env.relay.playerTour.playerTours(player, page)
              isFollowing <- ctx.me
                .fold(Future.successful(false))(me => env.fide.repo.follower.isFollowing(me.userId, id))
              rendered <- renderPage(views.fide.player.show(player, user, tours, isFollowing))
            yield Ok(rendered)
  }

  def follow(fideId: chess.FideId, follow: Boolean) = AuthBody { _ ?=> me ?=>
    follow match
      case true  => env.fide.repo.follower.follow(me.userId, fideId).map(_ => Ok)
      case false => env.fide.repo.follower.unfollow(me.userId, fideId).map(_ => Ok)
  }

  def apiShow(id: chess.FideId) = Anon:
    Found(env.fide.repo.player.fetch(id))(JsonOk)

  def apiSearch(q: String) = Anon:
    env.fide.search(q.some, 1).map(_.fold(Seq(_), _.currentPageResults)).map(JsonOk)

  def federations(page: Int) = Open:
    for
      feds         <- env.fide.paginator.federations(page)
      renderedPage <- renderPage(views.fide.federation.index(feds))
    yield Ok(renderedPage)

  def federation(slug: String, page: Int) = Open:
    Found(env.fide.federationApi.find(slug)): fed =>
      val fedSlug = Federation.nameToSlug(fed.name)
      if slug != fedSlug then Redirect(routes.Fide.federation(fedSlug))
      else
        for
          players  <- env.fide.paginator.federationPlayers(fed, page)
          rendered <- renderPage(views.fide.federation.show(fed, players))
        yield Ok(rendered)
