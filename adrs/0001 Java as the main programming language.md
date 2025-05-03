# Java as the main programming language

## Status

Adopted

## Decision

The main programming language for this tool is Java.

## Context

I need to pick a programming language for this tool that I intend to run continuously in the background as a daemon.

## Consequences

I'll mostly feel like a fish in the water and will be able to focus on adding functionality over learning new stuff. And I'll have fun doing it. But I shouldn't expect this project to attract many users next to myself.   

## Options considered

1. **Java**
2. Ruby
3. Kotlin
4. Go
5. Swift
6. TypeScript

### Java

#### Pros

- I have more than 25 years of experience with Java. It's the language I am most proficient with, including its ecosystem. Even though I'm not a full-time programmer anymore, I've kept up with the JDK releases.
- I enjoy programming in Java with IntelliJ IDEA! I don't have a lot of free time on my hands, so I need a language I enjoy using.

#### Cons

- A JVM-language is probably not the best solution for a macOS desktop app, even if it is a daemon. People will have to install a JVM on their machine. I shouldn't expect many people to use or contribute to this project.
- The JVM requires a fair amount of memory. I think I can get it at an acceptable level.
- Startup time of a JVM is typically low. But since this is a daemon application, that's not a real con. Actually, the "warming up" the JVM does might even be a pro.

### Ruby

#### Pros

- Ruby is my second go-to language. I'm fairly proficient in it.
- I enjoy programming in Ruby a lot.

#### Cons

- Ruby is not typesafe. Duck typing is awesome, but I do find it hard to maintain moderately sized applications because of the lack of types.
- Ruby is not the best solution for a macOS desktop app, especially since the default one on Monterey - 2.6.8 - is quite old. On top of that we can expect Ruby not to be part of the OS at all anymore. 

### Kotlin

#### Pros

- Kotlin is like "a better Java". At work, more and more teams are using it in favor of Java.
- Whenever I get into trouble, I can always fall back on Java, both the language and the ecosystem. 

#### Cons

- I've read the book and have created some small programs with Kotlin, but I'm far from proficient in it. I can expect my progress to be slow.
- Kotlin is a JVM-language and thus requires a JVM. See the Java cons above.

### Go

#### Pros

- Go compiles to a single binary, for easy deployment. No runtimes needed.
- It's cross-platform, so I can deliver binaries for macOS, Windows and Linux easily.

#### Cons

- I'm not yet proficient in Go. I need to invest more time in learning the ecosystem to be productive in it. I've learned the hard way that learning a new language *and* creating a new tool is a recipe for disaster for me, in the limited time I have available. 

### Swift

#### Pros

- See Go.

#### Cons

- See Go.

### TypeScript

#### Pros

- It's the most popular language at the moment, for some reason I can't fathom. It has the highest chance of attracting contributors. 

#### Cons

- I do not much like TypeScript (and JavaScript for that matter). I've been using it for over 25 years, and I still can't get enthusiastic about it.