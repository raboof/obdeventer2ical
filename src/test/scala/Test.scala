import icalendar.ical.Writer._

import net.ruippeixotog.scalascraper.browser.JsoupBrowser

import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import net.ruippeixotog.scalascraper.dsl.DSL.Parse._

import org.scalatest._

class Test extends WordSpec with Matchers with Main with BeforeAndAfterAll {
  "The HTML scraping algorithm" should {
    "correctly find events on the agenda page" in {
      val browser = JsoupBrowser()
      val elems = events(browser.parseResource("/agenda1.html"))
      elems.size should be(10)

      val inloopuur = elems.head
      inloopuur.uid.value.text should be("obdeventer2ical-935")
      inloopuur.url.get.value.uri.toString should be("https://www.obdeventer.nl/agenda1/935-inloopspreekuur-digihulp")
      inloopuur.summary.get.value.text should be("Inloopspreekuur Digihulp")
      inloopuur.description.get.value.text should be("Heb je vragen over je computer, tablet of smartphone? Stel ze tijdens het inloopspreekuur Digihulp aan digitaal expert Erik Baaij.")
    }
  }

  override def afterAll() = {
    system.terminate()
  }
}
