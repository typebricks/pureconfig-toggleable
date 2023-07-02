package example

import com.typesafe.config.ConfigObject
import com.typesafe.config.ConfigValueFactory
import pureconfig.ConfigReader
import pureconfig.ConfigWriter
import pureconfig.generic.derivation.default.derived

case class Foo(bar: Int, baz: String, bool: Boolean) derives ConfigReader:
  def toConfigObject: ConfigObject = ConfigValueFactory.fromMap(java.util.Map.of("bar", bar, "baz", baz, "bool", bool))

object Foo:
  given ConfigWriter[Foo] = ConfigWriter.fromFunction(_.toConfigObject)
