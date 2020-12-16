package part3_highlevelserver

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer

/**
  * @author ndesai on 2020-10-22
  *
 */
 
 
object HighLevelExample extends App {


  implicit val system = ActorSystem("DirectivesBreakDown")
  implicit val materializer = ActorMaterializer()
  import system.dispatcher

  // directives
  import akka.http.scaladsl.server.Directives._
}
