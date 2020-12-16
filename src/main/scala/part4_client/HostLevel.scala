package part4_client

import java.util.UUID

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpMethods, HttpRequest, HttpResponse, StatusCodes, Uri}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import spray.json._

import scala.util.{Failure, Success}

/**
  * @author ndesai on 2020-10-26
  *
  */
object HostLevel extends App with PaymentJsonProtocol with SprayJsonSupport {

  implicit val system = ActorSystem("HostLevel")
  implicit val materialzer = ActorMaterializer()
  import system.dispatcher

  val poolFlow = Http().cachedHostConnectionPool[Int]("www.google.com")

  Source(1 to 10)
    .map(i => (HttpRequest(), i))
    .via(poolFlow)
    .map {
      case (Success(response), value) =>
        // VERY IMPORTANT
        response.discardEntityBytes() //if you don't use this that means you are leaking connections
        s"Request $value has received response: $response"
      case (Failure(ex), value) =>
        s"Request $value has failed: $ex"
    }
    .runWith(Sink.foreach[String](println))

  import PaymentSystemDomain._
  val creditCards = List(
    CreditCard("4242-4242-4242-4242", "424", "tx-test-account"),
    CreditCard("1234-1234-1234-1234", "123", "tx-daniesl-account"),
    CreditCard("1234-1234-4321-4321", "321", "my-awesome-account")
  )

  val paymentRequests: List[PaymentRequest] = creditCards.map(
    creditcard => PaymentRequest(creditcard, "rtjvm-store-account", 99)
  )

  // create a source out of it
  val serverHttpRequest = paymentRequests.map(
    paymentRequest =>
      (
        HttpRequest(
          HttpMethods.POST,
          uri = Uri("/api/payments"),
          entity = HttpEntity(
            ContentTypes.`application/json`,
            paymentRequest.toJson.prettyPrint
          )
        ),
        UUID.randomUUID().toString
    )
  )

  Source(serverHttpRequest)
    .via(Http().cachedHostConnectionPool[String]("localhost",8080))
    .runForeach { //(Try[HttpResponse], String)
      case (Success(response@HttpResponse(StatusCodes.Forbidden, _, _, _)), orderId) =>
        println(s"The order Id $orderId was not allowed to proceed: $response")
      case (Success(response), orderId) => println(s"The order id $orderId was successful and returned the response: $response")
      case (Failure(ex), orderId) => println(s"The order Id $orderId could not be completed: $ex")
    }

  // High-volume , low-latency requests
}
