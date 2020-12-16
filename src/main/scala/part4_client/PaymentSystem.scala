package part4_client

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.{StatusCode, StatusCodes}
import akka.stream.ActorMaterializer
import spray.json._
import akka.http.scaladsl.server.Directives._
import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.Future

/**
  * @author ndesai on 2020-10-26
  *
 */

case class CreditCard(serialNumber: String, securityCode: String, account: String)

object PaymentSystemDomain {
  case class PaymentRequest(creditCard: CreditCard, receiverAccount: String, amount: Double)
  case object PaymentAccepted
  case object PaymentRejected
}

trait PaymentJsonProtocol extends DefaultJsonProtocol {
  implicit val creditcardFormat = jsonFormat3(CreditCard)
  implicit val paymentRequestFormat = jsonFormat3(PaymentSystemDomain.PaymentRequest)
}

class PaymentValidator extends Actor with ActorLogging {
  import PaymentSystemDomain._

  override def receive: Receive = {
    case PaymentRequest(CreditCard(serialNumber, _, senderAccount), receiverAccount, amount) =>
      log.info(s"$senderAccount is trying to send $amount dollars to $receiverAccount")
      if(serialNumber == "1234-1234-1234-1234") sender() ! PaymentRejected
      else sender() ! PaymentAccepted
  }
}


object PaymentSystem extends App with PaymentJsonProtocol with SprayJsonSupport{

  // microservice for payments
  implicit val system = ActorSystem("PaymentSystem")
  implicit val materializer = ActorMaterializer()
  import system.dispatcher
  import scala.concurrent.duration._
  implicit val timeout = Timeout(2 seconds)
  import PaymentSystemDomain._

  val paymentValidator = system.actorOf(Props[PaymentValidator], "paymentvalidator")

  val paymentRoute =
    path("api" / "payments") {
      post {
        entity(as[PaymentRequest]) { paymentRequst =>
          val validationResponse: Future[StatusCode] = (paymentValidator ? paymentRequst).map {
            case PaymentRejected => StatusCodes.Forbidden
            case PaymentAccepted => StatusCodes.OK
            case _ => StatusCodes.BadRequest
          }
          complete(validationResponse)

        }
      }
    }

  Http().bindAndHandle(paymentRoute, "localhost", 8080)



}
