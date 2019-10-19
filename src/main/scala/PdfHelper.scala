import org.apache.pdfbox.pdmodel.PDDocument
import java.io.File

object PdfHelper {
  def main(args: Array[String]): Unit = {
    val size = PDDocument.load(new File(args.head)).getNumberOfPages()
    println(size.toString)
  }
}
