package za.co.capitecbank.transaction_producer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.capitecbank.transaction_producer.domain.Transaction;

public interface TransactionRepository extends JpaRepository<Transaction, String> {
}
