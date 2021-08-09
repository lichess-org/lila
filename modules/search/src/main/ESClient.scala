package lila.search

import play.api.libs.json._
import play.api.libs.ws._
import play.api.libs.ws.JsonBodyWritables._
import scala.annotation.nowarn

sealed trait ESClient {

  def search[Q: Writes](query: Q, from: From, size: Size): Fu[SearchResponse]

  def count[Q: Writes](query: Q): Fu[CountResponse]

  def store(id: Id, doc: JsObject): Funit

  def deleteById(id: Id): Funit

  def deleteByIds(ids: List[Id]): Funit

  def refresh: Funit
}

final class ESClientHttp(
    ws: StandaloneWSClient,
    config: SearchConfig,
    val index: Index
)(implicit ec: scala.concurrent.ExecutionContext)
    extends ESClient {

  def store(id: Id, doc: JsObject) =
    config.writeable ?? monitor("store") {
      HTTP(s"store/${index.name}/${id.value}", doc)
    }

  def search[Q: Writes](query: Q, from: From, size: Size) =
    monitor("search") {
      HTTP(s"search/${index.name}/${from.value}/${size.value}", query, SearchResponse.apply)
        .dmap(~_)
    }

  def count[Q: Writes](query: Q) =
    monitor("count") {
      HTTP(s"count/${index.name}", query, CountResponse.apply)
        .dmap(~_)
    }

  def deleteById(id: lila.search.Id) =
    config.writeable ??
      HTTP(s"delete/id/${index.name}/${id.value}", Json.obj())

  def deleteByIds(ids: List[lila.search.Id]) =
    config.writeable ??
      HTTP(s"delete/ids/${index.name}", Json.obj("ids" -> ids.map(_.value)))

  def putMapping =
    HTTP(s"mapping/${index.name}", Json.obj())

  def storeBulk(docs: Seq[(Id, JsObject)]) =
    HTTP(
      s"store/bulk/${index.name}",
      JsObject(docs map { case (Id(id), doc) =>
        id -> JsString(Json.stringify(doc))
      })
    )

  def refresh =
    HTTP(s"refresh/${index.name}", Json.obj())

  private[search] def HTTP[D: Writes, R](url: String, data: D, read: String => R): Fu[Option[R]] =
    ws.url(s"${config.endpoint}/$url").post(Json toJson data) flatMap {
      case res if res.status == 200 => fuccess(read(res.body).some)
      case res if res.status == 400 => fuccess(none)
      case res                      => fufail(s"$url ${res.status}")
    }
  private[search] def HTTP(url: String, data: JsObject): Funit = HTTP(url, data, _ => ()).void

  private def monitor[A](op: String)(f: Fu[A]) =
    f.monTry(res => _.search.time(op, index.name, res.isSuccess))
}

final class ESClientStub extends ESClient {
  def search[Q: Writes](query: Q, from: From, size: Size) = fuccess(SearchResponse(Nil))
  def count[Q: Writes](query: Q)                          = fuccess(CountResponse(0))
  def store(id: Id, doc: JsObject)                        = funit
  @nowarn("msg=parameter value")
  def storeBulk(docs: Seq[(Id, JsObject)]) = funit
  def deleteById(id: Id)                   = funit
  def deleteByIds(ids: List[Id])           = funit
  def putMapping                           = funit
  def refresh                              = funit
}
