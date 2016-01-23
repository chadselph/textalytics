# Textalytics

Run this project to do some analysis on your iOS texting/iMessage history.

Only works on Mac OS X with latest-ish iOS version.
Requires you to have a recent iPhone backup to your computer (not iCloud).

## What does it show you?

* Top 10 All Time Texting (phone number)
* Top 10 All Time Sending
* Top 10 All Time Receiving
* Top Emoji
* Top Emoji Sent
* Top Emoji Received
* Top Hours of the Day (PDT)
* Top days of the week
* Top Hours of the week
* Top month of the year
* Top days ever
* Top average messages per day since first message

It's easy to add more; just check the source code.

## Running

Install [sbt](http://www.scala-sbt.org/) if it's not installed

```
 $ brew install sbt
```

And then just run

```
 $ sbt run
```
