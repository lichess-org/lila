package lila.swiss

import scalalib.paginator.Paginator

import lila.db.dsl.{ *, given }
import lila.db.paginator.Adapter

final class SwissRoundPager(mongo: SwissMongo)(using Executor):

  import BsonHandlers.given

  private val maxPerPage = MaxPerPage(50)

  def apply(swiss: Swiss, round: SwissRoundNumber, page: Int): Fu[Paginator[SwissPairing]] =
    Paginator(
      adapter = Adapter[SwissPairing](
        collection = mongo.pairing,
        selector = SwissPairing.fields { f =>
          $doc(f.swissId -> swiss.id, f.round -> round)
        },
        projection = none,
        sort = $empty,
        _.sec
      ),
      currentPage = page,
      maxPerPage = maxPerPage
    )
