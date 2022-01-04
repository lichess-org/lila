package lila

package object analyse extends PackageObject {

  type InfoAdvice  = (Info, Option[Advice])
  type InfoAdvices = List[InfoAdvice]

}
