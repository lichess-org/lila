package lila.relay

import scalalib.paginator.{ AdapterLike, Paginator }

import lila.db.dsl.{ *, given }
import lila.memo.CacheApi
import lila.relay.RelayTour.WithLastRound

final class RelayPager(
    tourRepo: RelayTourRepo,
    roundRepo: RelayRoundRepo,
    colls: RelayColls,
    cacheApi: CacheApi
)(using
    Executor
):

  import BSONHandlers.given
  import RelayTourRepo.selectors

  private val maxPerPage = MaxPerPage(20)

  def byOwner(owner: UserId, page: Int)(using me: Option[MyId]): Fu[Paginator[RelayTour | WithLastRound]] =
    val isMe = me.exists(_.is(owner))
    Paginator(
      adapter = new:
        def nbResults: Fu[Int] = tourRepo.countByOwner(owner, publicOnly = !isMe)
        def slice(offset: Int, length: Int): Fu[List[RelayTour | WithLastRound]] =
          tourRepo.coll
            .aggregateList(length, _.sec): framework =>
              import framework.*
              Match(selectors.ownerId(owner.id) ++ (!isMe).so(selectors.publicTour)) -> {
                List(Sort(Descending("createdAt"))) ::: aggregateRound(framework) ::: List(
                  Skip(offset),
                  Limit(length)
                )
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
            Match(selectors.privateTour) -> {
              List(Sort(Descending("createdAt"))) ::: aggregateRoundAndUnwind(framework) ::: List(
                Skip(offset),
                Limit(length)
              )
            }
          .map(readToursWithRound)
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
              List(Sort(Descending("createdAt"))) ::: aggregateRoundAndUnwind(framework) ::: List(
                Skip(offset),
                Limit(length)
              )
            }
          .map(readToursWithRound)
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
            List(Sort(Descending("syncedAt"))) ::: aggregateRoundAndUnwind(framework) ::: List(
              Skip(offset),
              Limit(length)
            )
          }
        .map(readToursWithRound)

    private val firstPageCache = cacheApi.unit[List[RelayTour.WithLastRound]]:
      _.refreshAfterWrite(3 seconds).buildAsyncFuture: _ =>
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

  def search(query: String, page: Int): Fu[Paginator[WithLastRound]] =
    val day = 1000L * 3600 * 24
    forSelector(
      selector = $text(query) ++ selectors.officialPublic,
      page = page,
      onlyKeepGroupFirst = false,
      addFields = $doc(
        "searchDate" -> $doc(
          "$add" -> $arr(
            $doc("$ifNull"   -> $arr("$syncedAt", "$createdAt")),
            $doc("$multiply" -> $arr($doc("$add" -> $arr("$tier", -RelayTour.Tier.NORMAL)), 60 * day)),
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
      onlyKeepGroupFirst: Boolean = true,
      addFields: Option[Bdoc] = None,
      sortFields: List[String] = List("tier", "syncedAt", "createdAt")
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
                  aggregateRoundAndUnwind(framework, onlyKeepGroupFirst) :::
                  List(Skip(offset), Limit(length))
              }
            .map(readToursWithRound)
      ,
      currentPage = page,
      maxPerPage = maxPerPage
    )

  private def aggregateRoundAndUnwind(
      framework: tourRepo.coll.AggregationFramework.type,
      onlyKeepGroupFirst: Boolean = true
  ) =
    aggregateRound(framework, onlyKeepGroupFirst) ::: List(framework.UnwindField("round"))

  private def aggregateRound(
      framework: tourRepo.coll.AggregationFramework.type,
      onlyKeepGroupFirst: Boolean = true
  ) =
    onlyKeepGroupFirst.so(
      List(
        framework.PipelineOperator(RelayListing.group.firstLookup(colls.group)),
        framework.Match(RelayListing.group.firstFilter)
      )
    ) ::: List(
      framework.PipelineOperator(
        $lookup.pipeline(
          from = roundRepo.coll,
          as = "round",
          local = "_id",
          foreign = "tourId",
          pipe = List(
            $doc("$sort"      -> RelayRoundRepo.sort.start),
            $doc("$limit"     -> 1),
            $doc("$addFields" -> $doc("sync.log" -> $arr()))
          )
        )
      )
    )

  private def readToursWithRound(docs: List[Bdoc]): List[WithLastRound] = for
    doc   <- docs
    tour  <- doc.asOpt[RelayTour]
    round <- doc.getAsOpt[RelayRound]("round")
    group = RelayListing.group.readFrom(doc)
  yield WithLastRound(tour, round, group)

  private def readTours(docs: List[Bdoc]): List[RelayTour | WithLastRound] = for
    doc    <- docs
    tour   <- doc.asOpt[RelayTour]
    rounds <- doc.getAsOpt[List[RelayRound]]("round")
    round = rounds.headOption
    group = RelayListing.group.readFrom(doc)
  yield round.fold(tour)(WithLastRound(tour, _, group))
