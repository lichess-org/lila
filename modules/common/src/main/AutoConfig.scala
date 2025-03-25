package lila.common
package autoconfig

import com.typesafe.config.*
import play.api.ConfigLoader

import scala.quoted.*

// Copied from https://github.com/keynmol/autoconfig-lichess-play-derivation/blob/main/macros.scala
// Thanks velvetbaldmime :)

/** Rename a property of an object to a different configuration key
  * @param name
  *   the name of the configuration key
  */

given [A](using loader: ConfigLoader[A]): ConfigLoader[Seq[A]] =
  ConfigLoader.seqConfigLoader.map(_.map { loader.load(_, "") })

given [A: ConfigLoader]: ConfigLoader[List[A]] =
  summon[ConfigLoader[Seq[A]]].map(_.toList)

given [A, B](using bts: SameRuntime[A, B], loader: ConfigLoader[A]): ConfigLoader[B] =
  loader.map(bts.apply)

def optionalConfig[A](using valueLoader: ConfigLoader[A]): ConfigLoader[Option[A]] = (config, path) =>
  if !config.hasPath(path) || config.getIsNull(path) then None
  else Some(valueLoader.load(config, path))

final case class ConfigName(name: String) extends scala.annotation.StaticAnnotation:
  assert(name != null && name.nonEmpty)

object AutoConfig:

  inline def loader[T] = ${ impl[T] }

  private def impl[T](using Quotes)(using tpe: Type[T]) =
    import quotes.reflect.*

    // get class symbol and ensure it's indeed a class
    val cls = TypeRepr
      .of[T]
      .classSymbol
      .getOrElse(report.errorAndAbort(s"${Type.show[T]} must be a class"))

    def build(confTerm: Expr[Config]) =
      val argss: List[List[Expr[Any]]] = cls.primaryConstructor.paramSymss
        .filter(paramList => paramList.forall(!_.isTypeParam))
        .map { params =>
          params.map { param =>

            // parameter name
            val paramName = Expr(param.name)

            // see if the name needs to be overridden using ConfigName annotation
            val nameOverride = param.annotations
              .collectFirst:
                case a if a.tpe.derivesFrom(TypeRepr.of[ConfigName].typeSymbol) =>
                  val annot = a.asExprOf[ConfigName]
                  annot
              .map(ac => '{ $ac.name })
              .getOrElse(paramName)

            // Get the type of this class member
            TypeRepr.of[T].memberType(param).asType match
              case '[Option[t]] =>
                Expr.summon[ConfigLoader[t]] match
                  case None =>
                    report.errorAndAbort(
                      s"Could not find an instance of ConfigLoader for type ${TypeRepr.of[t].show}"
                    )
                  case Some(value) =>
                    '{
                      optionalConfig[t](using $value).load($confTerm, $nameOverride)
                    }
              case '[t] =>
                // summon ConfigLoader for the type of this parameter
                Expr.summon[ConfigLoader[t]] match
                  case None =>
                    // poop if we couldn't
                    report.errorAndAbort(
                      s"Could not find an instance of ConfigLoader for type ${TypeRepr.of[t].show}"
                    )
                  case Some(value) =>
                    // create an expression that invoked the found
                    // instance of ConfigLoader
                    '{
                      $value.load($confTerm, $nameOverride)
                    }
            end match

          }
        }

      // Construct an AST the represent the `new T(params..)(params...)`
      New(TypeTree.of[T])
        .select(cls.primaryConstructor)
        .appliedToArgss(argss.map(_.map(_.asTerm)))
        .asExprOf[T]
    end build

    // We are creating an AST for a lambda like this: (config: Config) => new T(...)
    // That's the only way I could figure it out given that `config` is a runtime parameter
    val buildConfig = Lambda(
      owner = quotes.reflect.Symbol.spliceOwner,
      tpe = MethodType(List("config"))(
        paramInfosExp = _ => List(TypeRepr.of[Config]),
        resultTypeExp = _ => TypeRepr.of[T]
      ),
      rhsFn = (sym: quotes.reflect.Symbol, paramRefs: List[Tree]) =>
        val x = paramRefs.head.asExprOf[Config]

        build(x).asTerm
    ).asExprOf[Config => T]

    // final definition of config loader
    '{
      new ConfigLoader[T]:
        def load(config: Config, path: String): T =
          val conf = if path.isEmpty then config else config.getConfig(path)
          // we're invoking the lambda we manually constructed above
          $buildConfig(conf)
    }
  end impl

end AutoConfig
