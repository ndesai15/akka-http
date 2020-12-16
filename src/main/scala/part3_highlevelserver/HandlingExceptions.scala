package part3_highlevelserver

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.ExceptionHandler
import akka.stream.ActorMaterializer

/**
  * @author ndesai on 2020-10-22
  *
 */
 
 
object HandlingExceptions extends App {


  implicit val system = ActorSystem("HandlingExceptions")
  implicit val materializer = ActorMaterializer()
  import system.dispatcher

  // directives
  import akka.http.scaladsl.server.Directives._

  val simpleRoute =
    path("api" / "people") {
      get {
        // directive that throws an exception
        throw new RuntimeException("Getting all the people took too long")
      } ~
      post {
        parameter('id) {
          id =>
            if(id.length > 2 )
              throw new NoSuchElementException("no such element")
            complete(StatusCodes.OK)
        }
      }
    }

  implicit val customExceptionHandler: ExceptionHandler = ExceptionHandler {
    case e: RuntimeException =>
      complete(StatusCodes.NotFound, e.getMessage)
    case e: IllegalArgumentException =>
      complete(StatusCodes.BadRequest, e.getMessage)
  }

  Http().bindAndHandle(simpleRoute, "localhost", 8080)

}
