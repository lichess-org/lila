package lila.swiss

import play.api.i18n.Lang

import lila.i18n.I18nKeys as trans
import lila.rating.PerfType
import lila.user.{ Title, User }

sealed trait SwissCondition:

  def name(perf: PerfType)(using lang: Lang): String

  def withVerdict(verdict: SwissCondition.Verdict) = SwissCondition.WithVerdict(this, verdict)

object SwissCondition:

  trait FlatCond:

    def apply(user: User, perf: PerfType): SwissCondition.Verdict

  type GetMaxRating   = PerfType => Fu[IntRating]
  type GetBannedUntil = UserId => Fu[Option[Instant]]

  enum Verdict(val accepted: Boolean, val reason: Option[Lang => String]):
    case Accepted                         extends Verdict(true, none)
    case Refused(because: Lang => String) extends Verdict(false, because.some)
    case RefusedUntil(until: Instant)     extends Verdict(false, none)
  export Verdict.*

  case class WithVerdict(condition: SwissCondition, verdict: Verdict)

  case object PlayYourGames extends SwissCondition:
    def name(perf: PerfType)(using lang: Lang) = "Play your games"
    def withBan(bannedUntil: Option[Instant]) = withVerdict {
      bannedUntil.fold[Verdict](Accepted)(RefusedUntil.apply)
    }

  case object Titled extends SwissCondition with FlatCond:
    def name(perf: PerfType)(using lang: Lang) = "Only titled players"
    def apply(user: User, perf: PerfType) =
      if (user.title.exists(_ != Title.LM)) Accepted
      else Refused(lang => name(perf)(using lang))

  case class NbRatedGame(nb: Int) extends SwissCondition with FlatCond:

    def apply(user: User, perf: PerfType) =
      if (user.hasTitle) Accepted
      else if (user.perfs(perf).nb >= nb) Accepted
      else
        Refused { lang =>
          given Lang  = lang
          val missing = nb - user.perfs(perf).nb
          trans.needNbMorePerfGames.pluralTxt(missing, missing, perf.trans)
        }

    def name(perf: PerfType)(using lang: Lang) =
      trans.moreThanNbPerfRatedGames.pluralTxt(nb, nb, perf.trans)

  case class MaxRating(rating: Int) extends SwissCondition:

    def apply(perf: PerfType, getMaxRating: GetMaxRating)(
        user: User
    )(using Executor): Fu[Verdict] =
      if (user.perfs(perf).provisional.yes) fuccess(Refused { lang =>
        given Lang = lang
        trans.yourPerfRatingIsProvisional.txt(perf.trans)
      })
      else if (user.perfs(perf).intRating > rating) fuccess(Refused { lang =>
        given Lang = lang
        trans.yourPerfRatingIsTooHigh.txt(perf.trans, user.perfs(perf).intRating)
      })
      else
        getMaxRating(perf) map {
          case r if r <= rating => Accepted
          case r =>
            Refused { lang =>
              given Lang = lang
              trans.yourTopWeeklyPerfRatingIsTooHigh.txt(perf.trans, r)
            }
        }

    def maybe(user: User, perf: PerfType): Boolean =
      user.perfs(perf).provisional.no && user.perfs(perf).intRating <= rating

    def name(perf: PerfType)(using lang: Lang) = trans.ratedLessThanInPerf.txt(rating, perf.trans)

  case class MinRating(rating: Int) extends SwissCondition with FlatCond:

    def apply(user: User, perf: PerfType) =
      if (user.perfs(perf).provisional.yes) Refused { lang =>
        given Lang = lang
        trans.yourPerfRatingIsProvisional.txt(perf.trans)
      }
      else if (user.perfs(perf).intRating < rating) Refused { lang =>
        given Lang = lang
        trans.yourPerfRatingIsTooLow.txt(perf.trans, user.perfs(perf).intRating)
      }
      else Accepted

    def name(perf: PerfType)(using lang: Lang) = trans.ratedMoreThanInPerf.txt(rating, perf.trans)

  case class AllowList(value: String) extends SwissCondition with FlatCond:

    private lazy val segments = value.linesIterator.map(_.trim.toLowerCase).toSet

    private def allowAnyTitledUser = segments contains "%titled"

    def apply(user: User, @annotation.nowarn perf: PerfType): SwissCondition.Verdict =
      if (segments contains user.id.value) Accepted
      else if (allowAnyTitledUser && user.hasTitle) Accepted
      else Refused { _ => "Your name is not in the tournament line-up." }

    def name(perf: PerfType)(using lang: Lang) = "Fixed line-up"

  case class All(
      nbRatedGame: Option[NbRatedGame],
      maxRating: Option[MaxRating],
      minRating: Option[MinRating],
      titled: Option[Titled.type],
      allowList: Option[AllowList],
      playYourGames: Boolean = true
  ):

    lazy val list: List[SwissCondition] =
      List(nbRatedGame, maxRating, minRating, titled, allowList, playYourGames option PlayYourGames).flatten

    def relevant = list.nonEmpty

    def ifNonEmpty = list.nonEmpty option this

    def withVerdicts(
        perf: PerfType,
        getMaxRating: GetMaxRating,
        getBannedUntil: GetBannedUntil
    )(user: User)(using Executor): Fu[All.WithVerdicts] =
      list.map {
        case PlayYourGames => getBannedUntil(user.id) map PlayYourGames.withBan
        case c: MaxRating  => c(perf, getMaxRating)(user) map c.withVerdict
        case c: FlatCond   => fuccess(c withVerdict c(user, perf))
      }.parallel dmap All.WithVerdicts.apply

    def accepted = All.WithVerdicts(list.map { WithVerdict(_, Accepted) })

    def sameMaxRating(other: All) = maxRating.map(_.rating) == other.maxRating.map(_.rating)
    def sameMinRating(other: All) = minRating.map(_.rating) == other.minRating.map(_.rating)
    def sameRatings(other: All)   = sameMaxRating(other) && sameMinRating(other)

    def similar(other: All) = sameRatings(other) && titled == other.titled

    def isRatingLimited = maxRating.isDefined || minRating.isDefined

  object All:
    val empty = All(
      nbRatedGame = none,
      maxRating = none,
      minRating = none,
      titled = none,
      allowList = none,
      playYourGames = false
    )

    case class WithVerdicts(list: List[WithVerdict]) extends AnyVal:
      def relevant = list.nonEmpty
      def accepted = list.forall(_.verdict.accepted)

  final class Verify(historyApi: lila.history.HistoryApi, banApi: SwissBanApi):

    def apply(swiss: Swiss, user: User)(using Executor): Fu[All.WithVerdicts] =
      val getBan: GetBannedUntil     = banApi.bannedUntil
      val getMaxRating: GetMaxRating = perf => historyApi.lastWeekTopRating(user, perf)
      swiss.settings.conditions.withVerdicts(swiss.perfType, getMaxRating, getBan)(user)

  object BSONHandlers:
    import reactivemongo.api.bson.*
    import lila.db.dsl.{ *, given }
    private given BSONDocumentHandler[NbRatedGame] = Macros.handler
    private given BSONDocumentHandler[MaxRating]   = Macros.handler
    private given BSONDocumentHandler[MinRating]   = Macros.handler
    private given BSONHandler[Titled.type] = quickHandler[Titled.type](
      { case _: BSONValue => Titled },
      _ => BSONBoolean(true)
    )
    private given BSONDocumentHandler[AllowList] = Macros.handler
    given BSONDocumentHandler[All]               = Macros.handler

  object DataForm:
    import play.api.data.Forms.*
    import lila.common.Form.*
    val perfAuto = "auto" -> "Auto"
    val perfKeys = "auto" :: PerfType.nonPuzzle.map(_.key)
    def perfChoices(using lang: Lang) =
      perfAuto :: PerfType.nonPuzzle.map { pt =>
        pt.key -> pt.trans
      }
    val nbRatedGames = Vector(0, 5, 10, 15, 20, 30, 40, 50, 75, 100, 150, 200)
    val nbRatedGameChoices = options(nbRatedGames, "%d rated game{s}") map {
      case (0, _) => (0, "No restriction")
      case x      => x
    }
    val nbRatedGame = mapping(
      "nb" -> number(min = 0, max = ~nbRatedGames.lastOption)
    )(NbRatedGame.apply)(_.nb.some)
    case class RatingSetup(rating: Option[Int]):
      def actualRating = rating.filter(r => r > 600 && r < 3000)
    val maxRatings =
      List(2200, 2100, 2000, 1900, 1800, 1700, 1600, 1500, 1400, 1300, 1200, 1100, 1000, 900, 800)
    val maxRatingChoices = ("", "No restriction") ::
      options(maxRatings, "Max rating of %d").toList.map { case (k, v) => k.toString -> v }
    val maxRating = mapping(
      "rating" -> optional(numberIn(maxRatings))
    )(RatingSetup.apply)(_.rating.some)
    val minRatings = List(1000, 1100, 1200, 1300, 1400, 1500, 1600, 1700, 1800, 1900, 2000, 2100, 2200, 2300,
      2400, 2500, 2600)
    val minRatingChoices = ("", "No restriction") ::
      options(minRatings, "Min rating of %d").toList.map { case (k, v) => k.toString -> v }
    val minRating = mapping(
      "rating" -> optional(numberIn(minRatings))
    )(RatingSetup.apply)(_.rating.some)
    def all =
      mapping(
        "nbRatedGame"   -> optional(nbRatedGame),
        "maxRating"     -> maxRating,
        "minRating"     -> minRating,
        "titled"        -> optional(boolean),
        "allowList"     -> optional(allowList),
        "playYourGames" -> optional(boolean)
      )(AllSetup.apply)(unapply)
        .verifying("Invalid ratings", _.validRatings)

    case class AllSetup(
        nbRatedGame: Option[NbRatedGame],
        maxRating: RatingSetup,
        minRating: RatingSetup,
        titled: Option[Boolean],
        allowList: Option[String],
        playYourGames: Option[Boolean]
    ):

      def validRatings =
        (minRating.actualRating, maxRating.actualRating) match
          case (Some(min), Some(max)) => min < max
          case _                      => true

      def all = All(
        nbRatedGame.filter(_.nb > 0),
        maxRating.actualRating map MaxRating.apply,
        minRating.actualRating map MinRating.apply,
        ~titled option Titled,
        allowList = allowList map AllowList.apply,
        playYourGames = ~playYourGames
      )
    object AllSetup:
      val default = AllSetup(
        nbRatedGame = none,
        maxRating = RatingSetup(none),
        minRating = RatingSetup(none),
        titled = none,
        allowList = none,
        playYourGames = true.some
      )
      def apply(all: All): AllSetup =
        AllSetup(
          nbRatedGame = all.nbRatedGame,
          maxRating = RatingSetup(all.maxRating.map(_.rating)),
          minRating = RatingSetup(all.minRating.map(_.rating)),
          titled = all.titled has Titled option true,
          allowList = all.allowList.map(_.value),
          playYourGames = all.playYourGames.some
        )
