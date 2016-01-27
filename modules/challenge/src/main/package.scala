package lila

package object challenge extends PackageObject with WithPlay {

  type EitherChallenger = Either[Challenge.Anonymous, Challenge.Registered]
}
