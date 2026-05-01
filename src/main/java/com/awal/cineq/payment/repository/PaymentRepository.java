package com.awal.cineq.payment.repository;

import com.awal.cineq.payment.model.Payment;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends MongoRepository<Payment, String> {

    @Query("{ 'paymentId': ?0, 'deletedAt': null }")
    Optional<Payment> findByPaymentId(String paymentId);

    @Query("{ 'bookingId': ?0, 'deletedAt': null }")
    List<Payment> findByBookingId(String bookingId);

    @Query("{ 'customerId': ?0, 'deletedAt': null }")
    List<Payment> findByCustomerId(String customerId);

    @Query("{ 'gatewayTransactionId': ?0, 'deletedAt': null }")
    Optional<Payment> findByGatewayTransactionId(String gatewayTransactionId);
}
