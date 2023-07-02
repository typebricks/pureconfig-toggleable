package typebricks.pureconfig

import com.typesafe.config.ConfigException
import com.typesafe.config.ConfigObject
import com.typesafe.config.ConfigValue
import com.typesafe.config.ConfigValueFactory
import com.typesafe.config.ConfigValueType
import pureconfig.ConfigWriter

package object toggleable:

  opaque type FlagIs[K <: String, B <: Boolean] = (K, B)

  final type ToggleableByFlag[+A, K <: String] = A EnabledWhen (K FlagIs true)
  final type Toggleable[+A] = A ToggleableByFlag "enabled"

  object unsafe:
    private def unsafeAsObject(cv: ConfigValue): ConfigObject =
      if cv.valueType() != ConfigValueType.OBJECT then {
        throw new ConfigException.WrongType(cv.origin(), "has to be written as ConfigObject for toggling")
      } else cv.asInstanceOf[ConfigObject]

    given configWriter[K <: String: ValueOf, V <: Boolean: ValueOf, A](using
      cw: ConfigWriter[A]
    ): ConfigWriter[EnabledWhen[A, K FlagIs V]] = ConfigWriter.fromFunction {
      case Enabled(a) =>
        val obj = unsafeAsObject(cw.to(a))
        if obj.containsKey(valueOf[K]) then
          throw new ConfigException.BadValue(obj.origin(), valueOf[K], "inner key conflicts with a toggling key")
        else obj.withValue(valueOf[K], ConfigValueFactory.fromAnyRef(valueOf[V]))
      case Disabled =>
        ConfigValueFactory.fromMap(java.util.Map.of(valueOf[K], !valueOf[V]))
    }
