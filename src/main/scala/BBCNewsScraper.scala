
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.libs.json.Json.JsValueWrapper
import play.api.libs.json._

import scala.io.Source
import scala.util.Using
import scala.xml.{Elem, XML}

object BBCNewsScraper {
  def main(args: Array[String]): Unit = {
    val worldNewsUrl = "https://feeds.bbci.co.uk/news/world/rss.xml"
    val topNewsUrl = "https://feeds.bbci.co.uk/news/rss.xml"

    val allNewsLinks = scrapeAllNewsLinks(worldNewsUrl, topNewsUrl)

    // Scrape information for each news link
    val newsPageInfoSet = allNewsLinks.map(scrapeNewsPageInfo)

    // Convert to JSON array
    val jsonArray = toJsonArray(newsPageInfoSet)
    println(s"\nJSON Array:\n$jsonArray")

    // Return the number of entries in the JSON array
    val numEntries = jsonArray.value.size
    println(s"\nNumber of entries in the JSON array: $numEntries")
  }

  private case class NewsPageInfo(quelle: String, title: String, text: String, category: String, publishingDate: String)

  private def scrapeAllNewsLinks(worldNewsUrl: String, topNewsUrl: String): Set[String] = {
    val worldNewsLinks = scrapeNewsLinks(worldNewsUrl)
    val topNewsLinks = scrapeNewsLinks(topNewsUrl)

    // Combine and eliminate duplicates
    worldNewsLinks ++ topNewsLinks
  }

  private def scrapeNewsLinks(url: String): Set[String] = {
    Using.resource(Source.fromURL(url)) { source =>
      // Read the XML data from the URL
      val xmlString = source.mkString

      // Parse the XML
      val xml: Elem = XML.loadString(xmlString)

      // Extract news links
      val newsLinks = (xml \ "channel" \ "item" \ "link").map(_.text).toSet

      // Return the set of news links
      newsLinks
    }
  }

  // Custom implicit Releasable instance for Jsoup's Document
  implicit val jsoupDocumentReleasable: Using.Releasable[Document] = (_: Document) => ()

  private def scrapeNewsPageInfo(newsLink: String): NewsPageInfo = {
    Using.resource(Jsoup.connect(newsLink).get()) { document =>
      val quelle = "BBC"
      val title = document.select("h1").text()
      val text = document.select("article").text()
      val category = document.select("meta[property=article:section]").attr("content")
      val timestamp = document.select("time[datetime]").attr("datetime")

      NewsPageInfo(quelle,title, text, category, timestamp)
    }
  }

  private def toJsonArray(newsPageInfoSet: Set[NewsPageInfo]): JsArray = {
    val jsonArray: JsArray = if (newsPageInfoSet.nonEmpty) {
      Json.arr(newsPageInfoSet.toSeq.map { pageInfo =>
        Json.obj(
          "quelle" -> Json.toJson(pageInfo.quelle),
          "title" -> Json.toJson(pageInfo.title),
          "text" -> Json.toJson(pageInfo.text),
          "category" -> Json.toJson(pageInfo.category),
          "timestamp" -> Json.toJson(pageInfo.publishingDate)
        ): JsValueWrapper
      }: _*)
    } else {
      // If set is empty, add a default entry
      Json.arr(Json.obj("entry" -> "NoData"))
    }

    jsonArray
  }
}