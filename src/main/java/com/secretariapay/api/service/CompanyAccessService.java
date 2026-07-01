package com.secretariapay.api.service;

import com.secretariapay.api.entity.Booking;
import com.secretariapay.api.entity.Ticket;
import com.secretariapay.api.entity.Trip;
import com.secretariapay.api.entity.User;
import com.secretariapay.api.entity.enums.UserRole;
import com.secretariapay.api.repository.TicketRepository;
import com.secretariapay.api.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service("companyAccessService")
public class CompanyAccessService {

    private final UserRepository userRepository;
    private final TicketRepository ticketRepository;
    private final EntityManager entityManager;

    public CompanyAccessService(
            UserRepository userRepository,
            TicketRepository ticketRepository,
            EntityManager entityManager
    ) {
        this.userRepository = userRepository;
        this.ticketRepository = ticketRepository;
        this.entityManager = entityManager;
    }

    public boolean isAdmin() {
        User user = getCurrentUser();

        return user != null && UserRole.ADMIN.equals(user.getRole());
    }

    public boolean isOperator() {
        User user = getCurrentUser();

        return user != null && UserRole.OPERATOR.equals(user.getRole());
    }

    public boolean isCompanyAdmin() {
        User user = getCurrentUser();

        return user != null && UserRole.COMPANY_ADMIN.equals(user.getRole());
    }

    public boolean canAccessCompany(UUID companyId) {
        User user = getCurrentUser();

        if (user == null || companyId == null) {
            return false;
        }

        if (UserRole.ADMIN.equals(user.getRole())) {
            return true;
        }

        if (!UserRole.COMPANY_ADMIN.equals(user.getRole())) {
            return false;
        }

        if (user.getTransportCompany() == null) {
            return false;
        }

        return companyId.equals(user.getTransportCompany().getId());
    }

    public boolean canAccessTrip(UUID tripId) {
        User user = getCurrentUser();

        if (user == null || tripId == null) {
            return false;
        }

        if (UserRole.ADMIN.equals(user.getRole())) {
            return true;
        }

        if (!UserRole.COMPANY_ADMIN.equals(user.getRole())) {
            return false;
        }

        if (user.getTransportCompany() == null) {
            return false;
        }

        Trip trip = entityManager.find(Trip.class, tripId);

        if (trip == null || trip.getTransportCompany() == null) {
            return false;
        }

        return user.getTransportCompany()
                .getId()
                .equals(trip.getTransportCompany().getId());
    }

    public boolean canAccessBooking(UUID bookingId) {
        User user = getCurrentUser();

        if (user == null || bookingId == null) {
            return false;
        }

        if (UserRole.ADMIN.equals(user.getRole())) {
            return true;
        }

        if (!UserRole.COMPANY_ADMIN.equals(user.getRole())) {
            return false;
        }

        if (user.getTransportCompany() == null) {
            return false;
        }

        Booking booking = entityManager.find(Booking.class, bookingId);

        return canAccessBookingEntity(user, booking);
    }

    public boolean canAccessBookingCode(String bookingCode) {
        User user = getCurrentUser();

        if (user == null || bookingCode == null || bookingCode.isBlank()) {
            return false;
        }

        if (UserRole.ADMIN.equals(user.getRole())) {
            return true;
        }

        if (!UserRole.COMPANY_ADMIN.equals(user.getRole())) {
            return false;
        }

        if (user.getTransportCompany() == null) {
            return false;
        }

        Booking booking = entityManager
                .createQuery(
                        """
                        SELECT b
                        FROM Booking b
                        WHERE b.bookingCode = :bookingCode
                        """,
                        Booking.class
                )
                .setParameter("bookingCode", bookingCode)
                .getResultStream()
                .findFirst()
                .orElse(null);

        return canAccessBookingEntity(user, booking);
    }

    public boolean canAccessTicket(UUID ticketId) {
        User user = getCurrentUser();

        if (user == null || ticketId == null) {
            return false;
        }

        if (UserRole.ADMIN.equals(user.getRole())) {
            return true;
        }

        if (!UserRole.COMPANY_ADMIN.equals(user.getRole())) {
            return false;
        }

        if (user.getTransportCompany() == null) {
            return false;
        }

        Ticket ticket = entityManager.find(Ticket.class, ticketId);

        return canAccessTicketEntity(user, ticket);
    }

    public boolean canAccessTicketCode(String ticketCode) {
        User user = getCurrentUser();

        if (user == null || ticketCode == null || ticketCode.isBlank()) {
            return false;
        }

        if (UserRole.ADMIN.equals(user.getRole())) {
            return true;
        }

        if (!UserRole.COMPANY_ADMIN.equals(user.getRole())) {
            return false;
        }

        if (user.getTransportCompany() == null) {
            return false;
        }

        Ticket ticket = ticketRepository.findByTicketCode(ticketCode)
                .orElse(null);

        return canAccessTicketEntity(user, ticket);
    }

    public boolean canBoardTickets() {
        User user = getCurrentUser();

        if (user == null) {
            return false;
        }

        return UserRole.ADMIN.equals(user.getRole())
                || UserRole.OPERATOR.equals(user.getRole());
    }

    public boolean canBoardTicketCode(String ticketCode) {
        User user = getCurrentUser();

        if (user == null || ticketCode == null || ticketCode.isBlank()) {
            return false;
        }

        return UserRole.ADMIN.equals(user.getRole())
                || UserRole.OPERATOR.equals(user.getRole());
    }

    private boolean canAccessBookingEntity(User user, Booking booking) {
        if (booking == null
                || booking.getTrip() == null
                || booking.getTrip().getTransportCompany() == null) {
            return false;
        }

        return user.getTransportCompany()
                .getId()
                .equals(booking.getTrip().getTransportCompany().getId());
    }

    private boolean canAccessTicketEntity(User user, Ticket ticket) {
        if (ticket == null
                || ticket.getBooking() == null
                || ticket.getBooking().getTrip() == null
                || ticket.getBooking().getTrip().getTransportCompany() == null) {
            return false;
        }

        return user.getTransportCompany()
                .getId()
                .equals(ticket.getBooking().getTrip().getTransportCompany().getId());
    }

    private User getCurrentUser() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || authentication.getName() == null
                || "anonymousUser".equals(authentication.getName())) {
            return null;
        }

        return userRepository.findByEmailIgnoreCase(authentication.getName())
                .orElse(null);
    }
}
