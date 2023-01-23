package com.rminhas.bookings.actor


import akka.actor.typed.Behavior
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior}
import com.rminhas.bookings.model._

import scala.util.Random

// primary persistent actor
object Hotel {
  case class State(reservations: Set[Reservation])

  def commandHandler(hotelId: String): (State, Command) => Effect[Event, State] = (state, command) =>
    command match {
      case MakeReservation(guestId, startDate, endDate, roomNumber, replyTo) =>
        val tentativeReservation = Reservation(guestId, hotelId, startDate, endDate, roomNumber)
        if (state.reservations.exists(_.intersect(tentativeReservation)))
          Effect.reply(replyTo)(CommandFailure("Reservation failed: conflict with another reservation"))
        else
          Effect
            .persist(ReservationAccepted(tentativeReservation))
            .thenReply(replyTo)(_ => ReservationAccepted(tentativeReservation))

      case ChangeReservation(confirmationNumber, startDate, endDate, roomNumber, replyTo) =>
        // if no reservation => failure
        // create new tentative reservation
        // find if they conflict, if so => failure
        // otherwise, persist ReservationChanged
        val oldReservationOption = state.reservations.find(_.confirmationNumber == confirmationNumber)
        val newReservationOption = oldReservationOption
          .map(res => res.copy(startDate = startDate, endDate = endDate, roomNumber = roomNumber))
        val reservationUpdatedEventOption = oldReservationOption.zip(newReservationOption)
          .map(ReservationUpdated.tupled)
        val conflictingReservationOption = newReservationOption.flatMap { tentativeReservation =>
          state.reservations.find(r => r.confirmationNumber != confirmationNumber && r.intersect(tentativeReservation))
        }

        (reservationUpdatedEventOption, conflictingReservationOption) match {
          case (None, _) =>
            Effect.reply(replyTo)(CommandFailure(s"Cannot update reservation $confirmationNumber: not found"))
          case (_, Some(_)) =>
            Effect.reply(replyTo)(CommandFailure(s"Cannot update reservation $confirmationNumber: conflicting reservations"))
          case (Some(resUpdated), None) => // happy
            Effect.persist(resUpdated).thenReply(replyTo)(s => resUpdated)
        }

      case CancelReservation(confirmationNumber, replyTo) =>
        val reservationOption = state.reservations.find(_.confirmationNumber == confirmationNumber)
        reservationOption match {
          case Some(res) =>
            // success, confirmation found
            Effect.persist(ReservationCanceled(res)).thenReply(replyTo)(s => ReservationCanceled(res))
          case None =>
            // failure, reservation not found
            Effect.reply(replyTo)(CommandFailure(s"Cannot cancel reservation $confirmationNumber: not found"))
        }
    }

  def eventHandler(hotelId: String): (State, Event) => State = (state, event) =>
    event match {
      case ReservationAccepted(res) =>
        val newState = State(state.reservations + res)
        println(s"state changed: $newState")
        newState
      case ReservationUpdated(oldReservation, newReservation) =>
        val newState = State(state.reservations - oldReservation + newReservation)
        println(s"state changed: $newState")
        newState
      case ReservationCanceled(res) =>
        val newState = State(state.reservations - res)
        println(s"state changed: $newState")
        newState
    }

  def apply(hotelId: String): Behavior[Command] =
    EventSourcedBehavior[Command, Event, State](
      persistenceId = PersistenceId.ofUniqueId(hotelId),
      emptyState = State(Set()),
      commandHandler = commandHandler(hotelId),
      eventHandler = eventHandler(hotelId)
    )
}