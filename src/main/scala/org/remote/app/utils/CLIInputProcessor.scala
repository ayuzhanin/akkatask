package org.remote.app.utils

import akka.actor.{ActorRef, ActorSystem, PoisonPill}
import org.remote.app.controller.ControllerActor
import org.remote.app.messaging._

import scala.util.{Failure, Success, Try}


object CLIInputProcessor {

  case class ControllerConfig(hosts: List[String], localhost: String)

  case class SenderConfig(actions: List[Action], localhost: String)

  def configuration(args: Array[String]) = Try {
    val list = args.toList
    val config = list match {
      case configParams :: pathToConfig :: Nil =>
        configParams match {
          case "--role=controller" => controllerConfiguration(pathToConfig)
          case "--role=sender" => senderConfiguration(pathToConfig)
          case _ =>
            println(s"Cannot parse input params $configParams ")
            throw new IllegalArgumentException(s"Cannot parse input params $configParams")
        }
      case _ =>
        println(s"Cannot parse input params $list")
        throw new IllegalArgumentException(s"Cannot parse input params $list")
    }
    config.get
  }

  def senderConfiguration(file: String): Try[SenderConfig] = {
    import scala.io.Source
    val lines = Try(Source.fromFile(file).getLines.toList)
    for (list <- lines) yield {
      val filtered = list.filterNot(_.startsWith("localhost"))
      val actions = filtered map { str =>
        val arr = str.split(' ')
        val (action, host) = (arr(0), arr(1))
        action match {
          case "sendTo" => SendTo(host)
          case "receiveFrom" => ReceiveFrom(host)
          case _ => throw new IllegalArgumentException(s"Cannot define $action")
        }
      }
      Try(list.filter(_.startsWith("localhost")).head.split(' ')(1)) match {
        case Success(localhost) => SenderConfig(actions, localhost)
        case _ => throw new IllegalArgumentException(s"localhost in $file not provided")
      }
    }
  }

  def controllerConfiguration(file: String): Try[ControllerConfig] = {
    import scala.io.Source
    val lines = Try(Source.fromFile(file).getLines.toList)
    for (list <- lines) yield {
      val filtered = list.filterNot(_.startsWith("localhost"))
      val hosts = filtered map { str =>
        val arr = str.split(' ')
        val (command, host) = (arr(0), arr(1))
        if (command == "control") host
        else throw new IllegalArgumentException(s"Cannot define $command")
      }
      Try(list.filter(_.startsWith("localhost")).head.split(' ')(1)) match {
        case Success(localhost) => ControllerConfig(hosts, localhost)
        case _ => throw new IllegalArgumentException(s"localhost in $file not provided")
      }
    }
  }

  type State = (Map[String, ActorRef], String, ActorSystem)

  def until(condition: String => Boolean, action: State => State)
           (current: State): String =
    if (condition(current._2)) current._2
    else until(condition, action)(action(current))

  def interrupted: String => Boolean = _ == "stop"

  /*
  host sendTo/receiveFrom argument start/stop <=> list.size = 4
  control host start/stop                          <=> list.size = 3
  messagesPerSecond number                           <=> list.size = 2
  stop                                               <=> list.size = 1
   */
  def processInput: State => State = triple => {
    val (map, command, system) = triple
    val list = command.split(' ').toList
    val updatedMap = list.size match {
      case 1 =>
        list.head match {
          case "stop" | "pass" =>
          case _ => println(s"Illegal argument ${list.head}")
        }
        map
      case 2 => processList2(list, map)
        map
      case 3 => val upd = processList3(list, map, system)
        upd
      case 4 => processList4(list, map)
        map
      case _ =>
        println(s"Illegal argument $command")
        map
    }
    if (command == "stop") (updatedMap, command, system)
    else (updatedMap, scala.io.StdIn.readLine(), system)
  }

  // hostname sendTo/receiveFrom argumentHost start/stop
  def processList4(list: List[String], map: Map[String, ActorRef]) = {
    val actor = Try(map(list.head))
    actor match {
      case Failure(act) => println(s"Cannot resolve ${list.head}")
      case _ =>
    }
    val argHost = list(2)
    val message = Try {
      (list(1), list(3)) match {
        case ("sendTo", "start") => SendTo(argHost)
        case ("sendTo", "stop") => StopSendingTo(argHost)
        case ("receiveFrom", "start") => ReceiveFrom(argHost)
        case ("receiveFrom", "stop") => StopReceivingFrom(argHost)
        case other =>
          println(s"Illegal arguments $other")
          throw new IllegalArgumentException(s"Illegal arguments $other")
      }
    }
    for (act <- actor; msg <- message) act ! msg
  }

  // control hostname start/stop
  def processList3(list: List[String], map: Map[String, ActorRef], system: ActorSystem) = {
    val host = list(1)
    val actor = Try(map(host))
    val command = list.last
    if (list.head == "control") {
      val upd = (actor, command) match {
        case (Success(act), "start") =>
          println(s"Control $host is running already")
          map
        case (Failure(act), "start") =>
          val act = system.actorOf(ControllerActor.props(host), ControllerActor.actorName(host))
          println(s"Control $host starting..")
          map + (host -> act)
        case (Success(act), "stop") =>
          act ! PoisonPill
          println(s"Control $host stopping..")
          map - host
        case (Failure(act), "stop") =>
          println(s"Control $host was already stopped")
          map
        case _ =>
          println(s"Illegal argument $command")
          map
      }
      upd
    }
    else{
      println(s"Illegal argument ${list.head}")
      map
    }
  }

  //messagesPerSecond number
  def processList2(list: List[String], map: Map[String, ActorRef]) = {
    list match {
      case "updateMessagesPerSecond" :: number :: Nil =>
        Try(number.toInt) match {
          case Success(num) =>
            if (num >= 0){
              println(s"Updating messages per sec $num")
              map.values.foreach(_ ! UpdateMessagesPerSec(num))
            }
            else println(s"Illegal argument $num")
          case Failure(num) => println(s"Illegal argument $num")
        }
      case other => println(s"Illegal argument $other")
    }
  }
}
