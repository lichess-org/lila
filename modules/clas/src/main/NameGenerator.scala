package lila.clas

import play.api.i18n.Lang

final class NameGenerator(userRepo: lila.user.UserRepo)(using Executor):

  def apply(maxSize: Int = 17, triesLeft: Int = 20)(using lang: Lang): Fu[Option[UserName]] =
    scalalib.cuteName.CuteNameGenerator
      .make(maxSize, lang = lang.language)
      .map(UserName(_))
      .so: name =>
        userRepo
          .exists(name)
          .flatMap:
            case true if triesLeft > 0 => apply(maxSize, triesLeft - 1)
            case true => fuccess(none)
            case _ => fuccess(name.some)
