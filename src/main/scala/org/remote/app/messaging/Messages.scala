package org.remote.app.messaging

import java.time.LocalDateTime

case class Message(msg: Any)

trait Action

case class UpdateMessagesPerSec(number: Int) extends Action

case class SendTo(dest: String) extends Action

case class ReceiveFrom(source: String) extends Action

case class StopSendingTo(dest: String) extends Action

case class StopReceivingFrom(source: String) extends Action

case class Statistics(number: Long, date: LocalDateTime) extends Action