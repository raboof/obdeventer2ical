import java.io.{ InputStream, OutputStream }
import java.time._

import akka.actor.ActorSystem

import scala.language.implicitConversions
import scala.language.postfixOps
import akka.http.scaladsl._
import akka.http.scaladsl.model.HttpRequest
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.StreamConverters

import scala.concurrent._
import scala.concurrent.duration._
import icalendar._
import icalendar.Properties._
import icalendar.CalendarProperties._
import icalendar.ical.Writer._
import net.ruippeixotog.scalascraper.model._
import net.ruippeixotog.scalascraper.browser.JsoupBrowser
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._

trait Main {

  implicit def liftOption[T](value: T): Option[T] = Some(value)

  implicit val system = ActorSystem()
  implicit val ec: ExecutionContext = system.dispatcher
  implicit val mat = ActorMaterializer()

  val urlPrefix = "https://www.obdeventer.nl/agenda1" // ?start=10

  def fetchDocument(uri: String): Future[Document] = {
    val browser = JsoupBrowser()

    Http.get(system).singleRequest(HttpRequest(uri = uri))
      .map { response => response.entity.dataBytes.runWith(StreamConverters.asInputStream()) }
      .map { is => browser.parseInputStream(is, "UTF-8") }
  }

  def event(element: Element): Event = {
    import RegexUtils._

    //    println(element)
    val url = (element >> attr("href")("a")).drop("/agenda1/".length)
    val id = url.filter(_.isDigit)
    val dateTime = element >> text(".date")

    val amsterdam = ZoneId.of("Europe/Amsterdam")
    val now = ZonedDateTime.now(amsterdam)
    val date = dateTime match {
      case re"""\w+ (\d+)${ ToInt(dayOfMonth) } (\w+)${ ToMonth(month) } \((.*)$rest\)""" =>
        val year =
          if (month.getValue < now.getMonth.getValue) now.getYear + 1
          else now.getYear
        rest match {
          case re"""(vanaf |)$v(\d+)${ ToInt(h) }\.(\d+)${ ToInt(m) }.*""" =>
            ZonedDateTime.of(year, month.getValue, dayOfMonth, h, m, 0, 0, amsterdam)
        }
    }
    val title = element >> text("h3.titel")
    val body = element >> text(".item-introtext")
    Event(
      uid = Uid(s"obdeventer2ical-$id"),
      summary = Summary(title),
      description = Description(body),
      dtstart = date,
      url = Url(urlPrefix + "/" + url)
    )
  }

  def events(doc: Document): Iterable[Event] = (doc >> elements(".overview li div.item-container")).map(event(_))

  def fetchCalendar(): String = {
    val results = Await.result(fetchDocument(urlPrefix).map(events(_)), 120 seconds)

    // ec.dumpToFile("timeline.data")
    asIcal(Calendar(
      prodid = Prodid("-//raboof/obdeventer2ical//NONSGML v1.0//NL"),
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
  system.terminate()
}

object RegexUtils {
  implicit class RegexHelper(val sc: StringContext) extends AnyVal {
    def re: scala.util.matching.Regex = sc.parts.mkString.r
  }

  import scala.util.Try
  object ToInt {
    def unapply(in: String): Option[Int] = Try(in.toInt).toOption
  }

  object ToMonth {
    def unapply(in: String): Option[Month] = {
      import Month._
      Try(in match {
        case "januari" => JANUARY
        case "februari" => FEBRUARY
        case "maart" => MARCH
        case "april" => APRIL
        case "mei" => MAY
        case "juni" => JUNE
        case "juli" => JULY
        case "augustus" => AUGUST
        case "september" => SEPTEMBER
        case "oktober" => OCTOBER
        case "november" => NOVEMBER
        case "december" => DECEMBER
      }).toOption
    }
  }

}
