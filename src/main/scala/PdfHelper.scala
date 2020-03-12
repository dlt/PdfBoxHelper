import org.apache.pdfbox.pdmodel.{PDDocument, PDDocumentCatalog}
import java.io.File

import org.apache.pdfbox.pdmodel.interactive.action.PDActionGoTo
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.{PDNamedDestination, PDPageDestination}
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem

import scala.collection.mutable
import org.json4s._
import org.json4s.JsonDSL._
import org.json4s.native.Json

object PdfHelper {
  def getPageNumber(item: PDOutlineItem, catalog: PDDocumentCatalog): Int = {
    var pd: PDPageDestination = null;
    item.getDestination match {
      case destination: PDNamedDestination =>
        pd = catalog.findNamedDestinationPage(destination)
      case destination: PDPageDestination =>
        pd = destination
      case _ => item.getAction match {
        case gta: PDActionGoTo =>
          gta.getDestination match {
            case destination: PDPageDestination =>
              pd = destination
            case _ =>
          }
        case _ =>
      }
    }
    pd.retrievePageNumber() + 1
  }

  def addAllLeavesToQueue(queue: mutable.Queue[PDOutlineItem], item: PDOutlineItem) {
    item.children.forEach(c => queue.enqueue(c))
    var next = item.getNextSibling
    while (next != null) {
      queue.enqueue(next)
      next = next.getNextSibling
    }
  }

  def getBookmarks(document: PDDocument): List[(String, Int)] = {
    val catalog = document.getDocumentCatalog
    val queue = new mutable.Queue[PDOutlineItem]()
    val visited = new mutable.HashSet[PDOutlineItem]()
    queue.enqueue(catalog.getDocumentOutline.getFirstChild)
    var bookmarks: List[(String, Int)] = List()

    while (queue.nonEmpty) {
      val current = queue.dequeue()
      if (!visited.contains(current)) {
        bookmarks = bookmarks :+ (current.getTitle -> getPageNumber(current, catalog))
        addAllLeavesToQueue(queue, current)
        visited.add(current)
      }
    }
    bookmarks
  }

  def main(args: Array[String]): Unit = {
    val doc = PDDocument.load(new File(args.head))
    val json = ("metadata" -> ("pages" -> doc.getNumberOfPages) ~ ("bookmarks" -> getBookmarks(doc)))
    println(Json(DefaultFormats).write(json))
  }
}
