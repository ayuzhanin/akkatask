package org.remote.app.sender

import akka.actor.{Actor, ActorRef, PoisonPill, Props}
import org.remote.app.messaging._

import scala.util.{Failure, Try}

object ManagerActor {
  def props(config: List[Action], localhost: String): Props = {
    Props(classOf[ManagerActor], config, localhost)
  }

  def senderName(remote: String) = s"to${remote}sender"

  def receiverName(remote: String) = s"from${remote}receiver"

  val actorName = "managerActor"
}

class ManagerActor(actions: List[Action], localhost: String) extends Actor {

  import ManagerActor._

  val counterActor: ActorRef = context.actorOf(Props(classOf[MessagesPerSecCounterActor]), "counter")

  def mapActionsToActors(): Map[Action, ActorRef] = {
    val actionActorDict = actions map { action =>
      val actor = action match {
        case SendTo(remote) =>
          context.actorOf(MessageSenderActor.props(remote, localhost), senderName(remote))
        case ReceiveFrom(remote) =>
          context.actorOf(MessageReceiverActor props counterActor, receiverName(remote))
      }
      action -> actor
    }
    actionActorDict.toMap
  }

  override def postStop {
    println(s"${self.path} was stopped")
  }

  override def receive: Receive = active(mapActionsToActors())

  def active(map: Map[Action, ActorRef]): Receive = {
    case msg: UpdateMessagesPerSec =>
      updateMessagesPerSecond(msg, map)
    case msg: SendTo =>
      startSendingTo(msg, map)
    case msg: ReceiveFrom =>
      startReceivingFrom(msg, map)
    case msg: StopSendingTo =>
      stopSendingTo(msg, map)
    case msg: StopReceivingFrom =>
      stopReceivingFrom(msg, map)
    case a: Any => println(s"Unexpected message $a")
  }

  def updateMessagesPerSecond(msg: UpdateMessagesPerSec, map: Map[Action, ActorRef]) = {
    println(s"Update messages per second to ${msg.number}")
    map.filter(_._1.isInstanceOf[SendTo]).values.foreach( _ ! msg )
  }

  def startSendingTo(action: SendTo, map: Map[Action, ActorRef]) = {
    Try(map(action)) match {
      case Failure(_) =>
        val sender = context.actorOf(MessageSenderActor.props(action.dest, localhost), senderName(action.dest))
        val updatedMap = map + (action -> sender)
        println(s"Start sending messages to ${sender.path}")
        context become active(updatedMap)
      case _ => // If host is already in list (i.e. MessageSenderActor for that particular host was already assigned) then do nothing
    }
  }

  def startReceivingFrom(action: ReceiveFrom, map: Map[Action, ActorRef]) = {
    Try(map(action)) match {
      case Failure(_) =>
        val receiver = context.actorOf(MessageReceiverActor props counterActor, receiverName(action.source))
        val updatedMap = map + (action -> receiver)
        println(s"Start receiving messages from ${receiver.path}")
        context become active(updatedMap)
      case _ => // If host is already in list (i.e. MessageReceiverActor for that particular host was already assigned) then do nothing
    }
  }

  def stopSendingTo(action: StopSendingTo, map: Map[Action, ActorRef]) = {
    val sendTo = SendTo(action.dest)
    val tryActor = Try(map(sendTo))
    for (actor <- tryActor) {
      println(actor)
      actor ! UpdateMessagesPerSec(0)
      actor ! PoisonPill
      println(s"Stop sending messages to ${actor.path}")
      val updatedMap = map - sendTo
      context become active(updatedMap)
    }
  }

  def stopReceivingFrom(action: StopReceivingFrom, map: Map[Action, ActorRef]) = {
    val receiveFrom = ReceiveFrom(action.source)
    val tryActor = Try(map(receiveFrom))
    for (actor <- tryActor) {
      actor ! PoisonPill
      val updatedMap = map - receiveFrom
      println(s"Stop receiving messages from ${actor.path}")
      context become active(updatedMap)
    }
  }
}