import icalendar.ical.Writer._

import net.ruippeixotog.scalascraper.browser.JsoupBrowser

import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import net.ruippeixotog.scalascraper.dsl.DSL.Parse._

import org.scalatest._

class Test extends WordSpec with Matchers with Main {
  "The HTML scraping algorithm" should {
    "correctly find events on the specials page" in {
      val browser = JsoupBrowser()
      val elems = events(browser.parseResource("/specials.html"))
      elems.size should be(6)

      val rondeel = elems.head
      rondeel.uid.value.text should be("fdk2ical-5039")
      rondeel.url.get.value.uri.toString should be("http://www.filmhuisdekeizer.nl/programma/5039/rondeel-cinema-paradijs-binnen-handbereik")
      rondeel.summary.get.value.text should be("Rondeel Cinema: Paradijs Binnen Handbereik")
      rondeel.description.get.value.text should be("Te zien op 21 februari tijdens Rondeel Cinema. Met inleiding door Gerrit Lommerse. De Nederlandse tuin- en landschapsontwerper Piet Oudolf verwierf wereldfaam met zijn creaties waarmee hij de openbare ruimte voorgoed veranderd heeft. Zijn bekendste werk is")
    }
  }
}
