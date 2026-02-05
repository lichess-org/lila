package lila.relay

import scalalib.paginator.{ AdapterLike, Paginator }

import lila.db.dsl.{ *, given }
import lila.memo.CacheApi
import lila.relay.RelayTour.WithLastRound

import reactivemongo.api.bson.*
import lila.memo.CacheApi.buildAsyncTimeout

final class RelayPager(
    tourRepo: RelayTourRepo,
    colls: RelayColls,
    cacheApi: CacheApi
)(using Executor, Scheduler):

  import BSONHandlers.given
  import RelayTourRepo.{ selectors, readToursWithRoundAndGroup, unsetHeavyOptionalFields }

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
                List(Project(unsetHeavyOptionalFields), Sort(Descending("createdAt"))) :::
                  tourRepo.aggregateRound(
                    colls,
                    framework,
                    onlyKeepGroupFirst = false,
                    roundPipeline = roundPipelineFirstUnfinished.some
                  ) :::
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
              List(Project(unsetHeavyOptionalFields), Sort(Descending("createdAt"))) ::: tourRepo
                .aggregateRoundAndUnwind(colls, framework) ::: List(
                Skip(offset),
                Limit(length)
              )
            }
          .map(readToursWithRoundAndGroup(RelayTour.WithLastRound.apply))
    ,
    currentPage = page,
    maxPerPage = maxPerPage
  )

  def subscribedBy(userId: UserId, page: Int): Fu[Paginator[RelayTour | WithLastRound]] = Paginator(
    adapter = new:
      def nbResults: Fu[Int] = tourRepo.countBySubscriberId(userId)
      def slice(offset: Int, length: Int): Fu[List[RelayTour | WithLastRound]] =
        tourRepo.coll
          .aggregateList(length, _.sec): framework =>
            import framework.*
            Match(selectors.subscriberId(userId)) -> {
              List(Project(unsetHeavyOptionalFields), Sort(Descending("createdAt"))) ::: tourRepo
                .aggregateRound(
                  colls,
                  framework,
                  onlyKeepGroupFirst = false,
                  roundPipeline = roundPipelineFirstUnfinished.some
                ) ::: List(
                Skip(offset),
                Limit(length)
              )
            }
          .map(readTours)
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
            List(Project(unsetHeavyOptionalFields), Sort(Descending("syncedAt"))) :::
              tourRepo.aggregateRoundAndUnwind(colls, framework) :::
              List(Skip(offset), Limit(length))
          }
        .map(readToursWithRoundAndGroup(RelayTour.WithLastRound.apply))

    private val firstPageCache = cacheApi.unit[List[WithLastRound]]:
      _.refreshAfterWrite(3.seconds).buildAsyncTimeout(): _ =>
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

    val (textSearch, nameFilter) = query match
      case RelayPager.yearRegex(pre, year, post) =>
        val remaining = s"$pre $post".trim
        (if remaining.isEmpty then query else remaining, $doc("name".$regex(s"\\b$year\\b")))
      case q => (q, $empty)

    // We add quotes to the query to perform an exact match even when the query contains whitespaces
    val textSelector = $text(s"\"$textSearch\"") ++ nameFilter ++ selectors.officialPublic

    forSelector(
      selector = textSelector,
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

  // select the first round of the tour, that is not yet finished
  private val roundPipelineFirstUnfinished = List(
    $doc("$sort" -> RelayRoundRepo.sort.asc),
    $doc("$match" -> $doc("finishedAt".$exists(false))),
    $doc("$limit" -> 1)
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
        def nbResults: Fu[Int] = tourRepo.coll.secondary.countSel(selector)
        def slice(offset: Int, length: Int): Fu[List[WithLastRound]] =
          tourRepo.coll
            .aggregateList(length, _.sec): framework =>
              import framework.*
              Match(selector) -> {
                List(Project(unsetHeavyOptionalFields)) :::
                  addFields.map(AddFields(_)).toList :::
                  List(Sort(sortFields.map(Descending(_))*)) :::
                  tourRepo.aggregateRoundAndUnwind(colls, framework, onlyKeepGroupFirst) :::
                  List(Skip(offset), Limit(length))
              }
            .map(readToursWithRoundAndGroup(RelayTour.WithLastRound.apply))
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

private object RelayPager:
  val yearRegex = """(.*)\b(20\d{2})\b(.*)""".r
