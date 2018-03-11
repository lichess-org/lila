package controllers

import play.api.mvc._

import io.prismic.Document

import lila.app._

object Blog extends LilaController {

  private def blogApi = Env.blog.api

  import Prismic._

  def index(ref: Option[String]) = Open { implicit ctx =>
    blogApi context ref flatMap { implicit prismic =>
      blogApi.recent(prismic.api, ref, 50) flatMap {
        case Some(response) => fuccess(Ok(views.html.blog.index(response)))
        case _ => notFound
      }
    }
  }

  def show(id: String, slug: String, ref: Option[String]) = Open { implicit ctx =>
    blogApi context ref flatMap { implicit prismic =>
      blogApi.one(prismic.api, ref, id) flatMap { maybeDocument =>
        checkSlug(maybeDocument, slug) {
          case Left(newSlug) => MovedPermanently(routes.Blog.show(id, newSlug, ref).url)
          case Right(doc) => Ok(views.html.blog.show(doc))
        }
      } recoverWith {
        case e: RuntimeException if e.getMessage contains "Not Found" => notFound
      }
    }
  }

  def atom(ref: Option[String]) = Action.async { implicit req =>
    blogApi context ref flatMap { implicit prismic =>
      blogApi.recent(prismic.api, ref, 50) map {
        _ ?? (_.results)
      } map { docs =>
        Ok(views.xml.blog.atom(docs)) as XML
      }
    }
  }

  def discuss(id: String) = Open { implicit ctx =>
    val categSlug = "general-chess-discussion"
    val topicSlug = s"blog-$id"
    val redirect = Redirect(routes.ForumTopic.show(categSlug, topicSlug))
    lila.forum.TopicRepo.existsByTree(categSlug, topicSlug) flatMap {
      case true => fuccess(redirect)
      case _ => blogApi context none flatMap { implicit prismic =>
        blogApi.one(prismic.api, none, id) flatMap {
          _ ?? { doc =>
            lila.forum.CategRepo.bySlug(categSlug) flatMap {
              _ ?? { categ =>
                Env.forum.topicApi.makeBlogDiscuss(
                  categ = categ,
                  slug = topicSlug,
                  name = doc.getText("blog.title") | "New blog post",
                  url = s"${Env.api.Net.BaseUrl}${routes.Blog.show(doc.id, doc.slug)}"
                )
              }
            } inject redirect
          }
        }
      }
    }
  }

  // -- Helper: Check if the slug is valid and redirect to the most recent version id needed
  private def checkSlug(document: Option[Document], slug: String)(callback: Either[String, Document] => Result)(implicit ctx: lila.api.Context) =
    document.collect {
      case document if document.slug == slug => fuccess(callback(Right(document)))
      case document if document.slugs.contains(slug) => fuccess(callback(Left(document.slug)))
    } getOrElse notFound
}
