package com.rminhas.bookings.model

import java.sql.Date
import scala.util.Random

case class Reservation(
                        guestId: String,
                        hotelId: String,
                        startDate: Date,
                        endDate: Date,
                        roomNumber: Int,
                        confirmationNumber: String = Random.alphanumeric.take(10).mkString
                      ) {

  def intersect(another: Reservation) =
    this.hotelId == another.hotelId && this.roomNumber == another.roomNumber &&
      (
        startDate.compareTo(another.startDate) >= 0 && startDate.compareTo(another.endDate) <=0 ||
          another.startDate.compareTo(startDate) >= 0 && another.startDate.compareTo(endDate) <= 0
        )

  override def equals(obj: Any) = obj match {
    case Reservation(_, _, _, _, _, `confirmationNumber`) => true
    case _ => false
  }

  override def hashCode() = confirmationNumber.hashCode
}