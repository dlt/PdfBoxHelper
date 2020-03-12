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
  def getPageNumber(catalog: PDDocumentCatalog, item: PDOutlineItem): Int = {
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
    var bookmarks: List[(String, Int)] = List()

    if (catalog.getDocumentOutline != null) {
      val visited = new mutable.HashSet[PDOutlineItem]()
      val queue = new mutable.Queue[PDOutlineItem]()
      queue.enqueue(catalog.getDocumentOutline.getFirstChild)

      while (queue.nonEmpty) {
        val item = queue.dequeue()
        if (!visited.contains(item)) {
          bookmarks = bookmarks :+ (item.getTitle -> getPageNumber(catalog, item))
          addAllLeavesToQueue(queue, item)
          visited.add(item)
        }
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
