package lila.search

import play.api.libs.json._

sealed trait ESClient {

  def search[Q: Writes](query: Q, from: From, size: Size): Fu[SearchResponse]

  def count[Q: Writes](query: Q): Fu[CountResponse]

  def store(id: Id, doc: JsObject): Funit

  def deleteById(id: Id): Funit

  def deleteByIds(ids: List[Id]): Funit

  def refresh: Funit
}

final class ESClientHttp(
    endpoint: String,
    val index: Index,
    writeable: Boolean
) extends ESClient {
  import play.api.libs.ws.WS
  import play.api.Play.current

  def store(id: Id, doc: JsObject) = writeable ?? monitor("store") {
    HTTP(s"store/${index.name}/${id.value}", doc)
  }

  def search[Q: Writes](query: Q, from: From, size: Size) = monitor("search") {
    HTTP(s"search/${index.name}/${from.value}/${size.value}", query, SearchResponse.apply)
  }

  def count[Q: Writes](query: Q) = monitor("count") {
    HTTP(s"count/${index.name}", query, CountResponse.apply)
  }

  def deleteById(id: lila.search.Id) = writeable ??
    HTTP(s"delete/id/${index.name}/${id.value}", Json.obj())

  def deleteByIds(ids: List[lila.search.Id]) = writeable ??
    HTTP(s"delete/ids/${index.name}", Json.obj("ids" -> ids.map(_.value)))

  def putMapping =
    HTTP(s"mapping/${index.name}/${index.name}", Json.obj())

  def storeBulk(docs: Seq[(Id, JsObject)]) =
    HTTP(s"store/bulk/${index.name}/${index.name}", JsObject(docs map {
      case (Id(id), doc) => id -> JsString(Json.stringify(doc))
    }))

  def refresh =
    HTTP(s"refresh/${index.name}", Json.obj())

  private[search] def HTTP[D: Writes, R](url: String, data: D, read: String => R): Fu[R] =
    WS.url(s"$endpoint/$url").post(Json toJson data) flatMap {
      case res if res.status == 200 => fuccess(read(res.body))
      case res => fufail(s"$url ${res.status}")
    }
  private[search] def HTTP(url: String, data: JsObject): Funit = HTTP(url, data, _ => ())

  private def monitor[A](op: String)(f: Fu[A]) =
    f.mon(_.search.client(op)).addEffects(
      _ => lila.mon.search.failure(op)(),
      _ => lila.mon.search.success(op)()
    )
}

final class ESClientStub extends ESClient {
  def search[Q: Writes](query: Q, from: From, size: Size) = fuccess(SearchResponse(Nil))
  def count[Q: Writes](query: Q) = fuccess(CountResponse(0))
  def store(id: Id, doc: JsObject) = funit
  def storeBulk(docs: Seq[(Id, JsObject)]) = funit
  def deleteById(id: Id) = funit
  def deleteByIds(ids: List[Id]) = funit
  def putMapping = funit
  def refresh = funit
}
