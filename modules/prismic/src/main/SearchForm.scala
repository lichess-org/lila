package lila.prismic

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.api.libs.ws.JsonBodyReadables._
import play.api.libs.ws.WSClient

import io.mola.galimatias.URL

case class SearchForm(api: Api, form: Form, data: Map[String, Seq[String]]) {

  def set(field: String, value: String): SearchForm =
    form.fields
      .get(field)
      .map { fieldDesc =>
        copy(data =
          data ++ Map(
            field -> (if (fieldDesc.multiple)
                        data.getOrElse(field, Nil) ++ Seq(value)
                      else Seq(value)),
          ),
        )
      }
      .getOrElse(sys.error(s"Unknown field $field"))

  def set(field: String, value: Int): SearchForm =
    form.fields
      .get(field)
      .map(_.`type`)
      .map {
        case "Integer" => set(field, value.toString)
        case t =>
          sys.error(
            s"Cannot use a Int as value for the field $field of type $t",
          )
      }
      .getOrElse(sys.error(s"Unknown field $field"))

  def ref(r: Ref): SearchForm    = ref(r.ref)
  def ref(r: String): SearchForm = set("ref", r)

  def query(query: String) = {
    if (form.fields.get("q").map(_.multiple) == Some(true)) {
      set("q", query)
    } else {
      // Temporary Hack for backward compatibility
      def strip(q: String) = q.trim.drop(1).dropRight(1)
      copy(data = data ++ Map("q" -> Seq(s"""[${form
          .fields("q")
          .default
          .map(strip)
          .getOrElse("")}${strip(query)}]""")))
    }
  }

  /** Build an "AND" query with all the predicates passed in parameter
    * @param predicates
    *   one or more Predicate
    * @return
    *   the SearchForm instance for chaining
    */
  def query(predicates: Predicate*): SearchForm = {
    this.query("[" + predicates.map(_.q).mkString + "]")
  }

  def page(p: Int)     = set("page", p)
  def pageSize(p: Int) = set("pageSize", p)

  def orderings(o: String) = set("orderings", o)

  def submit()(implicit
      ws: WSClient,
      es: ExecutionContext,
  ): Future[Response] = {

    def parseResponse(json: JsValue): Response =
      Response.jsonReader reads json match {
        case JsSuccess(result, _) => result
        case JsError(err) =>
          sys.error(s"Unable to parse prismic.io response: $json\n$err")
      }

    (form.method, form.enctype, form.action) match {
      case ("GET", "application/x-www-form-urlencoded", action) =>
        val url = data
          .flatMap { case (key, values) =>
            values.map(key -> _)
          }
          .foldLeft(io.mola.galimatias.URL.parse(action)) {
            case (u, (k, v)) => {
              val currentQuery = Option(u.query()).getOrElse("")
              val newQuery =
                if (currentQuery.isEmpty)
                  s"$k=$v"
                else
                  s"$currentQuery&$k=$v"
              URL.parse(u.withQuery(newQuery).toString)
            }
          }
          .toString

        api.cache.getFuture(
          url,
          _ =>
            ws.url(url)
              .withHttpHeaders("Accept" -> "application/json")
              .get() map { resp =>
              resp.status match {
                case 200 => parseResponse(resp.body[JsValue])
                case error =>
                  sys.error(
                    s"Http error(status:$error msg:${resp.statusText} body:${resp.body}",
                  )
              }
            },
        )

      case _ => sys.error("Form type not supported")
    }
  }

}

case class Response(
    results: List[Document],
    page: Int,
    resultsPerPage: Int,
    resultsSize: Int,
    totalResultsSize: Int,
    totalPages: Int,
    nextPage: Option[String],
    prevPage: Option[String],
)

private[prismic] object Response {

  implicit private val documentReader: Reads[Document] = Document.reader

  val jsonReader = (
    (__ \ "results").read[List[Document]] and
      (__ \ "page").read[Int] and
      (__ \ "results_per_page").read[Int] and
      (__ \ "results_size").read[Int] and
      (__ \ "total_results_size").read[Int] and
      (__ \ "total_pages").read[Int] and
      (__ \ "next_page").readNullable[String] and
      (__ \ "prev_page").readNullable[String]
  )(Response.apply _)
}
