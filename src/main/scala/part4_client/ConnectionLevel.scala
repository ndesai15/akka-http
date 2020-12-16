package part4_client

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpMethods, HttpRequest, HttpResponse, Uri}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}

import scala.util.{Failure, Success}
import spray.json._

/**
  * @author ndesai on 2020-10-26
  *
 */
 
 
object ConnectionLevel extends App with PaymentJsonProtocol with SprayJsonSupport{

  implicit val system = ActorSystem("ConnectionLevel")
  implicit val materializer = ActorMaterializer()
  import system.dispatcher

  // Goal: Use Akka-http to issue a request to other servers

  // Create a connection flow request for a server where you want to send lots of requests
  val connectionFlow = Http().outgoingConnection("www.google.com")

  def oneOffRequest(request: HttpRequest) =
    Source.single(request).via(connectionFlow).runWith(Sink.head)

  oneOffRequest(HttpRequest()).onComplete {
    case Success(response) => println(s"Got successful response $response")
    case Failure(ex) => println(s"Got an exception $ex")
  }

  /*
     A small payment system
   */
  import PaymentSystemDomain._
  val creditCards = List(
    CreditCard("4242-4242-4242-4242", "424", "tx-test-account"),
    CreditCard("1234-1234-1234-1234", "123", "tx-daniesl-account"),
    CreditCard("1234-1234-4321-4321", "321", "my-awesome-account")
  )

  val paymentRequests: List[PaymentRequest] = creditCards.map(creditcard => PaymentRequest(creditcard, "rtjvm-store-account", 99))

  // create a source out of it
  val serverHttpRequest = paymentRequests.map(paymentRequest =>
    HttpRequest(
      HttpMethods.POST,
      uri = Uri("/api/payments"),
      entity = HttpEntity(
        ContentTypes.`application/json`,
        paymentRequest.toJson.prettyPrint
      )
    )
  )
  // client code that send request to server
  Source(serverHttpRequest)
    .via(Http().outgoingConnection("localhost", 8080))
    .to(Sink.foreach[HttpResponse](println)) // runnable graph
    .run()
}
