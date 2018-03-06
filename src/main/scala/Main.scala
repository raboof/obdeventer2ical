import java.io.{ InputStream, OutputStream }
import java.nio.charset.Charset
import java.time._

import scala.language.implicitConversions
import scala.language.postfixOps

import dispatch.Http

import scala.concurrent._
import scala.concurrent.duration._

import icalendar._
import icalendar.Properties._
import icalendar.CalendarProperties._
import icalendar.ValueTypes._
import icalendar.ical.Writer._

import net.ruippeixotog.scalascraper.model._
import net.ruippeixotog.scalascraper.browser.JsoupBrowser
import net.ruippeixotog.scalascraper.scraper.HtmlExtractor

import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import net.ruippeixotog.scalascraper.dsl.DSL.Parse._

trait Main {
  import scala.util.Try
  object ToInt {
    def unapply(in: String): Option[Int] = Try(in.toInt).toOption
  }
  implicit val ec: ExecutionContext = ExecutionContext.global
  implicit def liftOption[T](value: T): Option[T] = Some(value)

  def links(doc: Document): List[String] =
    (doc >> elementList("#tab-agenda-list ul.list li")).map(element =>
      (element >> elementList("a"))(1) >> attr("href")("a"))

  def getDatalistFields(article: Element): Map[String, String] =
    (article >> elements(".datalist"))
      .flatMap(_.children.sliding(2, 2).flatMap {
        case Seq(k, v) => Some((k >> text("dt")) -> (v >> text("dd")))
        case Seq(_) => None
      })
      .toMap

  def parseEvent(url: String, doc: Document): Event = {
    val id = url.replaceAll("[^\\d]", "").toInt
    val article = doc >> element("article")
    val data = getDatalistFields(article)
    val datePattern = "(\\d+)-(\\d+)-(\\d+).*".r
    val date = data("Datum:") match {
      case datePattern(d, m, y) => s"$y-$m-$d"
    }
    val starttime = "\\d+:\\d+".r.findFirstIn(data("Geopend:")).getOrElse("00:00")

    Event(
      uid = Uid(s"vvvdeventer2ical-$id"),
      dtstart =
        ZonedDateTime.parse(s"${date}T${starttime}+02:00[Europe/Amsterdam]").withZoneSameInstant(ZoneOffset.UTC),
      summary = Summary(Text.fromString(article >> text("h1"))),
      description = Description(Text.fromString(article >> text("p"))),
      categories = List(Categories(ListType(data("Categorie:")))),
      url = Url(url)
    )
  }

  def fetchDocument(uri: String): Future[Document] = {
    val browser = JsoupBrowser()

    // JsoupBrowser.get expects UTF-8, vvvdeventer is windows codepage
    Http(dispatch.url(uri) OK dispatch.as.String).map {
      val doc = browser.parseString(_)
      doc
    }
  }

  def event(element: Element): Event = {
    val url = element >> attr("href")("a")
    val id = url.filter(_.isDigit)
    val time = element >> text(".time_overview")
    val datePattern = """\w\w (\d+)-(\d+), (\d+):(\d+) uur""".r
    val date = time match {
      case datePattern(ToInt(day), ToInt(month), ToInt(hour), ToInt(minute)) =>
        ZonedDateTime.parse(f"2017-$month%02d-$day%02dT$hour%02d:$minute%02d:00+02:00[Europe/Amsterdam]").withZoneSameInstant(ZoneOffset.UTC)
    }
    val title = element >> text(".titelbalk2")
    val lines = element >> elementList(".movie_event_desc li")
    val body = (lines(2) >> text("li")).replaceAll(" ... lees meer", "")
    Event(
      uid = Uid(s"fdk2ical-$id"),
      summary = Summary(title),
      description = Description(body),
      dtstart = date,
      url = Url(url)
    )
  }

  def events(doc: Document): Iterable[Event] = (doc >> elements(".movie_event")).map(event(_))

  def fetchCalendar(): String = {
    val urlPrefix = "https://www.filmhuisdekeizer.nl/programma/"

    val results = Await.result(fetchDocument(urlPrefix + "specials/").map(events(_)), 120 seconds)

    // ec.dumpToFile("timeline.data")
    asIcal(Calendar(
      prodid = Prodid("-//raboof/fdk2ical//NONSGML v1.0//NL"),
      events = results.toList
    ))
  }
}

class MainLambda extends Main {
  def handleRequest(inputStream: InputStream, outputStream: OutputStream): Unit = {
    val result = fetchCalendar()
    outputStream.write(result.getBytes("UTF-8"));
    outputStream.flush();
  }
}

object MainApp extends App with Main {
  print(fetchCalendar())
}
