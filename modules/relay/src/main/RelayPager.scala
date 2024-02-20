package lila.relay

import lila.common.config.MaxPerPage
import lila.common.paginator.{ AdapterLike, Paginator }
import lila.db.dsl.*
import lila.relay.RelayTour.WithLastRound
import lila.memo.CacheApi

final class RelayPager(
    tourRepo: RelayTourRepo,
    roundRepo: RelayRoundRepo,
    colls: RelayColls,
    cacheApi: CacheApi
)(using
    Executor
):

  import BSONHandlers.given

  private val maxPerPage = MaxPerPage(20)

  def byOwner(owner: UserId, page: Int): Fu[Paginator[RelayTour | WithLastRound]] = Paginator(
    adapter = new:
      def nbResults: Fu[Int] = tourRepo.countByOwner(owner)
      def slice(offset: Int, length: Int): Fu[List[RelayTour | WithLastRound]] =
        tourRepo.coll
          .aggregateList(length, _.sec): framework =>
            import framework.*
            Match(RelayTourRepo.selectors.ownerId(owner.id)) -> {
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

  def subscribedBy(userId: UserId, page: Int): Fu[Paginator[RelayTour | WithLastRound]] = Paginator(
    adapter = new:
      def nbResults: Fu[Int] = tourRepo.countBySubscriberId(userId)
      def slice(offset: Int, length: Int): Fu[List[WithLastRound]] =
        tourRepo.coll
          .aggregateList(length, _.sec): framework =>
            import framework.*
            Match(RelayTourRepo.selectors.subscriberId(userId)) -> {
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
          Match(RelayTourRepo.selectors.officialInactive) -> {
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
    Paginator(
      adapter = new:
        private val selector   = $text(query) ++ $doc("tier" $exists true)
        def nbResults: Fu[Int] = tourRepo.coll.countSel(selector)
        def slice(offset: Int, length: Int): Fu[List[WithLastRound]] =
          tourRepo.coll
            .aggregateList(length, _.sec): framework =>
              import framework.*
              Match(selector) -> {
                List(Sort(Descending("tier"), Descending("syncedAt"), Descending("createdAt"))) :::
                  aggregateRoundAndUnwind(framework) :::
                  List(Skip(offset), Limit(length))
              }
            .map(readToursWithRound)
      ,
      currentPage = page,
      maxPerPage = maxPerPage
    )

  private def aggregateRoundAndUnwind(framework: tourRepo.coll.AggregationFramework.type) =
    aggregateRound(framework) ::: List(framework.UnwindField("round"))

  private def aggregateRound(framework: tourRepo.coll.AggregationFramework.type) = List(
    framework.PipelineOperator(RelayListing.group.lookup(colls.group)),
    framework.Match(RelayListing.group.filter),
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
