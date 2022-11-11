package lila.swiss

import reactivemongo.api.ReadPreference
import scala.concurrent.ExecutionContext
import com.softwaremill.tagging._

import lila.common.config
import lila.common.paginator.Paginator
import lila.db.dsl.{ *, given }
import lila.db.paginator.Adapter

final class SwissRoundPager(pairingColl: Coll @@ PairingColl)(using ec: ExecutionContext) {

  import BsonHandlers.given

  private val maxPerPage = config.MaxPerPage(50)

  def apply(swiss: Swiss, round: SwissRound.Number, page: Int): Fu[Paginator[SwissPairing]] =
    Paginator(
      adapter = new Adapter[SwissPairing](
        collection = pairingColl,
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
