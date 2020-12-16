package part3_highlevelserver

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.model.ws.{BinaryMessage, Message, TextMessage}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.util.CompactByteString
import akka.http.scaladsl.server.Directives._
import scala.concurrent.duration._

/**
  * @author ndesai on 2020-10-23
  *
 */
 
 
object WebSocketsDemo extends App {

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
  import system.dispatcher

  // Message: TextMessage & BinaryMessage
  val textMessage = TextMessage(Source.single("Jay bahuchar via a text message")) // client & server can exchange any type of data
  val binaryMessage = BinaryMessage(Source.single(CompactByteString("Jay bahuchar from a binary string"))) // you can convert any type of data to byte stream like audio, video

  val html =
    """
      |<html>
      |<head>
      |    <script>
      |        // Standard method of opening web socket
      |        var exampleSocket = new WebSocket("ws://localhost:8080/greeter");// creates a web socket
      |        console.log("starting websocket ...");
      |
      |        exampleSocket.onmessage = function (event) {
      |            var newChild = document.createElement("div");
      |            newChild.innerText = event.data;
      |            document.getElementById("1").appendChild(newChild);
      |        };
      |
      |        exampleSocket.onopen = function (event) {
      |            exampleSocket.send("socket seems to be opened");
      |        };
      |        exampleSocket.send("socket says: hello server!");
      |</script>
      |</head>
      |<body>
      |starting websocket
      |<div id="1">
      |
      |</div>
      |</body>
      |</html>
    """.stripMargin

  def webSocketFlow: Flow[Message, Message, Any] = Flow[Message].map {
    case tm: TextMessage =>  // If Server receives a text message, it will reply as text message
      TextMessage(Source.single("server says back:") ++ tm.textStream ++ Source.single("!"))
    case bm: BinaryMessage =>
      bm.dataStream.runWith(Sink.ignore)
      TextMessage(Source.single("server received a binary message ...."))
  }




  case class SocialPost(owner: String, content: String)

  val socialFeed = Source(
    List(
      SocialPost("Martin","Scala 3 has been announced!"),
      SocialPost("Daniel", "A new rock the JVM course is open!"),
      SocialPost("Martin", "I killed the JAVA")
    )
  )

  val socialMessages = socialFeed
    .throttle(1, 2 seconds)
    .map(socialPost => TextMessage(s"${socialPost.owner} said: ${socialPost.content}"))

  val socialFlow: Flow[Message, Message, Any] = Flow.fromSinkAndSource(
    Sink.foreach[Message](println),
    socialMessages
  )
  val webSocketRoute =
    (pathEndOrSingleSlash & get) {
      complete(
        HttpEntity (
          ContentTypes.`text/html(UTF-8)`,
          html
        )
      )
    } ~
      path("greeter") {
        handleWebSocketMessages(socialFlow)
      }

  Http().bindAndHandle(webSocketRoute, "localhost",8080)


}
