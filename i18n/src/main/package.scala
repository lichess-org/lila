package lila

package object i18n extends PackageObject with WithPlay {

  object tube {

    private[i18n] implicit lazy val translationTube = 
      Translation.tube inColl Env.current.translationColl
  }
}
