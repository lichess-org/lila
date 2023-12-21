package lila.relay

final private class RelayProxy(colls: RelayColls)(using Executor):

  import RelayProxy.*

  def post(data: form.Data): Funit =
    ???

object RelayProxy:

  object form:
    val form = Form:
      mapping(
        "url"  -> nonEmptyText,
        "body" -> nonEmptyText
      )(Data.apply)(unapply)
    case class Data(url: String, body: String)
