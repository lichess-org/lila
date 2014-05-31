package controllers

import play.api.mvc._, Results._

import io.prismic.{ Document, DocumentLinkResolver, Fragment }

import lila.app._
import views._

object Blog extends LilaController {

  private def blogApi = Env.blog.api

  def index(ref: Option[String]) = Open { implicit ctx =>
    blogApi context ref flatMap { implicit prismic =>
      blogApi recent 20 map { response =>
        Ok(views.html.blog.index(response))
      }
    }
  }

  def show(id: String, slug: String, ref: Option[String]) = Open { implicit ctx =>
    blogApi context ref flatMap { implicit prismic =>
      blogApi one id flatMap { maybeDocument =>
        checkSlug(maybeDocument, slug) {
          case Left(newSlug) => MovedPermanently(routes.Blog.show(id, newSlug, ref).url)
          case Right(doc)    => Ok(views.html.blog.show(doc))
        }
      }
    }
  }

  implicit def makeLinkResolver(prismicApi: io.prismic.Api, ref: Option[String]): DocumentLinkResolver =
    DocumentLinkResolver(prismicApi) {
      case (Fragment.DocumentLink(id, _, _, slug, false), _) => routes.Blog.show(id, slug, ref).url
      case (Fragment.DocumentLink(_, _, _, _, true), _)      => routes.Lobby.home.url
    }

  // -- Helper: Check if the slug is valid and redirect to the most recent version id needed
  private def checkSlug(document: Option[Document], slug: String)(callback: Either[String, Document] => SimpleResult)(implicit ctx: lila.api.Context) =
    document.collect {
      case document if document.slug == slug         => fuccess(callback(Right(document)))
      case document if document.slugs.contains(slug) => fuccess(callback(Left(document.slug)))
    } getOrElse notFound
}
