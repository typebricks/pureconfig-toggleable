package typebricks.pureconfig

import com.typesafe.config.ConfigException
import com.typesafe.config.ConfigObject
import com.typesafe.config.ConfigValue
import com.typesafe.config.ConfigValueFactory
import example.Foo
import org.scalatest.*
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.wordspec.*
import pureconfig.ConfigReader
import pureconfig.ConfigWriter
import pureconfig.error.ConfigReaderFailures
import pureconfig.error.ConvertFailure
import pureconfig.error.FailureReason
import pureconfig.error.KeyNotFound
import pureconfig.error.WrongType
import typebricks.pureconfig.toggleable.Disabled
import typebricks.pureconfig.toggleable.Enabled
import typebricks.pureconfig.toggleable.EnabledWhen
import typebricks.pureconfig.toggleable.FlagIs
import typebricks.pureconfig.toggleable.unsafe.given

import scala.jdk.javaapi.CollectionConverters.*

import matchers.*

class ToggleableSpec extends AnyWordSpec, must.Matchers:

  val instance: Foo = Foo(bar = 42, baz = "meme", bool = true)
  val instanceConfig: ConfigObject = instance.toConfigObject

  type CustomEnabled = Foo EnabledWhen ("custom-activation-flag", true)
  type CustomDisabled = Foo EnabledWhen ("custom-disabled-flag", false)

  def mkObject(pairs: (String, Any)*): ConfigObject = ConfigValueFactory.fromMap(asJava(Map(pairs: _*)))

  def decode[A](cv: ConfigValue)(using r: ConfigReader[A]): ConfigReader.Result[A] = r.from(cv)

  object Failed:
    def unapply(result: ConfigReader.Result[Any]): Option[FailureReason] = result match
      case Left(ConfigReaderFailures(ConvertFailure(fr, _, _), _*)) => Some(fr)
      case _                                                        => None

  private def testFlagIs[K <: String: ValueOf, B <: Boolean: ValueOf](name: String): Unit = {
    val read = ConfigReader[Foo EnabledWhen (K FlagIs B)]
    s"$name ConfigReader" should {

      "fail to read when it's not a config object" in {
        read.from(ConfigValueFactory.fromAnyRef("string")) must matchPattern { case Failed(w: WrongType) => () }
      }

      "require a presence of toggling flag" in {
        read.from(instanceConfig) must matchPattern { case Failed(KeyNotFound(key, _)) if key == valueOf[K] => () }
      }
      "not attempt to decode a toggled section when it's disabled" in {
        read.from(mkObject(valueOf[K] -> !valueOf[B])) must matchPattern { case Right(Disabled) => () }
      }
      "not hide section decoding errors" in {
        read.from(mkObject(valueOf[K] -> valueOf[B])) must matchPattern { case Failed(KeyNotFound(_, _)) => () }
      }
      "properly decode an enabled section" in {
        read.from(instanceConfig.withValue(valueOf[K], ConfigValueFactory.fromAnyRef(valueOf[B]))) must matchPattern {
          case Right(Enabled(`instance`)) => ()
        }
      }
    }
    s"$name - unsafe ConfigWriter" can {
      "support only objects" in {
        assertThrows[ConfigException.WrongType] {
          ConfigWriter[String EnabledWhen (K FlagIs B)].to(Enabled("string"))
        }
      }
      val write = ConfigWriter[Foo EnabledWhen (K FlagIs B)]
      "write only a flag value for Disabled" in {
        write.to(Disabled) must be(mkObject(valueOf[K] -> !valueOf[B]))
      }
      "write both flag and object section for Enabled" in {
        write.to(Enabled(instance)) must be(
          instanceConfig.withValue(valueOf[K], ConfigValueFactory.fromAnyRef(valueOf[B]))
        )
      }
    }
  }

  "Given a ConfigReader[A] a ConfigReader[Toggleable[A]]" should {
    "pop out toggle flag out of config section overlapping is impossible" in {
      ConfigReader[Foo EnabledWhen ("bool" FlagIs true)].from(instanceConfig) must matchPattern {
        case Failed(KeyNotFound("bool", _)) => ()
      }
    }
  }

  testFlagIs["enabled", true]("default `Toggleable` type")
  testFlagIs["disabled", false]("A reverse - disabled flag")
  testFlagIs["custom-activation-flag", true]("""EnabledWhen ("custom-activation-flag" FlagIs true)""")
  testFlagIs["custom-disabling-flag", false]("""EnabledWhen ("custom-disabling-flag" FlagIs false)""")

  "ConfigWriter" should {
    "fail instead of overwriting a conflicting key" in {
      util.Try(ConfigWriter[Foo EnabledWhen ("bool" FlagIs true)].to(Enabled(instance))).toEither must matchPattern {
        case Left(e: ConfigException.BadValue) if e.getMessage.contains("inner key conflicts with a toggling key") =>
          ()
      }
    }
  }
