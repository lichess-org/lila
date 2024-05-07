package lila.ui

import play.api.mvc.{ PathBindable, QueryStringBindable }
import scalalib.newtypes.SameRuntime

import lila.core.id.*
import chess.variant.Variant

object LilaRouter:

  given opaquePathBindable[T, A](using
      sr: SameRuntime[A, T],
      rs: SameRuntime[T, A],
      bindable: PathBindable[A]
  ): PathBindable[T] =
    bindable.transform(sr.apply, rs.apply)

  given opaqueQueryStringBindable[T, A](using
      sr: SameRuntime[A, T],
      rs: SameRuntime[T, A],
      bindable: QueryStringBindable[A]
  ): QueryStringBindable[T] =
    bindable.transform(sr.apply, rs.apply)

  given PathBindable[UserStr] = new:
    def bind(key: String, value: String)    = UserStr.read(value).toRight("Invalid Lichess username")
    def unbind(key: String, value: UserStr) = value.value

  given PathBindable[PerfKey] = new:
    def bind(key: String, value: String)    = PerfKey(value).toRight("Invalid Lichess performance key")
    def unbind(key: String, value: PerfKey) = value.value

  object conversions:
    given Conversion[GameId, String]                   = _.value
    given Conversion[GameFullId, String]               = _.value
    given Conversion[GameAnyId, String]                = _.value
    given Conversion[UserId, String]                   = _.value
    given Conversion[UserName, String]                 = _.value
    given Conversion[Option[UserName], Option[String]] = UserName.raw(_)
    // where a UserStr is accepted, we can pass a UserName or UserId
    given Conversion[UserName, UserStr]                                      = _.into(UserStr)
    given Conversion[UserId, UserStr]                                        = _.into(UserStr)
    given reportIdConv: Conversion[ReportId, String]                         = _.value
    given Conversion[UblogPostId, String]                                    = _.value
    given Conversion[lila.core.i18n.Language, String]                        = _.value
    given Conversion[StudyId, String]                                        = _.value
    given Conversion[StudyChapterId, String]                                 = _.value
    given Conversion[PuzzleId, String]                                       = _.value
    given Conversion[SimulId, String]                                        = _.value
    given Conversion[SwissId, String]                                        = _.value
    given Conversion[TourId, String]                                         = _.value
    given Conversion[TeamId, String]                                         = _.value
    given Conversion[RelayRoundId, String]                                   = _.value
    given Conversion[chess.opening.OpeningKey, String]                       = _.value
    given Conversion[chess.format.Uci, String]                               = _.uci
    given Conversion[Variant.LilaKey, String]                                = _.value
    given variantKeyOpt: Conversion[Option[Variant.LilaKey], Option[String]] = Variant.LilaKey.raw(_)
    given postIdConv: Conversion[ForumPostId, String]                        = _.value
    given Conversion[ForumCategId, String]                                   = _.value
    given Conversion[ForumTopicId, String]                                   = _.value
    given relayTourIdConv: Conversion[RelayTourId, String]                   = _.value
    given Conversion[chess.FideId, Int]                                      = _.value
    given challengeIdConv: Conversion[ChallengeId, String]                   = _.value
