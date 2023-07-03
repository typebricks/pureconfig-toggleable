# Pureconfig Toggleable

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.github.typebricks/pureconfig-toggleable_3/badge.svg)](https://search.maven.org/artifact/io.github.typebricks/pureconfig-toggleable_3/1.0.0/jar)

A tiny type brick for Scala 3 makes it possible to explicitly toggle config sections.

When a toggleable section is disabled, its content won't be decoded by `ConfigReader`

There's only two possible cases for `Toggleable[A]`
- `Enabled[A](value: A)`
- `Disabled`


one can naturally convert it to `Option[A]` using `.toOption` method

## Installation
```sbt
libraryDependencies += "io.github.typebricks" %% "pureconfig-toggleable" % "1.0.0"
```

## Code example

<details>
<summary>// assumed imports</summary>

```scala

import java.net.URI
import pureconfig.ConfigReader
import pureconfig.generic.derivation.default.derived
import scala.concurrent.duration.FiniteDuration
```

</details>

```scala

import typebricks.pureconfig.toggleable.Toggleable

/* A complex configurable thing */
case class ComplexThing(
  uri: URI,
  duration: FiniteDuration
) derives ConfigReader

case class MyAppConfig(
  somethingBasic: String,
  // the whole ComplexThing can be explicitly disabled now
  aComplexThing: Toggleable[ComplexThing],
) derives ConfigReader

```

In the example above, a `Toggleable` wrapper is used to make a complex section optional.
Let's see how disabled / enabled configurations may look like:

<details>
<summary>Enabled config</summary>

```hocon
something-basic = "top-level"
a-complex-thing = {
  
  enabled = true
  
  uri = "https://github.com/typebricks"
  duration = "15 days"
}

```
</details>


<details>
<summary>Disabled config</summary>

```hocon
something-basic = "top-level"
a-complex-thing {
  
  enabled = false
  
  // no valid keys are required now
  url = ""
}
```
</details>

### Note on using environment variables
Given a toggleable section, it's easy to use env variables for configuration.
However, a hocon substitution happens before config reading, so it's more practical to use `${?VARIABLE}` syntax over an invalid value. When a section is disabled, there's no need to make those keys valid.

```hocon
a-complex-thing {
  
  enabled = ${COMPLEX_THING_ENABLED}

  // a missing variable results to invalid value only when "enabled"
  url = "-!- missing COMPLEX_THING_URL env var -!-"
  url = ${?COMPLEX_THING_URL}
  
  duration = "-!- missing COMPLEX_THING_DURATION env var -!-"
  duration = ${?COMPLEX_THING_DURATION}
}
```

So a single environment variable `COMPLEX_THING_ENABLED=false` works as a minimum


## Custom flag name / value
`Toggleable[X]` requires `enabled` flag to be `true` for `X` to be configured, but this can be customized.

Let's customise our example such that `ComplexThing` is only enabled when 
`complex-thing-disabled` is `false`, so a config will look like
```hocon

a-complex-thing {
  complex-thing-disabled = false
  url = ...
  duration = ...
}

```

Luckily, `Toggleable[A]` is just an alias of `A EnabledWhen ("enabled" FlagIs true)`
(or `EnabledWhen[A, FlagIs["enabled", true]]` if written classically).
We can apply our own "toggling key" and "enabling value" literals:

```scala

import typebricks.pureconfig.toggleable.{EnabledWhen, FlagIs}

case class MyAppConfig(
  somethingBasic: String,
  // an explicit toggling condition with literal types
  aComplexThing: ComplexThing EnabledWhen ("complex-thing-disabled" FlagIs false),
) derives ConfigReader

```

## FAQ

### May a toggling flag interfere with another key for the section?

**No**. A flag key does not pass through when section is enabled. No ambiguity is possible.
`Toggleable[Map[String, Int]]` will **never** result in `Enabled(Map("enabled" -> 1))`
 

### Is there a `ConfigWriter`?
Yes, but it's not in default implicit scope because it's formally unsafe. Import it with
```scala
import typebricks.pureconfig.toggleable.unsafe.given
```
**Why this ConfigWriter is unsafe?**

Writing a config is designed as a pure operation, but `pureconfig-toggleable` only supports object output types (if not, `ConfigException.WrongType` will be thrown). It also ensures that flag name doesn't conflict with an inner section key - if it does, `ConfigException.BadValue` is raised.
