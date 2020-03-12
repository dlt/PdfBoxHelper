import org.apache.pdfbox.pdmodel.PDDocument
import java.io.File

import org.apache.pdfbox.pdmodel.interactive.action.PDActionGoTo
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.{PDNamedDestination, PDPageDestination}
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem

import scala.collection.mutable

import org.json4s.native.Json
import org.json4s.DefaultFormats

object PdfHelper {
  def main(args: Array[String]): Unit = {
    val doc = PDDocument.load(new File(args.head))
    val size = doc.getNumberOfPages
    val catalog = doc.getDocumentCatalog
    val outline = catalog.getDocumentOutline

    val titles = new mutable.HashMap[String, Int]
    val queue = new mutable.Queue[PDOutlineItem]()
    val visited = new mutable.HashSet[PDOutlineItem]()

    queue.enqueue(outline.getFirstChild)
    var current = queue.dequeue()

    while (current != null) {
      if (!visited.contains(current)) {
        var pd: PDPageDestination = null;

        current.getDestination match {
          case destination: PDNamedDestination =>
            pd = catalog.findNamedDestinationPage(destination)
          case destination: PDPageDestination =>
            pd = destination
          case _ => current.getAction match {
            case gta: PDActionGoTo =>
              gta.getDestination match {
                case destination: PDPageDestination =>
                  pd = destination
                case _ =>
              }
            case _ =>
          }
        }

        titles += (current.getTitle -> (pd.retrievePageNumber() + 1))

        current.children().forEach(c => queue.enqueue(c))
        var next = current.getNextSibling
        while (next != null) {
          queue.enqueue(next)
          next = next.getNextSibling
        }

        visited.add(current)
      }

      if (queue.nonEmpty) {
        current = queue.dequeue()
      } else {
        current = null
      }

    }

    println(Json(DefaultFormats).write(titles))
    println(size.toString)
  }
}
