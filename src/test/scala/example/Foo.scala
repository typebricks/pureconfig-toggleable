package example

import com.typesafe.config.ConfigObject
import com.typesafe.config.ConfigValueFactory
import pureconfig.ConfigReader
import pureconfig.ConfigWriter
import pureconfig.generic.derivation.default.derived

import scala.jdk.CollectionConverters.*

case class Foo(bar: Int, baz: String, bool: Boolean) derives ConfigReader:
  def toConfigObject: ConfigObject =
    ConfigValueFactory.fromMap(Map[String, Any]("bar" -> bar, "baz" -> baz, "bool" -> bool).asJava)

object Foo:
  given ConfigWriter[Foo] = ConfigWriter.fromFunction(_.toConfigObject)
