package controllers

import play.api.mvc._, Results._

import io.prismic.Document

import lila.app._
import views._

object Blog extends LilaController {

  def index(ref: Option[String]) = Open { implicit ctx =>
    Prismic.WithPrismic(ref) { implicit prismic =>
      prismic.api.forms("blog").ref(prismic.ref).pageSize(20).page(1).submit() map { response =>
        Ok(views.html.blog.index(response))
      }
    }(ctx.req)
  }

  def show(id: String, slug: String, ref: Option[String]) = Open { implicit ctx =>
    Prismic.WithPrismic(ref) { implicit prismic =>
      Prismic getDocument id flatMap { maybeDocument =>
        checkSlug(maybeDocument, slug) {
          case Left(newSlug) => MovedPermanently(routes.Blog.show(id, newSlug, ref).url)
          case Right(doc)    => Ok(views.html.blog.show(doc))
        }
      }
    }(ctx.req)
  }

  // -- Helper: Check if the slug is valid and redirect to the most recent version id needed
  private def checkSlug(document: Option[Document], slug: String)(callback: Either[String, Document] => SimpleResult)(implicit ctx: lila.api.Context) =
    document.collect {
      case document if document.slug == slug         => fuccess(callback(Right(document)))
      case document if document.slugs.contains(slug) => fuccess(callback(Left(document.slug)))
    } getOrElse notFound
}
