package part3_highlevelserver

import akka.actor.ActorSystem
import akka.event.LoggingAdapter
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpRequest, StatusCodes}
import akka.stream.ActorMaterializer

/**
  * @author ndesai on 2020-10-22
  *
 */
 
 
object DirectivesBreakDown extends App {

  implicit val system = ActorSystem("DirectivesBreakDown")
  implicit val materializer = ActorMaterializer()
  import system.dispatcher

  // directives
  import akka.http.scaladsl.server.Directives._

  /*
      Type #1 : Filter directives
   */

  val simpleRoute = path("jayambe") {
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

  val complexPathRoute =
    path("api" / "myEndPoint") { // localhost:8080/api/myEndPoint
      complete(StatusCodes.OK)
    }

  // url encoded
  val dontConfuseRoute =
    path("api/myEndPoint") { // localhost:8080/api%2FmyEndPoint
      complete(StatusCodes.OK)
    }

  /*
     Type #2: extraction directives
  */

  val pathMultiExtractRoute =
    path("api" / "order" / IntNumber / IntNumber) { (a,b) =>
      println(s"I got two numbers $a & $b")
      complete(StatusCodes.OK)
    }

  val extractRequestRoute =
    path("controlEndpoint") {
      extractRequest { (httpRequest: HttpRequest) =>
        extractLog { (log: LoggingAdapter) =>
          log.info(s"I got $httpRequest")
          complete(StatusCodes.OK)
        }
      }
    }

  val queryParamExtractioRoute =
  // api/item?id=45
  path("api" / "item") {
    parameter('id.as[Int]) { (itemId: Int) =>
      println(s"got $itemId")
      complete(StatusCodes.OK)
    }
  }

  /*
     Type #3 : composite directives
   */

  val simpleNestedRoute =
    path("api" / "item") {
      get {
        complete(StatusCodes.OK)
      }
    }

  val goodRoute = (path ("api" / "item") & get) {
    complete(StatusCodes.OK)
  }

  // /about and /aboutUs

  val repeatedRoute =
    path("about") {
      complete(StatusCodes.OK)
    } ~
    path("aboutUs") {
      complete(StatusCodes.OK)
    }

  val dryRoute = (path("about") | path("aboutUs") ) {
    complete(StatusCodes.OK)
  }

  // yourblog.com/42 or yourblog.com?postId=42
  val combinedBlogById = (path(IntNumber) | parameter('postId.as[Int])) { // both combined extraction should return same type
    (id: Int) => // server logic
      complete(StatusCodes.OK)
  }

  /*
      Type #4: "actionable" directives
   */
  val completeOkRoute = complete(StatusCodes.OK)
  val failedRoute = path("noHell") {
    failWith(new RuntimeException("Jay bahuchar"))
  }

  val routeWithRejection =
    path("home1") {
      reject
    } ~
    path("index") {
      completeOkRoute
    }

}
