package lila.search

import play.api.libs.json._

sealed trait ESClient {

  def search[Q: Writes](query: Q, from: From, size: Size): Fu[SearchResponse]

  def count[Q: Writes](query: Q): Fu[CountResponse]

  def store(id: Id, doc: JsObject): Funit

  def deleteById(id: Id): Funit

  def deleteByQuery(query: StringQuery): Funit
}

final class ESClientHttp(
    endpoint: String,
    val index: Index,
    writeable: Boolean) extends ESClient {
  import play.api.libs.ws.WS
  import play.api.Play.current

  def store(id: Id, doc: JsObject) = writeable ??
    HTTP(s"store/${index.name}/${id.value}", doc)

  def search[Q: Writes](query: Q, from: From, size: Size) =
    HTTP(s"search/${index.name}/${from.value}/${size.value}", query, SearchResponse.apply)

  def count[Q: Writes](query: Q) =
    HTTP(s"count/${index.name}", query, CountResponse.apply)

  def deleteById(id: lila.search.Id) = writeable ??
    HTTP(s"delete/id/${index.name}/${id.value}", Json.obj())

  def deleteByQuery(query: lila.search.StringQuery) = writeable ??
    HTTP(s"delete/query/${index.name}/${query.value}", Json.obj())

  def createTempIndex = {
    val tempIndex = Index(s"${index.name}_${ornicar.scalalib.Random.nextString(4)}")
    val tempClient = new ESClientHttpTemp(
      index,
      new ESClientHttp(endpoint, tempIndex, writeable))
    tempClient.putMapping inject tempClient
  }

  private[search] def HTTP[D: Writes, R](url: String, data: D, read: String => R): Fu[R] =
    WS.url(s"$endpoint/$url").post(Json toJson data) flatMap {
      case res if res.status == 200 => fuccess(read(res.body))
      case res                      => fufail(s"$url ${res.status}")
    }
  private[search] def HTTP(url: String, data: JsObject): Funit = HTTP(url, data, _ => ())

  private val logger = play.api.Logger("ESClientHttp")
}

final class ESClientHttpTemp(
    mainIndex: Index,
    client: ESClientHttp) {

  def putMapping =
    client.HTTP(s"mapping/${tempIndex.name}/${mainIndex.name}", Json.obj())

  def tempIndex = client.index

  def storeBulk(docs: Seq[(Id, JsObject)]) =
    client.HTTP(s"store/bulk/${tempIndex.name}/${mainIndex.name}", JsObject(docs map {
      case (Id(id), doc) => id -> JsString(Json.stringify(doc))
    }))

  def aliasBackToMain =
    client.HTTP(s"alias/${tempIndex.name}/${mainIndex.name}", Json.obj())

}

final class ESClientStub extends ESClient {
  def search[Q: Writes](query: Q, from: From, size: Size) = fuccess(SearchResponse(Nil))
  def count[Q: Writes](query: Q) = fuccess(CountResponse(0))
  def store(id: Id, doc: JsObject) = funit
  def storeBulk(docs: Seq[(Id, JsObject)]) = funit
  def deleteById(id: Id) = funit
  def deleteByQuery(query: StringQuery) = funit
  def putMapping = funit
}
