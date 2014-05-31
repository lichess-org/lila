package lila.blog

import io.prismic._
import play.api.mvc.RequestHeader

final class BlogApi(prismicUrl: String, collection: String) {

  def recent(nb: Int)(implicit ctx: BlogApi.Context) =
    ctx.api.forms(collection).ref(ctx.ref).pageSize(nb).page(1).submit()

  def one(id: String)(implicit ctx: BlogApi.Context) =
    ctx.api.forms(collection)
      .query(s"""[[:d = at(document.id, "$id")]]""")
      .ref(ctx.ref).submit() map (_.results.headOption)

  // -- Build a Prismic context
  def context(ref: Option[String])(implicit linkResolver: (Api, Option[String]) => DocumentLinkResolver) =
    prismicApi map { api =>
      BlogApi.Context(
        api,
        ref.map(_.trim).filterNot(_.isEmpty).getOrElse(api.master.ref),
        linkResolver(api, ref))
    }

  private val cache = BuiltInCache(200)
  private val logger = (level: Symbol, message: String) => level match {
    case 'DEBUG => play.api.Logger("prismic") debug message
    case 'ERROR => play.api.Logger("prismic") error message
    case _      => play.api.Logger("prismic") info message
  }

  private def prismicApi = Api.get(prismicUrl, cache = cache, logger = logger)
}

object BlogApi {

  case class Context(api: Api, ref: String, linkResolver: DocumentLinkResolver) {
    def maybeRef = Option(ref).filterNot(_ == api.master.ref)
  }
}
