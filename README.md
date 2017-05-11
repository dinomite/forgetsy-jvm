# forgetsy-jvm

A JVM port of [Forgetsy](https://github.com/cavvia/forgetsy) in Kotlin.  Heavy lifting (and persistence, if you care
about that) is done by Redis, so you'll need one of those.

# Synopsis

```kotlin
...
jedisPool = JedisPool(redisHost, redisPort)

val followsData = Delta(jedisPool, name, 7.days(), true)

followsData.increment("UserFoo", date = 14.daysAgo())
followsData.increment("UserBar", date = 10.daysAgo())
followsData.increment("UserBar", date = 7.daysAgo())
followsData.increment("UserFoo", date = 1.daysAgo())
followsData.increment("UserFoo")

println(followsData.fetch())
```

Outputs:

```{UserFoo=0.6666666666666666, UserBar=0.5}```

# Ruby

I wrote this as a straight port of [Forgetsy](https://github.com/cavvia/forgetsy), mirroring the API very closely and even much of the internals.  It should interop well (i.e. you should be ok using this and Forgetsy pointed at the same Redis) but I haven't tested that yet.

# Kotlin (and Java)

This library is written in Kotlin and I use it from Kotlin.  Kotlin has great Java interop…but this library makes use of
named arguments which Java doesn't support and likely makes the API as viewed from Java pretty janky.  If you want to
use this from Java provide a PR or just let me know and we can work on it together.

# See also

- [Forgetsy](https://github.com/cavvia/forgetsy)
- [bitly's post on Forget Table](http://word.bitly.com/post/41284219720/forget-table)
- [bitly's Forget Table source](https://github.com/bitly/forgettable)
