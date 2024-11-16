# Tl Parser

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.github.qdsfdhvh/tl-parser/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.github.qdsfdhvh/tl-parser)

Generate [gram-js](https://github.com/gram-js/gramjs) define to kotlin code.

## How to use

### 1. Setup 

```gradle
plugins {
    id "io.github.qdsfdhvh.tl-generator" version "<LAST_VERSION>"
}
```

```kotlin
gramTl {
    packageName = "xx.xx.model"
    prefix = "TL_"
    tlSourceDir = "src/main/tl"
}
```

### 2. write Test.tl file in `src/main/tl`

```tl
user#d23c81a3 id:int first_name:string last_name:string = User;
```

### 3. Run `gradle generateGramTl`

```sh
./gradlew generateGramTl
```

Then will generated Test.kt code:

```kotlin
import kotlin.Boolean
import kotlin.Int
import kotlin.String
import kotlin.jvm.JvmField
import org.telegram.tgnet.AbstractSerializedData
import org.telegram.tgnet.TLObject

public data class TL_User internal constructor(
  @JvmField
  public var id: Int = 0,
  @JvmField
  public var first_name: String = "",
  @JvmField
  public var last_name: String = "",
) : TLObject() {
  override fun deserializeResponse(
    stream: AbstractSerializedData,
    `constructor`: Int,
    exception: Boolean,
  ): TLObject? = TL_User.TLdeserialize(stream, `constructor`, exception)

  override fun readParams(stream: AbstractSerializedData, exception: Boolean) {
    id = stream.readInt32(exception)
    first_name = stream.readString(exception)
    last_name = stream.readString(exception)
  }

  override fun serializeToStream(stream: AbstractSerializedData) {
    stream.writeInt32(`constructor`)
    stream.writeInt32(id)
    stream.writeString(first_name)
    stream.writeString(last_name)
  }

  public companion object {
    public val `constructor`: Int
      get() = 0xd23c81a3.toInt()

    public fun TLdeserialize(
      stream: AbstractSerializedData,
      `constructor`: Int,
      exception: Boolean,
    ): TL_User? {
      when (`constructor`) {
        TL_User.`constructor` -> {
          val result = TL_User()
          result.readParams(stream, exception)
          return result
        }
        else -> {
          if (exception) {
            error("can't parse magic ${`constructor`} in " + "TL_User")
          }
          return null
        }
      }
    }
  }
}
```
