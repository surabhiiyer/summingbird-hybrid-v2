package summingbird.proto

import com.twitter.summingbird.store.ClientStore
import com.twitter.util.Await

import org.slf4j.LoggerFactory
import com.twitter.summingbird.batch.BatchID

/**
  * The following object contains code to execute the Summingbird
  * WordCount job defined in ExampleJob.scala on a hybrid
  * cluster.
  */
object HybridRunner {
  @transient private val logger = LoggerFactory.getLogger(this.getClass)
  /**
    * These imports bring the requisite serialization injections, the
    * time extractor and the batcher into implicit scope. This is
    * required for the dependency injection pattern used by the
    * Summingbird Scalding platform.
    */
  import Serialization._, ViewCount._

  /**
    * The ClientStore combines results from the offline store
    * with the results for any unprocessed batches from the
    * online store.
    */
  val store = ClientStore(
    //ScaldingRunner.servingStore,
    StormRunner.viewCountStore,
    1
  )

//  def lookup(lookId: Long): Option[Long] =
//    Await.result {
//      store.get(lookId)
//    }


//  def lookup(lookup:String): Option[Long] =
//    Await.result {
//      store.get(lookup)
//    }

//  def lookup(pdpView:ProductViewed): Option[Long] =
//    Await.result {
//      store.get(pdpView.userGuid)
//    }

  def lookup(pdpView:ProductViewed): Option[Long] =
    Await.result {

      //logger.info("Events Counted (online): " + StormRunner.viewCountStore.multiGet(pdpView.userGuid.map(_ -> batcher.currentBatch).toSet).map(kv => Await.result(kv._2).getOrElse(0L)).sum)

      for(v: Option[Long] <- store.get(pdpView.userGuid))
      {
       logger.info("#### OptionLong ###",v);
      }
      //store.get(pdpView.userGuid)
    }



  //  def lookupDebug(lookId: Long): Unit = {
//    val offline = ScaldingRunner.lookup(lookId)
//    logger.info("Offline: %s".format(offline))
//
//    Stream.iterate(offline.map(_._1).getOrElse(batcher.currentBatch))(_ + 1)
//      .takeWhile(_ <= batcher.currentBatch).foreach { batch =>
//      val online = Await.result {
//        StormRunner.viewCountStore.get(lookId -> batch)
//      }
//      logger.info("Online: %s".format((batch,online)))
//    }
//
//    val hybrid = lookup(lookId)
//
//    logger.info("Hybrid: %s".format(hybrid))
//
//  }

//  def lookupAll() = {
//    store.multiGet((0L to (MaxId - 1)).toSet).map { case (k,v) =>
//      logger.info(k + " : " + Await.result(v))
//    }
//  }
}


object RunHybrid extends App {
  @transient private val logger = LoggerFactory.getLogger(this.getClass)

  import java.util.concurrent.{CyclicBarrier, Executors,TimeUnit}
  import com.twitter.summingbird.storm
  import summingbird.proto.ViewCount._

  import sys.process._

  s"rm -rf ${DataDir}".!!
  s"rm -rf ${JobDir}waitstate".!!
  s"rm -rf ${JobDir}store".!!

  s"mkdir -p ${DataDir}".!!


  val executor = Executors.newScheduledThreadPool(5)

  val barrier = new CyclicBarrier(2)

  // start ingestion
  executor.submit(new Runnable {
    def run = try { Ingestion.run(() => barrier.await()) } catch { case e: Throwable => logger.error("ingestion error", e) }
  })


  // start storm processing
  // not really sure if this needs a separate thread or if storm.Executor does that
  executor.submit(new Runnable {
    def run = try { storm.Executor(Array("--local"), StormRunner(_)) } catch { case e: Throwable => logger.error("storm error", e) }
  })


  // start message generator
 // executor.submit(new Runnable {
 //   def run = try { DummyClickstream.run() } catch { case e: Throwable => logger.error("dummy clickstream error", e) }
//  })

  // Run the batch job after each log file is available
//  executor.submit(new Runnable {
//    def run = {
//      while (true) {
//        barrier.await()
//        try { ScaldingRunner.runOnce } catch { case e: Throwable => logger.error("batch failure", e) }
//      }
//    }
//  })
  // run sanity checks

  executor.scheduleAtFixedRate(
    new Runnable {
      def run = { try {
        logger.info("Sanity Check")
        //loggerinfo("lookupDebug(7)")
        //HybridRunner.lookupDebug(7)
        logger.info("Events Ingested: " + Ingestion.ingested)
        val a: String  = "";
        val pdpView = new ProductViewed(a);
       // val ids = 0L to (MaxId - 1)
       //var a = lookup() ;
      //logger.info("Events Counted (online): " + StormRunner.viewCountStore.multiGet(userGuid.map(_ -> batcher.currentBatch).toSet).map(kv => Await.result(kv._2).getOrElse(0L)).sum)
      //logger.info("Events Counted (online): " + StormRunner.viewCountStore.multiGet(map(_ -> batcher.currentBatch).toSet).map(kv => Await.result(kv._2).getOrElse("")).sum)
      //logger.info("Events Counted (online): " + a)
     // val userGuid = ProductViewed.userGuid;
     // logger.info("Events Counted (online): " + StormRunner.viewCountStore.multiGet(userGuid.map(_ -> batcher.currentBatch).toSet).map(kv => Await.result(kv._2).getOrElse(0L)).sum);
       logger.info("Events Counted (online): " + HybridRunner.lookup(pdpView));
      //logger.info("Events Counted (hybrid): " + HybridRunner.store.multiGet(ids.toSet).map(kv => Await.result(kv._2).getOrElse(0L)).sum)
      }
      catch {
        case e: Throwable => logger.error("sanity check failure", e)
      }}
    },
    1, 1, TimeUnit.MINUTES
  )

}

//        logger.info("Events Produced: " + DummyClickstream.produced)
//        logger.info("Events Counted (offline): " + ScaldingRunner.servingStore.multiGet(ids.toSet).map(kv => Await.result(kv._2).map(_._2).getOrElse(0L)).sum)
