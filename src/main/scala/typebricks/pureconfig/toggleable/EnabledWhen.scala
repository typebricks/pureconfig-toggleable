package typebricks.pureconfig.toggleable

import pureconfig.ConfigReader

sealed trait EnabledWhen[+A, +P]:
  def toOption: Option[A] = this match
    case Enabled(value) => Some(value)
    case Disabled       => None

object EnabledWhen:

  given configReader[K <: String: ValueOf, V <: Boolean: ValueOf, A](using
    cr: ConfigReader[A]
  ): ConfigReader[EnabledWhen[A, K FlagIs V]] =
    ConfigReader.fromCursor(_.asObjectCursor.flatMap { sectionCursor =>
      sectionCursor
        .atKey(valueOf[K])
        .flatMap(
          _.asBoolean.flatMap(flagValue =>
            if flagValue == valueOf[V] then
              cr.from(sectionCursor.withoutKey(valueOf[K])).map(section => Enabled(section))
            else Right(Disabled)
          )
        )
    })

case class Enabled[+A](value: A) extends EnabledWhen[A, Nothing]

case object Disabled extends EnabledWhen[Nothing, Nothing]
