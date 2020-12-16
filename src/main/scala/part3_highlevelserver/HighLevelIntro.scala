package part3_highlevelserver

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer

/**
  * @author ndesai on 2020-10-22
  *
 */
 
 
object HighLevelIntro extends App {

  implicit val system = ActorSystem("HighLevelIntro")
  implicit val materializer = ActorMaterializer()
  import system.dispatcher

  // directives
  import akka.http.scaladsl.server.Directives._

  val simpleRoute: Route =
    path("home") { // DIRECTIVE
      complete(StatusCodes.OK) // DIRECTIVE
    }

  val pathGetRoute =
    path("home") {
      get {
        complete(StatusCodes.OK)
      }
    }

  val chainedRoute =
    path("jaybahuchar") {
      get {
        complete(StatusCodes.OK)
      } /* VERY IMPORTANT -----> */ ~
      post {
        complete(StatusCodes.Forbidden)
      }
    } ~
    path("jayambe") {
      complete(
        HttpEntity(
          ContentTypes.`text/html(UTF-8)`,
          """
            |<html>
            |<body>
            |Jay Ambe, Jaybahuchar Mataji
            |</body>
            |</html>
          """.stripMargin
        )
      )
    } // Routing Tree
  Http().bindAndHandle(chainedRoute, "localhost", 8080)
}
