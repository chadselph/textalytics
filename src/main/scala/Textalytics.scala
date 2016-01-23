import java.io.File
import java.nio.file.Paths
import java.sql.DriverManager
import java.time.temporal.ChronoUnit
import java.time.{Instant, ZoneId, ZonedDateTime}

import scala.collection.mutable.ListBuffer

object Textalytics extends App {

  implicit object ZonedDateTimeOrdering extends Ordering[ZonedDateTime] {
    override def compare(x: ZonedDateTime, y: ZonedDateTime): Int = x.toInstant.compareTo(y.toInstant)
  }

  type Number = String
  case class Message(number: Number, text: String, date: ZonedDateTime, fromMe: Boolean)

  val BackupDirectoryPath = Paths.get(
    System.getProperty("user.home"),
    "/Library/Application Support/MobileSync/Backup/"
  ).toString
  val SmsFilename = "3d0d7e5fb2ce288813306e4d4636395e047a3d28"


  val messages = fetchAllMessages()

  println("Top 10 All Time Texting")
  messages.top(_ => true, 10) foreach { printTop }
  println()

  println("Top 10 All Time Sending")
  messages.top(_.fromMe, 10) foreach { printTop }
  println()

  println("Top 10 All Time Receiving")
  messages.top(!_.fromMe, 10) foreach { printTop }
  println()

  println("Top Emoji")
   messages.flatMap(_.text.codePoints().toArray.filterNot(Character.isBmpCodePoint))
    .mostCommon(10, codePointToString).foreach { printTop }
  println("Top Emoji Sent")
  messages.filter(_.fromMe).flatMap(_.text.codePoints().toArray.filterNot(Character.isBmpCodePoint))
    .mostCommon(10, codePointToString).foreach { printTop }
  println("Top Emoji Received")
  messages.filterNot(_.fromMe).flatMap(_.text.codePoints().toArray.filterNot(Character.isBmpCodePoint))
    .mostCommon(10, codePointToString).foreach { printTop }

  /*
  println("Top Words")
  messages.flatMap(_.text.split(" ")).filter(_.length >= 4).mostCommon(25).foreach(printTop)
  */

  println("Top Hours (PDT)")
  messages.mostCommon(7, _.date.getHour).foreach(printTop)

  println("Top days of the week")
  messages.mostCommon(7, _.date.getDayOfWeek).foreach(printTop)

  println("Top Hours of the week")
  messages.mostCommon(10, d => (d.date.getDayOfWeek, d.date.getHour)).foreach(printTop)

  println("Top month of the year")
  messages.mostCommon(10, d => (d.date.getMonth, d.date.getYear)).foreach(printTop)

  println("Top days ever")
  messages.mostCommon(10, _.date.toLocalDate).foreach(printTop)

  println("Top average messages per day since first text")
  messages.groupBy(_.number).map {
    case (n, msgs) =>
      val dates = msgs.map(_.date)
      val delta = 1 + ChronoUnit.DAYS.between(dates.min, dates.max)
      (n, msgs.length.toFloat / delta)
  }.toSeq.sortBy(-_._2).take(10).foreach(printTop)

  def fetchAllMessages(): Seq[Message] = {
    val backupDirectory = new File(BackupDirectoryPath)
    if (!backupDirectory.exists()) throw new Exception(s"No backup found in $BackupDirectoryPath")

    val newestBackup = backupDirectory.listFiles().maxBy(_.lastModified())
    val dbPath = Paths.get(newestBackup.getAbsolutePath, SmsFilename)

    val connection = DriverManager.getConnection(s"jdbc:sqlite:$dbPath")
    val statement = connection.createStatement()

    val rs = statement.executeQuery(
      """
      SELECT chat_identifier, text, date, is_from_me FROM message
      JOIN chat_message_join ON chat_message_join.message_id = message.rowid
      JOIN chat ON chat.rowid = chat_message_join.chat_id
      """)
    val buffer = ListBuffer[Message]()
    while(rs.next()) {
      buffer += Message(
        rs.getString("chat_identifier"),
        new String(Option(rs.getBytes("text")).getOrElse(Array()), "UTF-8"),
        ZonedDateTime.ofInstant(Instant.ofEpochSecond(rs.getInt("date")), ZoneId.of("UTC")).plusYears(31).withZoneSameInstant(ZoneId.systemDefault()),
        rs.getBoolean("is_from_me")
      )
    }
    buffer.toSeq
  }

  def printTop[A,B](top: (A, B)) = {
    println(s"${top._2} - ${top._1}")
  }

  def codePointToString(cp: Int) = {
    new String(Array(cp), 0, 1)
  }

  implicit class RichSeq[A](val s: Seq[A]) {

    def mostCommon(n: Int): Seq[(A, Int)] = {
      mostCommon(n, identity)
    }

    /**
     * Not a particularly efficient implementation, but good enough.
     */
    def mostCommon[B](n: Int, f: (A => B)): Seq[(B, Int)] = {
      s.groupBy(f)
        .map { case (found, grouped) => found -> grouped.length }
        .toSeq
        .sortBy(-_._2)
        .take(n)
    }
  }

  implicit class RichMessageSeq(val messageSeq: Seq[Message]) {
    def top(filter: Message => Boolean, n: Int = 10): Seq[(Number, Int)] = {
      messageSeq.filter(filter).mostCommon(n, _.number)
    }
  }
}
