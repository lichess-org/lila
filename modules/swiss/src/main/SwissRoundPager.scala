package lila.swiss

import reactivemongo.api.ReadPreference
import scala.concurrent.ExecutionContext

import lila.common.config
import lila.common.paginator.Paginator
import lila.db.dsl._
import lila.db.paginator.Adapter

final class SwissRoundPager(colls: SwissColls)(implicit ec: ExecutionContext) {

  import BsonHandlers._

  private val maxPerPage = config.MaxPerPage(50)

  def apply(swiss: Swiss, round: SwissRound.Number, page: Int): Fu[Paginator[SwissPairing]] =
    Paginator(
      adapter = new Adapter[SwissPairing](
        collection = colls.pairing,
        selector = SwissPairing.fields { f =>
          $doc(f.swissId -> swiss.id, f.round -> round)
        },
        projection = none,
        sort = $empty,
        readPreference = ReadPreference.secondaryPreferred
      ),
      currentPage = page,
      maxPerPage = maxPerPage
    )
}
