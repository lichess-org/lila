package lila.relay

import scalalib.paginator.{ AdapterLike, Paginator }

import lila.db.dsl.{ *, given }
import lila.memo.CacheApi
import lila.relay.RelayTour.WithLastRound

import reactivemongo.api.bson.*

final class RelayPager(
    tourRepo: RelayTourRepo,
    colls: RelayColls,
    cacheApi: CacheApi
)(using Executor):

  import BSONHandlers.given
  import RelayTourRepo.{ selectors, readToursWithRound }

  private val maxPerPage = MaxPerPage(24)

  def byOwner(owner: UserId, page: Int)(using me: Option[MyId]): Fu[Paginator[RelayTour | WithLastRound]] =
    val isMe = me.exists(_.is(owner))
    Paginator(
      adapter = new:
        def nbResults: Fu[Int] = tourRepo.countByOwner(owner, publicOnly = !isMe)
        def slice(offset: Int, length: Int): Fu[List[RelayTour | WithLastRound]] =
          tourRepo.coll
            .aggregateList(length, _.sec): framework =>
              import framework.*
              Match(selectors.ownerId(owner.id) ++ isMe.not.so(selectors.vis.public)) -> {
                List(Sort(Descending("createdAt"))) :::
                  tourRepo.aggregateRound(colls, framework, onlyKeepGroupFirst = false) :::
                  List(Skip(offset), Limit(length))
              }
            .map(readTours)
      ,
      currentPage = page,
      maxPerPage = maxPerPage
    )

  def allPrivate(page: Int): Fu[Paginator[RelayTour | WithLastRound]] = Paginator(
    adapter = new:
      def nbResults: Fu[Int] = fuccess(9999)
      def slice(offset: Int, length: Int): Fu[List[WithLastRound]] =
        tourRepo.coll
          .aggregateList(length, _.sec): framework =>
            import framework.*
            Match(selectors.officialNotPublic) -> {
              List(Sort(Descending("createdAt"))) ::: tourRepo
                .aggregateRoundAndUnwind(colls, framework) ::: List(
                Skip(offset),
                Limit(length)
              )
            }
          .map(readToursWithRound(RelayTour.WithLastRound.apply))
    ,
    currentPage = page,
    maxPerPage = maxPerPage
  )

  def subscribedBy(userId: UserId, page: Int): Fu[Paginator[RelayTour | WithLastRound]] = Paginator(
    adapter = new:
      def nbResults: Fu[Int] = tourRepo.countBySubscriberId(userId)
      def slice(offset: Int, length: Int): Fu[List[WithLastRound]] =
        tourRepo.coll
          .aggregateList(length, _.sec): framework =>
            import framework.*
            Match(selectors.subscriberId(userId)) -> {
              List(Sort(Descending("createdAt"))) ::: tourRepo
                .aggregateRoundAndUnwind(colls, framework) ::: List(
                Skip(offset),
                Limit(length)
              )
            }
          .map(readToursWithRound(RelayTour.WithLastRound.apply))
    ,
    currentPage = page,
    maxPerPage = maxPerPage
  )

  object inactive:

    private def slice(offset: Int, length: Int): Fu[List[WithLastRound]] =
      tourRepo.coll
        .aggregateList(length, _.sec): framework =>
          import framework.*
          Match(selectors.officialInactive) -> {
            List(Sort(Descending("syncedAt"))) ::: tourRepo.aggregateRoundAndUnwind(
              colls,
              framework
            ) ::: List(
              Skip(offset),
              Limit(length)
            )
          }
        .map(readToursWithRound(RelayTour.WithLastRound.apply))

    private val firstPageCache = cacheApi.unit[List[WithLastRound]]:
      _.refreshAfterWrite(3.seconds).buildAsyncFuture: _ =>
        slice(0, maxPerPage.value)

    def apply(page: Int): Fu[Paginator[WithLastRound]] =
      Paginator(
        adapter = new:
          def nbResults: Fu[Int] = fuccess(9999)
          def slice(offset: Int, length: Int): Fu[List[WithLastRound]] =
            if offset == 0 then firstPageCache.get({})
            else inactive.slice(offset, length)
        ,
        currentPage = page,
        maxPerPage = maxPerPage
      )

    def firstPageResults(): Fu[List[WithLastRound]] = firstPageCache.get({})

  def search(query: String, page: Int): Fu[Paginator[WithLastRound]] =

    val day = 1000L * 3600 * 24
    // We add quotes to the query to perform an exact match even when the query contains whitespaces
    val exactQuery = s"\"$query\""

    val textSelector = $text(exactQuery) ++ selectors.officialPublic

    // Special case of querying so that users can filter broadcasts by year
    val yearOpt = """\b(20)\d{2}\b""".r.findFirstIn(query)
    val selector = yearOpt.foldLeft(textSelector): (sel, year) =>
      sel ++ "name".$regex(s"\\b$year\\b")

    forSelector(
      selector = selector,
      page = page,
      onlyKeepGroupFirst = false,
      addFields = $doc(
        "searchDate" -> $doc(
          "$add" -> $arr(
            $doc("$ifNull" -> $arr("$syncedAt", "$createdAt")),
            $doc("$multiply" -> $arr($doc("$add" -> $arr("$tier", -RelayTour.Tier.normal.v)), 60 * day)),
            $doc("$multiply" -> $arr($doc("$meta" -> "textScore"), 30 * day))
          )
        )
      ).some,
      sortFields = List("searchDate")
    )

  def byIds(ids: List[RelayTourId], page: Int): Fu[Paginator[WithLastRound]] =
    forSelector(
      $inIds(ids) ++ selectors.officialPublic,
      page = page,
      onlyKeepGroupFirst = false,
      sortFields = List("syncedAt")
    )

  private def forSelector(
      selector: Bdoc,
      page: Int,
      onlyKeepGroupFirst: Boolean,
      addFields: Option[Bdoc] = None,
      sortFields: List[String]
  ): Fu[Paginator[WithLastRound]] =
    Paginator(
      adapter = new:
        def nbResults: Fu[Int] = tourRepo.coll.countSel(selector)
        def slice(offset: Int, length: Int): Fu[List[WithLastRound]] =
          tourRepo.coll
            .aggregateList(length, _.sec): framework =>
              import framework.*
              Match(selector) -> {
                addFields.map(AddFields(_)).toList :::
                  List(Sort(sortFields.map(Descending(_))*)) :::
                  tourRepo.aggregateRoundAndUnwind(colls, framework, onlyKeepGroupFirst) :::
                  List(Skip(offset), Limit(length))
              }
            .map(readToursWithRound(RelayTour.WithLastRound.apply))
      ,
      currentPage = page,
      maxPerPage = maxPerPage
    )

  private def readTours(docs: List[Bdoc]): List[RelayTour | WithLastRound] = for
    doc <- docs
    tour <- doc.asOpt[RelayTour]
    rounds <- doc.getAsOpt[List[RelayRound]]("round")
    round = rounds.headOption
    group = RelayTourRepo.group.readFrom(doc)
  yield round.fold(tour)(WithLastRound(tour, _, group))
