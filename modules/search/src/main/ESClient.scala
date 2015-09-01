package lila.search

import play.api.libs.json._

sealed trait ESClient {

  def search(query: Query, from: From, size: Size): Fu[SearchResponse]

  def count(query: Query): Fu[CountResponse]

  def store(id: Id, doc: JsObject): Funit

  def storeBulk(docs: Seq[(Id, JsObject)]): Funit

  def deleteById(id: Id): Funit

  def deleteByQuery(query: StringQuery): Funit

  def putMapping: Funit
}

final class ESClientHttp(endpoint: String, index: Index) extends ESClient {
  import play.api.libs.ws.WS
  import play.api.Play.current

  def store(id: Id, doc: JsObject) =
    HTTP(s"store/${index.name}/${id.value}", doc)

  def storeBulk(docs: Seq[(Id, JsObject)]) =
    HTTP(s"store/bulk/${index.name}", JsObject(docs map {
      case (Id(id), doc) => id -> doc
    }))

  def search(query: Query, from: From, size: Size) =
    HTTP(s"search/${index.name}/${from.value}/${size.value}", query.toJson, SearchResponse.apply)

  def count(query: Query) =
    HTTP(s"count/${index.name}", query.toJson, CountResponse.apply)

  def deleteById(id: lila.search.Id) =
    HTTP(s"delete/id/${index.name}/${id.value}", Json.obj())

  def deleteByQuery(query: lila.search.StringQuery) =
    HTTP(s"delete/query/${index.name}/${query.value}", Json.obj())

  def putMapping = HTTP(s"mapping/${index.name}", Json.obj())

  private def HTTP[A](url: String, data: JsObject, read: String => A): Fu[A] =
    WS.url(s"$endpoint/$url").post(data) flatMap {
      case res if res.status == 200 => fuccess(read(res.body))
      case res                      => fufail(s"$url ${res.status} ${res.body}")
    }
  private def HTTP(url: String, data: JsObject): Funit = HTTP(url, data, _ => ())

  private val logger = play.api.Logger("ESClientHttp")
}

final class ESClientStub extends ESClient {
  def search(query: Query, from: From, size: Size) = fuccess(SearchResponse(Nil))
  def count(query: Query) = fuccess(CountResponse(0))
  def store(id: Id, doc: JsObject) = funit
  def storeBulk(docs: Seq[(Id, JsObject)]) = funit
  def deleteById(id: Id) = funit
  def deleteByQuery(query: StringQuery) = funit
  def putMapping = funit
}
