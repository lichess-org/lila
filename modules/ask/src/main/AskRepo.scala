package lila.ask

import reactivemongo.api.bson.*

import lila.db.dsl.{ *, given }
import lila.memo.CacheApi.*
import lila.ask.AskApi
import lila.core.id.AskId
import lila.core.ask.*
import lila.core.ask.Ask.*

final class AskRepo(askDb: lila.db.AsyncColl, cacheApi: lila.memo.CacheApi)(using Executor, Scheduler)
    extends lila.core.ask.AskRepo:

  given BSONDocumentHandler[Ask] = Macros.handler[Ask]

  private val cache = cacheApi[AskId, Option[Ask]](1000, "ask") { builder =>
    builder.expireAfterAccess(1.hour).buildAsyncTimeout() { askId =>
      askDb { coll => coll.byId[Ask](askId) }
    }
  }
  private def cachePut(ask: Ask): Unit = cache.put(ask._id, fuccess(ask.some))
  private def cacheRemove(askId: AskId): Unit = cache.put(askId, fuccess(none))
  private def cacheInvalidate(askId: AskId): Unit = cache.underlying.synchronous.invalidate(askId)

  def getAsync(askId: AskId): Fu[Option[Ask]] = cache.get(askId)

  def setPicks(askId: AskId, voterId: String, picks: Option[Vector[Int]]): Fu[Option[Ask]] =
    update(askId, voterId, picks, modifyPicksCached, writePicks)

  def setForm(askId: AskId, voterId: String, form: Option[String]): Fu[Option[Ask]] =
    update(askId, voterId, form, modifyFormCached, writeForm)

  def unset(askId: AskId, voterId: String): Fu[Option[Ask]] =
    update(askId, voterId, none[Unit], unsetCached, writeUnset)

  def delete(askId: AskId): Funit = askDb: coll =>
    coll.delete.one($id(askId)).void.addEffect(_ => cacheRemove(askId))

  def conclude(askId: AskId): Fu[Option[Ask]] = askDb: coll =>
    coll
      .findAndUpdateSimplified[Ask]($id(askId), $addToSet("tags" -> "concluded"), fetchNewObject = true)
      .collect:
        case Some(ask) =>
          cachePut(ask)
          /*if ask.url.nonEmpty && !ask.isAnon then
            timeline ! Propagate(AskConcluded(ask.creator, ask.question, ~ask.url))
              .toUsers(ask.participants.map(UserId(_)).toList)
              .exceptUser(ask.creator)*/
          ask.some

  def reset(askId: AskId): Fu[Option[Ask]] = askDb: coll =>
    coll
      .findAndUpdateSimplified[Ask](
        $id(askId),
        $doc($unset("picks", "form"), $pull("tags" -> "concluded")),
        fetchNewObject = true
      )
      .collect:
        case Some(ask) =>
          cachePut(ask)
          ask.some

  def byUser(userId: UserId): Fu[List[Ask]] = askDb: coll =>
    coll
      .find($doc("creator" -> userId))
      .sort($sort.desc("createdAt"))
      .cursor[Ask]()
      .list(Int.MaxValue)
      .map: asks =>
        asks.foreach(cachePut)
        asks

  def byUrl(url: String): Fu[List[Ask]] = askDb: coll =>
    coll
      .find($doc("url" -> url))
      .sort($sort.asc("createdAt"))
      .cursor[Ask]()
      .list(Int.MaxValue)
      .map: asks =>
        asks.foreach(cachePut)
        asks

  def deleteAll(encoded: String): Funit = askDb: coll =>
    val ids = AskApi.extractIds(encoded)
    if ids.nonEmpty then coll.delete.one($inIds(ids)).void.addEffect(_ => ids.foreach(cacheRemove))
    else funit

  // none values in these lists are still important for render sequencing
  def asksIn(encoded: String*): Fu[List[Option[Ask]]] =
    val ids = encoded.toList.flatMap(AskApi.extractIds)
    ids.map(getAsync).parallel

  def isOpen(askId: AskId): Fu[Boolean] =
    getAsync(askId).map(_.exists(_.isOpen))

  def setUrl(encoded: String, url: Option[String]): Funit = askDb: coll =>
    if !AskApi.hasAskId(encoded) then funit
    else
      val selector = $inIds(AskApi.extractIds(encoded))
      coll.update.one(selector, $set("url" -> url), multi = true) >>
        coll.list(selector).map(_.foreach(ask => cachePut(ask.copy(url = url))))

  private val emptyPicks = Map.empty[String, Vector[Int]]
  private val emptyForm = Map.empty[String, String]

  private def update[A](
      askId: AskId,
      voterId: String,
      value: Option[A],
      cached: (Ask, String, Option[A]) => Ask,
      writeField: (AskId, String, Option[A], Boolean) => Fu[Option[Ask]]
  ) =
    getAsync(askId).flatMap:
      case Some(ask) =>
        val cachedAsk = cached(ask, voterId, value)
        cachePut(cachedAsk)
        writeField(askId, voterId, value, false)
          .inject(cachedAsk.some)
          .recoverWith:
            case error =>
              cacheInvalidate(askId)
              fufail(error)
      case _ =>
        writeField(askId, voterId, value, true).map(_.map: ask =>
          cachePut(ask)
          ask)

  private def modifyPicksCached(ask: Ask, voterId: String, newPicks: Option[Vector[Int]]) =
    ask.copy(picks = newPicks.fold(ask.picks.fold(emptyPicks)(_ - voterId).some): p =>
      ((ask.picks.getOrElse(emptyPicks) + (voterId -> p)).some))

  private def modifyFormCached(ask: Ask, voterId: String, newForm: Option[String]) =
    ask.copy(form = newForm.fold(ask.form.fold(emptyForm)(_ - voterId).some): f =>
      ((ask.form.getOrElse(emptyForm) + (voterId -> f)).some))

  private def unsetCached(ask: Ask, voterId: String, unused: Option[Unit]) =
    unused.void
    ask.copy(
      picks = ask.picks.fold(emptyPicks)(_ - voterId).some,
      form = ask.form.fold(emptyForm)(_ - voterId).some
    )

  private def writePicks(askId: AskId, voterId: String, picks: Option[Vector[Int]], fetchNew: Boolean) =
    updateAsk(askId, picks.fold($unset(s"picks.$voterId"))(r => $set(s"picks.$voterId" -> r)), fetchNew)

  private def writeForm(askId: AskId, voterId: String, form: Option[String], fetchNew: Boolean) =
    updateAsk(askId, form.fold($unset(s"form.$voterId"))(f => $set(s"form.$voterId" -> f)), fetchNew)

  private def writeUnset(askId: AskId, voterId: String, unused: Option[Unit], fetchNew: Boolean) =
    unused.void
    updateAsk(askId, $unset(s"picks.$voterId", s"form.$voterId"), fetchNew)

  private def updateAsk(askId: AskId, update: BSONDocument, fetchNew: Boolean) = askDb: coll =>
    coll.update
      .one($and($id(askId), $doc("tags" -> $ne("concluded"))), update)
      .flatMap:
        case _ => if fetchNew then coll.byId[Ask](askId) else fuccess(none[Ask])

  // only preserve votes if important fields haven't been altered
  private[ask] def upsert(ask: Ask): Fu[Ask] = askDb: coll =>
    coll
      .byId[Ask](ask._id)
      .flatMap:
        case Some(dbAsk) =>
          val mergedAsk = ask.merge(dbAsk)
          if dbAsk eq mergedAsk then fuccess(mergedAsk).addEffect(cachePut)
          else coll.update.one($id(ask._id), mergedAsk).inject(mergedAsk).addEffect(cachePut)
        case _ =>
          coll.insert.one(ask).inject(ask).addEffect(cachePut)
