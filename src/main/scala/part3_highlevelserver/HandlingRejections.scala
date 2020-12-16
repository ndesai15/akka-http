package part3_highlevelserver

import akka.actor.ActorSystem
import akka.http.javadsl.server.{MethodRejection, MissingQueryParamRejection}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.{Rejection, RejectionHandler}
import akka.stream.ActorMaterializer

/**
  * @author ndesai on 2020-10-22
  *
  */
object HandlingRejections extends App {

  implicit val system = ActorSystem("HandlingRejections")
  implicit val materializer = ActorMaterializer()
  import system.dispatcher

  // directives
  import akka.http.scaladsl.server.Directives._

  // Rejection Handlers
  val badRejectionHandler: RejectionHandler = { x: Seq[Rejection] =>
    println(s"I have encountered rejections $x")
    Some(complete(StatusCodes.BadRequest))
  }
  val forbiddenRejectionHandler: RejectionHandler = { x: Seq[Rejection] =>
    println(s"I have encountered rejections $x")
    Some(complete(StatusCodes.Forbidden))
  }

  val simpleRoute =
    handleRejections(badRejectionHandler) {
      path("api" / "home") {
        get {
          complete(StatusCodes.OK)
        } ~
          post {
            handleRejections(forbiddenRejectionHandler) {
              parameter('id) { _ =>
                complete(StatusCodes.OK)
              }
            }
          }
      }
    }

  val simpleRouteWithImplicit =
      path("api" / "home") {
        get {
          complete(StatusCodes.OK)
        } ~
          post {
              parameter('id) { _ =>
                complete(StatusCodes.OK)
              }
          }
      }

  implicit val customRejectionHandler = RejectionHandler
    .newBuilder()
    .handle {
      case m: MissingQueryParamRejection =>
        println(s"query parameter $m")
        complete(StatusCodes.Forbidden)
    }
    .handle {
      case m: MethodRejection =>
        println(s"incorrect method call $m")
        complete(StatusCodes.BadRequest)
    }
      .result()

  Http().bindAndHandle(simpleRouteWithImplicit, "localhost", 8080)

}
