package summingbird

package object proto {
  import ViewCount._
  import com.twitter.summingbird.batch.BatchID
  import java.text.SimpleDateFormat
  import java.util.{Date, TimeZone}

  val random = new scala.util.Random

  val JobDir = "summingbird/tmp/summingbird-proto/"
  val DataDir = JobDir + "data/"

  val KafkaZkConnectionString = "stage-pf8.stage.ch.flipkart.com:2181/kafka/bigfoot/fireball_1"
  //val KafkaTopic = "summingbird.proto.productview"
  val KafkaTopic = "be.oms.b2c.orders"
  val MaxId = 10

  val DataFileDateFormat = new SimpleDateFormat("yyyyMMdd-HHmmss")
  DataFileDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"))

  def dataFileForBatch(batch: BatchID) = {
    // timestamp is the end time of the batch
    DataDir + "productview_0_" + DataFileDateFormat.format(batcher.earliestTimeOf(batch.next).toDate)
  }

//  def randomView(date: Date = new Date()) = {
//    ProductViewed(
//      random.nextLong.abs % MaxId,
//      date,
//      java.util.UUID.randomUUID.toString
//    )
//  }

  def parseView(bytes: Array[Byte]): ProductViewed = {
    parseView(new String(bytes))
  }

  def parseView(s: String): ProductViewed = {
    val bits = s.split("\t")
    ProductViewed(bits(0))
  }


//  def parseView(s: String): ProductViewed = {
//    val bits = s.split("\t")
//    ProductViewed(bits(0))
//  }

//  def serializeView(pdpView: ProductViewed): String = {
//    "%s\t%s\t%s".format(pdpView.productId, pdpView.requestTime.getTime, pdpView.userGuid)
//  }

  def serializeView(pdpView: ProductViewed): String = {
    "%s\t%s\t%s".format(pdpView.userGuid)
  }
}
