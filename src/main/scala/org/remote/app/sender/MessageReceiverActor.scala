package org.remote.app.sender

import akka.actor.{Actor, ActorRef, Props}
import org.remote.app.messaging.{Message, Statistics}

object MessageReceiverActor {
  def props(messagesCounter: ActorRef) = {
    Props(classOf[MessageReceiverActor], messagesCounter)
  }
}

class MessageReceiverActor(messagesCounter: ActorRef) extends Actor {
  import java.time.LocalDateTime
  import java.time.temporal.ChronoUnit._

  override def postStop {
    println(s"${self.path} was stopped")
  }

  override def receive: Receive = active(List[LocalDateTime]())

  def active(acc: List[LocalDateTime ]): Receive = {
    case Message(msg) =>
      val now = LocalDateTime.now
      val updated: List[LocalDateTime] = (now :: acc).filter(date => Math.abs(MILLIS.between(date, now)) < 1000)
      //println(s"${updated.size}")
      messagesCounter ! Statistics(updated.size, now)
      context become active(updated)
  }
}
