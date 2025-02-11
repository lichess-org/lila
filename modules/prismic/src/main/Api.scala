package lila.prismic

import play.api.libs.functional.syntax._
import play.api.libs.json.JodaReads._
import play.api.libs.json._

import com.github.blemale.scaffeine.AsyncCache
import org.joda.time._

final class Api(
    data: ApiData,
    private[prismic] val cache: AsyncCache[String, Response],
) {

  def refs: Map[String, Ref] =
    data.refs.groupBy(_.label).view.mapValues(_.head).toMap
  private def forms: Map[String, SearchForm] =
    data.forms.view
      .mapValues(form => SearchForm(this, form, form.defaultData))
      .toMap
  def search = forms("everything")
  def master: Ref =
    refs.values
      .collectFirst { case ref if ref.isMasterRef => ref }
      .getOrElse(sys.error("no master reference found"))

}

case class Ref(
    id: String,
    ref: String,
    label: String,
    isMasterRef: Boolean = false,
    scheduledAt: Option[DateTime] = None,
)

private[prismic] object Ref {

  implicit val reader: Reads[Ref] = (
    (__ \ "id").read[String] and
      (__ \ "ref").read[String] and
      (__ \ "label").read[String] and
      ((__ \ "isMasterRef").read[Boolean] orElse Reads.pure(false)) and
      (__ \ "scheduledAt").readNullable[DateTime]
  )(Ref.apply _)

}

case class Field(`type`: String, multiple: Boolean, default: Option[String])

private[prismic] object Field {
  implicit val reader: Reads[Field] = (
    (__ \ "type").read[String] and
      (__ \ "multiple").readNullable[Boolean].map(_.getOrElse(false)) and
      (__ \ "default").readNullable[String]
  )(Field.apply _)
}

case class Form(
    name: Option[String],
    method: String,
    rel: Option[String],
    enctype: String,
    action: String,
    fields: Map[String, Field],
) {

  def defaultData: Map[String, Seq[String]] = {
    fields.view
      .mapValues(_.default)
      .collect { case (key, Some(value)) =>
        (key, Seq(value))
      }
      .toMap
  }

}

private[prismic] object Form {
  implicit val reader: Reads[Form] = Json.reads[Form]
}

case class ApiData(
    refs: Seq[Ref],
    types: Map[String, String],
    tags: Seq[String],
    forms: Map[String, Form],
    oauthEndpoints: (String, String),
)

object ApiData {

  implicit val reader: Reads[ApiData] = (
    (__ \ "refs").read[Seq[Ref]] and
      (__ \ "types").read[Map[String, String]] and
      (__ \ "tags").read[Seq[String]] and
      (__ \ "forms").read[Map[String, Form]] and
      (
        (__ \ "oauth_initiate").read[String] and
          (__ \ "oauth_token").read[String] tupled
      )
  )(ApiData.apply _)

}
