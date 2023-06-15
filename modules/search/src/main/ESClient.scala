package lila.search

import play.api.libs.json.*
import play.api.libs.ws.*
import play.api.libs.ws.JsonBodyWritables.*

import lila.common.Json.given

sealed trait ESClient:

  def search[Q: Writes](query: Q, from: From, size: Size): Fu[SearchResponse]

  def count[Q: Writes](query: Q): Fu[CountResponse]

  def store(id: Id, doc: JsObject): Funit

  def deleteById(id: Id): Funit

  def deleteByIds(ids: List[Id]): Funit

  def refresh: Funit

final class ESClientHttp(
    ws: StandaloneWSClient,
    config: SearchConfig,
    val index: Index
)(using Executor)
    extends ESClient:

  def store(id: Id, doc: JsObject) =
    config.writeable so monitor("store") {
      HTTP(s"store/$index/${id.value}", doc)
    }

  def search[Q: Writes](query: Q, from: From, size: Size) =
    monitor("search") {
      HTTP(s"search/$index/${from.value}/${size.value}", query, SearchResponse.apply)
        .dmap(~_)
    }

  def count[Q: Writes](query: Q) =
    monitor("count") {
      HTTP(s"count/$index", query, CountResponse.apply)
        .dmap(~_)
    }

  def deleteById(id: lila.search.Id) =
    config.writeable so HTTP(s"delete/id/$index/${id.value}", Json.obj())

  def deleteByIds(ids: List[lila.search.Id]) =
    config.writeable so HTTP(s"delete/ids/$index", Json.obj("ids" -> ids))

  def putMapping =
    HTTP(s"mapping/$index", Json.obj())

  def storeBulk(docs: Seq[(Id, JsObject)]) =
    HTTP(
      s"store/bulk/$index",
      JsObject(docs map { case (id, doc) =>
        id.value -> JsString(Json.stringify(doc))
      })
    )

  def refresh =
    HTTP(s"refresh/$index", Json.obj())

  private[search] def HTTP[D: Writes, R](url: String, data: D, read: String => R): Fu[Option[R]] =
    ws.url(s"${config.endpoint}/$url").post(Json toJson data) flatMap {
      case res if res.status == 200 => fuccess(read(res.body).some)
      case res if res.status == 400 => fuccess(none)
      case res                      => fufail(s"$url ${res.status}")
    }
  private[search] def HTTP(url: String, data: JsObject): Funit = HTTP(url, data, _ => ()).void

  private def monitor[A](op: String)(f: Fu[A]) =
    f.monTry(res => _.search.time(op, index.value, res.isSuccess))

final class ESClientStub extends ESClient:
  def search[Q: Writes](query: Q, from: From, size: Size) = fuccess(SearchResponse(Nil))
  def count[Q: Writes](query: Q)                          = fuccess(CountResponse(0))
  def store(id: Id, doc: JsObject)                        = funit
  def storeBulk(docs: Seq[(Id, JsObject)])                = funit
  def deleteById(id: Id)                                  = funit
  def deleteByIds(ids: List[Id])                          = funit
  def putMapping                                          = funit
  def refresh                                             = funit
