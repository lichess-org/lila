package lidraughts

package object analyse extends PackageObject {

  type InfoAdvice = (Info, Option[Advice])
  type InfoAdvices = List[InfoAdvice]

  type PdnMove = String
}
