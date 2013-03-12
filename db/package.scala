package lila

package object db extends PackageObject with WithPlay with WithDb {

  type WithStringId = { def id: String }
}
