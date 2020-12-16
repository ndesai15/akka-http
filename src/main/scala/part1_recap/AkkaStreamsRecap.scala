package part1_recap

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, OverflowStrategy}
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}

import scala.util.{Failure, Success}

/**
  * @author ndesai on 2020-10-22
  *
 */
 
 
object AkkaStreamsRecap extends App {

  implicit val system = ActorSystem("Jaybahuchar")
  implicit val materializer = ActorMaterializer()
  import system.dispatcher

  // Source -> Sink
  val source = Source(1 to 100) // emits elements via sinks
  val sink = Sink.foreach[Int](println)
  val flow = Flow[Int].map(x=> x + 1)

  // construct akka streams
  val runnableGraph = source.via(flow).to(sink)
  val simpleMaterializedValue = runnableGraph.run()   // materialization value

  // MATERIALIZED VALUE
  val sumSink = Sink.fold[Int, Int](0)((currentSum, element) => currentSum + element)
  val sumFuture = source.runWith(sumSink)

  sumFuture.onComplete{
    case Success(value) => println(s"sum $value")
    case Failure(exception) => println(s"fail $exception")
  }

  val anotherMaterializedValue = source.viaMat(flow)(Keep.right).toMat(sink)(Keep.left).run()

  /*
      1 - Materializing a graph means materializing its all components
      2 - a materialized value can be ANYTHING AT ALL
   */

  /*
      Backpressure actions
        - buffer elements
        - apply a strategy in case the buffer overflows
        - fail the entire system
   */

  val bufferedFlow = Flow[Int].buffer(10, OverflowStrategy.dropHead)

  source.async
    .via(bufferedFlow).async
    .runForeach{
      e =>
      // a slow consumer
      Thread.sleep(100)
      println(e)
    }



}
