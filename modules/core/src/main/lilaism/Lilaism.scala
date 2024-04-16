package lila.core.lilaism

object Lilaism extends LilaLibraryExtensions:

  export lila.core.id.{
    GameId,
    ChatId,
    TeamId,
    Flair,
    StudyId,
    StudyChapterId,
    TourId,
    SimulId,
    SwissId,
    ForumPostId,
    UblogPostId,
    RoomId
  }
  export lila.core.userId.{ UserId, UserName, UserStr, MyId, UserIdOf }
  export lila.core.data.{ Markdown, Html, JsonStr }
  export lila.core.rating.data.{ IntRating, IntRatingDiff, RatingProvisional }
  export lila.core.perf.{ PerfKey, Perf }
  export lila.core.email.EmailAddress
  export lila.core.user.{ User, Me }
  export lila.core.game.{ Game, Pov }

  def some[A](a: A): Option[A] = Some(a)

  trait StringValue extends Any:
    def value: String
    override def toString = value
  given cats.Show[StringValue] = cats.Show.show(_.value)

  // move somewhere else when we have more Eqs
  given cats.Eq[play.api.i18n.Lang] = cats.Eq.fromUniversalEquals

  import play.api.Mode
  extension (mode: Mode)
    def isDev   = mode == Mode.Dev
    def isProd  = mode == Mode.Prod
    def notProd = mode != Mode.Prod
