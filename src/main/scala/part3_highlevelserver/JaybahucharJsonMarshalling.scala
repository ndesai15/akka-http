package part3_highlevelserver

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes
import akka.stream.ActorMaterializer
import akka.http.scaladsl.server.Directives._
import akka.pattern.ask
import akka.util.Timeout
import spray.json._

import scala.concurrent.Future

/**
  * @author ndesai on 2020-10-23
  *
  */
case class Player(nickname: String, characterClass: String, level: Int)

object GameAreaMap {
  case object GetAllPlayers
  case class GetPlayer(nickname: String)
  case class GetPlayersByClass(characterClass: String)
  case class AddPlayer(player: Player)
  case class RemovePlayer(player: Player)
  case object OperationSuccess
}

class GameAreaMap extends Actor with ActorLogging {
  var players = Map[String, Player]()
  import GameAreaMap._
  override def receive: Receive = {
    case GetAllPlayers =>
      log.info("Getting all players")
      sender() ! players.values.toList

    case GetPlayer(nickname) =>
      log.info(s"Getting player with nickName $nickname")
      sender() ! players.get(nickname) //Option[Player]

    case GetPlayersByClass(characterClass) =>
      log.info(
        s"Getting all the players with the character class $characterClass"
      )
      sender() ! players.values.toList
        .filter(_.characterClass == characterClass)

    case AddPlayer(player) =>
      log.info(s"Trying to add a player $player")
      players = players + (player.nickname -> player)
      sender() ! OperationSuccess

    case RemovePlayer(player) =>
      log.info(s"Trying to remove a player $player")
      players = players - (player.nickname)
      sender() ! OperationSuccess
  }

}
trait PlayerJsonFormat extends DefaultJsonProtocol {
  implicit val playerFormat = jsonFormat3(Player) // Converts to & from json
}

object JaybahucharJsonMarshalling
    extends App
    with PlayerJsonFormat
    with SprayJsonSupport {
  import GameAreaMap._
  implicit val system = ActorSystem("JaybahucharMarshalling")
  implicit val materializer = ActorMaterializer()
  import system.dispatcher

  val playerActor = system.actorOf(Props[GameAreaMap], "Player Actor")
  val playerList = List(
    Player("martin", "warrior", 70),
    Player("hello", "world", 7),
    Player("small", "one", 10)
  )

  playerList.foreach { player =>
    playerActor ! AddPlayer(player)
  }

  /*
      GET /api/player : Returns all the players in the map -> as JSON
      GET /api/player/nickname: return player as JSON
      GET /api/player?nickname=X
      GET /api/player/class/(charClass) => Returns all the players with the given character class
      POST /api/player with JSON payload add a player
      DELETE /api/player with JSON payload , removes a player
   */

  import scala.concurrent.duration._
  implicit val timeout = Timeout(2 seconds)
  val simpleRoute =
    pathPrefix("api" / "player") {
      get {
        path("class" / Segment) { characterClass =>
          val playersByClassFuture: Future[List[Player]] =
            (playerActor ? GetPlayersByClass(characterClass))
              .mapTo[List[Player]]
          complete(playersByClassFuture) // Marshalling/ serializing object -> json
        } ~
          (path(Segment) | parameter('nickname)) { nickname =>
            val playerByNickNameFuture = (playerActor ? GetPlayer(nickname))
              .mapTo[Option[Player]] // convertable to json
            complete(playerByNickNameFuture) // Marshalling/ serializing object -> json
          } ~
          pathEndOrSingleSlash {
            val playersFuture = (playerActor ? GetAllPlayers)
              .mapTo[List[Player]] // convertable to json
            complete(playersFuture) // Marshalling/ serializing object -> json
          }
      } ~
        post {
          entity(as[Player]) { player =>
            complete((playerActor ? AddPlayer(player)).map(_ => StatusCodes.OK))
          }

        } ~
        delete {
          entity(as[Player]) { player =>
            complete(
              (playerActor ? RemovePlayer(player)).map(_ => StatusCodes.OK)
            )
          }
        }
    }

}
