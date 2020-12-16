package part4_client

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpMethods, HttpRequest, Uri}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import spray.json._

import scala.util.{Failure, Success}

/**
  * @author ndesai on 2020-10-26
  *
 */
 
 
object RequestLevel extends App with PaymentJsonProtocol with SprayJsonSupport {

  implicit val system = ActorSystem("RequestLevelAPI")
  implicit val materializer = ActorMaterializer()
  import system.dispatcher

  val responseFuture = Http().singleRequest(HttpRequest(uri= "http://www.google.com"))

  responseFuture.onComplete {
    case Success(response) =>
      // VERY IMPORTANT
      response.discardEntityBytes()
      println(s"The request was successful and returned: $response")
    case Failure(ex) =>
      println(s"The request failed with:$ex")
  }

  import PaymentSystemDomain._
  val creditCards = List(
    CreditCard("4242-4242-4242-4242", "424", "tx-test-account"),
    CreditCard("1234-1234-1234-1234", "123", "tx-daniesl-account"),
    CreditCard("1234-1234-4321-4321", "321", "my-awesome-account")
  )

  val paymentRequests: List[PaymentRequest] = creditCards.map(creditcard => PaymentRequest(creditcard, "rtjvm-store-account", 99))

  // create a source out of it
  val serverHttpRequest: List[HttpRequest] = paymentRequests.map(paymentRequest =>
    HttpRequest(
      HttpMethods.POST,
      uri = "http://localhost:8080/api/payments",
      entity = HttpEntity(
        ContentTypes.`application/json`,
        paymentRequest.toJson.prettyPrint
      )
    )
  )

  Source(serverHttpRequest)
    .mapAsync(10)(request => Http().singleRequest(request))
    .runForeach(println)
}
